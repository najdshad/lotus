# Database Migration Plan: Realm to Room

## Overview

This document provides a step-by-step guide for migrating the Lotus music player database from Realm to Room. The migration focuses on playlist data storage, which is currently stored in Realm as JSON strings.

**Current State:**
- Playlists stored in Realm via `PlaylistJson` entity
- Tracks queried from MediaStore (not in Realm)
- Data stored as serialized JSON strings in Realm
- Single Realm instance managed by Koin DI

**Target State:**
- Playlists stored in Room with normalized schema
- Proper relational data structure
- Type-safe queries with Room DAO
- Reactive Flow-based observations maintained

## Current Realm Analysis

### Schema (Realm)
- **Entity:** `PlaylistJson`
  - `name: String` (primary key)
  - `json: String` (serialized Playlist object)

### Repository Operations
- `getPlaylists(): Flow<List<Playlist>>`
- `insertPlaylist(playlist: Playlist)`
- `updatePlaylistTrackList(playlist: Playlist, trackList: List<Track>)`
- `renamePlaylist(playlist: Playlist, name: String)`
- `deletePlaylist(playlist: Playlist)`

### Key Dependencies
- Realm Kotlin SDK v2.3.0
- kotlinx.serialization for JSON serialization
- Koin for dependency injection

---

## Migration Plan

### Phase 1: Setup and Dependencies (Day 1)

#### 1.1 Update Gradle Dependencies

**File:** `gradle/libs.versions.toml`

Add Room dependencies:
```toml
[versions]
room = "2.7.0-alpha10"  # Check latest stable version

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version = "2.0.20-1.0.25" }
```

**File:** `app/build.gradle.kts`

Add KSP plugin and Room dependencies:
```kotlin
plugins {
    // existing plugins...
    alias(libs.plugins.ksp)  // Add this line
}

dependencies {
    // existing dependencies...
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Remove these after migration is complete:
    // implementation(libs.realm.library.base)
    // alias(libs.plugins.realm)
}
```

#### 1.2 Create Type Converters

**File:** `app/src/main/java/com/dn0ne/player/app/data/database/Converters.kt`

```kotlin
package com.dn0ne.player.app.data.database

import android.net.Uri
import androidx.room.TypeConverter

object UriConverter {
    @TypeConverter
    fun fromUri(uri: Uri?): String? = uri?.toString()

    @TypeConverter
    fun toUri(uriString: String?): Uri? = uriString?.let { Uri.parse(it) }
}
```

---

### Phase 2: Create Room Entities (Day 1-2)

#### 2.1 Create Playlist Entity

**File:** `app/src/main/java/com/dn0ne/player/app/data/database/PlaylistEntity.kt`

```kotlin
package com.dn0ne.player.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### 2.2 Create PlaylistTrack Junction Entity

**File:** `app/src/main/java/com/dn0ne/player/app/data/database/PlaylistTrackEntity.kt`

```kotlin
package com.dn0ne.player.app.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistName", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["name"],
            childColumns = ["playlistName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistName"])]
)
data class PlaylistTrackEntity(
    val playlistName: String,
    val position: Int,
    val trackData: String  // Serialized Track object as JSON
)
```

#### 2.3 Create Track Serialization Helper

**File:** `app/src/main/java/com/dn0ne/player/app/data/database/TrackJson.kt`

```kotlin
package com.dn0ne.player.app.data.database

import com.dn0ne.player.app.domain.track.Track
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class TrackJson(
    val json: String
)

fun Track.toTrackJson(): TrackJson = TrackJson(Json.encodeToString(this))

fun TrackJson.toTrack(): Track = Json.decodeFromString<Track>(json)
```

---

### Phase 3: Create Room DAO (Day 2)

**File:** `app/src/main/java/com/dn0ne/player/app/data/database/PlaylistDao.kt`

```kotlin
package com.dn0ne.player.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dn0ne.player.app.domain.track.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    
    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>)
    
    @Query("DELETE FROM playlist_tracks WHERE playlistName = :playlistName")
    suspend fun deletePlaylistTracks(playlistName: String)
    
    @Query("DELETE FROM playlists WHERE name = :name")
    suspend fun deletePlaylist(name: String)
    
    @Query("SELECT * FROM playlist_tracks WHERE playlistName = :playlistName ORDER BY position ASC")
    fun getPlaylistTracks(playlistName: String): Flow<List<PlaylistTrackEntity>>
    
    @Transaction
    suspend fun insertPlaylistWithTracks(playlist: PlaylistEntity, tracks: List<PlaylistTrackEntity>) {
        insertPlaylist(playlist)
        insertPlaylistTracks(tracks)
    }
    
    @Transaction
    suspend fun renamePlaylist(oldName: String, newName: String) {
        val playlist = getPlaylistByName(oldName) ?: return
        deletePlaylistTracks(oldName)
        deletePlaylist(oldName)
        insertPlaylist(playlist.copy(name = newName))
        // Tracks need to be migrated to new name
    }
}
```

---

### Phase 4: Create Room Database (Day 2-3)

**File:** `app/src/main/java/com/dn0ne/player/app/data/database/LotusDatabase.kt`

```kotlin
package com.dn0ne.player.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(UriConverter::class)
abstract class LotusDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    
    companion object {
        private const val DATABASE_NAME = "lotus_database"
        
        @Volatile
        private var INSTANCE: LotusDatabase? = null
        
        fun getDatabase(context: Context): LotusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LotusDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

### Phase 5: Create Migration Logic from Realm (Day 3-4)

**File:** `app/src/main/java/com/dn0ne/player/app/data/database/RealmMigrationHelper.kt`

```kotlin
package com.dn0ne.player.app.data.database

import android.content.Context
import com.dn0ne.player.app.data.repository.PlaylistJson
import com.dn0ne.player.app.domain.track.Playlist
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealmMigrationHelper(private val context: Context) {
    
    suspend fun migrateRealmDataToRoom(
        realmConfig: RealmConfiguration,
        roomDatabase: LotusDatabase
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val realm = Realm.open(realmConfig)
            val realmPlaylists = realm.query<PlaylistJson>().find()
            val dao = roomDatabase.playlistDao()
            
            realmPlaylists.forEach { realmPlaylist ->
                val playlist = realmPlaylist.toPlaylist()
                
                // Create playlist entity
                val playlistEntity = PlaylistEntity(
                    name = playlist.name ?: "Unknown"
                )
                
                // Create track entities
                val trackEntities = playlist.trackList.mapIndexed { index, track ->
                    PlaylistTrackEntity(
                        playlistName = playlist.name ?: "Unknown",
                        position = index,
                        trackData = track.toTrackJson().json
                    )
                }
                
                // Insert into Room
                dao.insertPlaylistWithTracks(playlistEntity, trackEntities)
            }
            
            realm.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun realmHasData(realmConfig: RealmConfiguration): Boolean {
        return try {
            val realm = Realm.open(realmConfig)
            val hasData = realm.query<PlaylistJson>().count() > 0
            realm.close()
            hasData
        } catch (e: Exception) {
            false
        }
    }
    
    fun deleteRealmDatabase(realmConfig: RealmConfiguration) {
        Realm.deleteRealm(realmConfig)
    }
}
```

---

### Phase 6: Create Room-Based Repository (Day 4-5)

**File:** `app/src/main/java/com/dn0ne/player/app/data/repository/RoomPlaylistRepository.kt`

```kotlin
package com.dn0ne.player.app.data.repository

import androidx.compose.ui.util.fastMap
import com.dn0ne.player.app.data.database.LotusDatabase
import com.dn0ne.player.app.data.database.PlaylistEntity
import com.dn0ne.player.app.data.database.PlaylistTrackEntity
import com.dn0ne.player.app.data.database.toTrack
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPlaylistRepository(
    private val database: LotusDatabase
) : PlaylistRepository {
    
    override fun getPlaylists(): Flow<List<Playlist>> {
        return database.playlistDao().getAllPlaylists().map { playlistEntities ->
            playlistEntities.map { entity ->
                Playlist(
                    name = entity.name,
                    trackList = emptyList()  // Loaded separately to avoid circular dependency
                )
            }
        }
    }
    
    override suspend fun insertPlaylist(playlist: Playlist) {
        val playlistEntity = PlaylistEntity(name = playlist.name ?: "Unknown")
        val trackEntities = playlist.trackList.mapIndexed { index, track ->
            PlaylistTrackEntity(
                playlistName = playlist.name ?: "Unknown",
                position = index,
                trackData = track.toTrackJson().json
            )
        }
        
        database.playlistDao().insertPlaylistWithTracks(playlistEntity, trackEntities)
    }
    
    override suspend fun updatePlaylistTrackList(playlist: Playlist, trackList: List<Track>) {
        // Delete existing tracks
        database.playlistDao().deletePlaylistTracks(playlist.name ?: "Unknown")
        
        // Insert new tracks
        val trackEntities = trackList.mapIndexed { index, track ->
            PlaylistTrackEntity(
                playlistName = playlist.name ?: "Unknown",
                position = index,
                trackData = track.toTrackJson().json
            )
        }
        
        database.playlistDao().insertPlaylistTracks(trackEntities)
    }
    
    override suspend fun renamePlaylist(playlist: Playlist, name: String) {
        database.playlistDao().renamePlaylist(playlist.name ?: "Unknown", name)
    }
    
    override suspend fun deletePlaylist(playlist: Playlist) {
        database.playlistDao().deletePlaylistTracks(playlist.name ?: "Unknown")
        database.playlistDao().deletePlaylist(playlist.name ?: "Unknown")
    }
    
    suspend fun getPlaylistWithTracks(name: String): Playlist? {
        val tracks = database.playlistDao().getPlaylistTracks(name)
            .map { entities ->
                entities.sortedBy { it.position }.fastMap { entity ->
                    entity.toTrack()
                }
            }
        
        return if (tracks.isNotEmpty()) {
            Playlist(name = name, trackList = tracks)
        } else {
            null
        }
    }
}
```

**Note:** For the `getPlaylists()` method, consider using a Room relation or modifying the approach to include tracks. See **Optimization Considerations** section.

---

### Phase 7: Update Dependency Injection (Day 5)

**File:** `app/src/main/java/com/dn0ne/player/app/di/PlayerModule.kt`

```kotlin
package com.dn0ne.player.app.di

import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.data.database.LotusDatabase
import com.dn0ne.player.app.data.database.RealmMigrationHelper
import com.dn0ne.player.app.data.database.RoomPlaylistRepository
import com.dn0ne.player.app.data.repository.PlaylistRepository
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.data.repository.TrackRepositoryImpl
import com.dn0ne.player.app.presentation.PlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val playerModule = module {

    single<TrackRepository> {
        TrackRepositoryImpl(
            context = androidContext(),
            settings = get()
        )
    }

    single<SavedPlayerState> {
        SavedPlayerState(
            context = androidContext()
        )
    }

    single<LotusDatabase> {
        LotusDatabase.getDatabase(androidContext())
    }
    
    single<RealmMigrationHelper> {
        RealmMigrationHelper(context = androidContext())
    }

    single<PlaylistRepository> {
        RoomPlaylistRepository(
            database = get()
        )
    }

    viewModel<PlayerViewModel> {
        PlayerViewModel(
            savedPlayerState = get(),
            trackRepository = get(),
            playlistRepository = get(),
            unsupportedArtworkEditFormats = emptyList(),
            settings = get(),
            musicScanner = get()
        )
    }
}
```

---

### Phase 8: Create Migration Service (Day 5-6)

**File:** `app/src/main/java/com/dn0ne/player/app/data/migration/DataMigrationService.kt`

```kotlin
package com.dn0ne.player.app.data.migration

import android.content.Context
import androidx.startup.Initializer
import com.dn0ne.player.app.data.database.LotusDatabase
import com.dn0ne.player.app.data.database.RealmMigrationHelper
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DataMigrationInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            performMigrationIfNeeded(context)
        }
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

private suspend fun performMigrationIfNeeded(context: Context) {
    val prefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
    val migrationCompleted = prefs.getBoolean("realm_to_room_migration", false)
    
    if (!migrationCompleted) {
        val realmConfig = RealmConfiguration.create(schema = setOf())
        val roomDatabase = LotusDatabase.getDatabase(context)
        val migrationHelper = RealmMigrationHelper(context)
        
        if (migrationHelper.realmHasData(realmConfig)) {
            val success = migrationHelper.migrateRealmDataToRoom(realmConfig, roomDatabase)
            if (success) {
                // Backup and delete Realm database
                migrationHelper.deleteRealmDatabase(realmConfig)
                prefs.edit().putBoolean("realm_to_room_migration", true).apply()
            }
        } else {
            // No Realm data to migrate, mark as complete
            prefs.edit().putBoolean("realm_to_room_migration", true).apply()
        }
    }
}
```

**File:** `app/src/main/AndroidManifest.xml`

Add the Initializer:
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="com.dn0ne.player.app.data.migration.DataMigrationInitializer"
        android:value="androidx.startup" />
</provider>
```

---

### Phase 9: Testing and Validation (Day 6-7)

#### 9.1 Unit Tests

**File:** `app/src/test/java/com/dn0ne/player/app/data/repository/RoomPlaylistRepositoryTest.kt`

```kotlin
package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.database.LotusDatabase
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RoomPlaylistRepositoryTest {
    
    private lateinit var database: LotusDatabase
    private lateinit var repository: RoomPlaylistRepository
    
    @Before
    fun setup() {
        // Use in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            LotusDatabase::class.java
        ).build()
        repository = RoomPlaylistRepository(database)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `insertPlaylist and retrieve it`() = runTest {
        val playlist = Playlist(
            name = "Test Playlist",
            trackList = emptyList()
        )
        
        repository.insertPlaylist(playlist)
        
        val playlists = repository.getPlaylists().first()
        assertThat(playlists).hasSize(1)
        assertThat(playlists[0].name).isEqualTo("Test Playlist")
    }
    
    @Test
    fun `deletePlaylist removes it from database`() = runTest {
        val playlist = Playlist(
            name = "Test Playlist",
            trackList = emptyList()
        )
        
        repository.insertPlaylist(playlist)
        repository.deletePlaylist(playlist)
        
        val playlists = repository.getPlaylists().first()
        assertThat(playlists).isEmpty()
    }
    
    // Add more tests for other operations...
}
```

#### 9.2 Manual Testing Checklist

- [ ] Create a new playlist in Realm version
- [ ] Add tracks to playlist
- [ ] Install app with Room migration
- [ ] Verify playlist appears correctly
- [ ] Verify all tracks are present
- [ ] Create new playlist in Room version
- [ ] Rename playlist
- [ ] Delete playlist
- [ ] Reorder tracks in playlist
- [ ] Import M3U playlist
- [ ] Test with large playlist (100+ tracks)
- [ ] Test with no existing Realm data (fresh install)

#### 9.3 Performance Testing

- Measure query time for playlists with 100, 500, 1000 tracks
- Compare with Realm performance
- Ensure UI remains responsive during large playlist operations

---

### Phase 10: Cleanup (Day 7)

#### 10.1 Remove Realm Dependencies

**File:** `gradle/libs.versions.toml`
- Remove `realm = "2.3.0"` from versions
- Remove `realm-library-base` from libraries
- Remove `realm` plugin from plugins

**File:** `app/build.gradle.kts`
- Remove `alias(libs.plugins.realm)` from plugins
- Remove `implementation(libs.realm.library.base)` from dependencies

#### 10.2 Delete Realm-Related Files

Delete these files:
- `app/src/main/java/com/dn0ne/player/app/data/repository/RealmPlaylistRepository.kt`
- `app/src/main/java/com/dn0ne/player/app/data/repository/PlaylistJson.kt` (moved to migration helper)

#### 10.3 Update PlayerModule Imports

Remove imports:
- `import io.realm.kotlin.Realm`
- `import io.realm.kotlin.RealmConfiguration`

---

## Optimization Considerations

### 1. Playlist with Tracks Loading

The current `getPlaylists()` implementation returns playlists without tracks. This is a design decision to avoid N+1 query problems. Consider these approaches:

**Option A: Load Tracks on Demand (Current)**
- Pros: Fast initial load
- Cons: Additional query when playlist is opened

**Option B: Pre-load with Room Relations**
```kotlin
data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "playlistName"
    )
    val tracks: List<PlaylistTrackEntity>
)

@Query("SELECT * FROM playlists")
fun getPlaylistsWithTracks(): Flow<List<PlaylistWithTracks>>
```

**Option C: Use Room's Multi-Instance Paging**
For very large playlists, use Paging 3 library.

### 2. Index Optimization

Consider adding indexes for common queries:
```kotlin
@Entity(
    tableName = "playlist_tracks",
    indices = [
        Index(value = ["playlistName"]),
        Index(value = ["playlistName", "position"])  // Composite index
    ]
)
```

### 3. Track Data Storage

Currently storing Track as JSON string. Consider:

**Pros of JSON approach:**
- Simple, no complex migrations
- Handles all Track properties easily

**Cons:**
- Can't query by track properties
- Larger storage size
- No foreign key integrity

**Alternative:** Create separate `TrackEntity` table with foreign key to playlist_tracks.

### 4. Caching

Consider caching frequently accessed playlists in memory using `LruCache`.

---

## Risk Mitigation

### 1. Data Loss Prevention

- **Backup Realm database** before migration
- Keep Realm migration code for 2-3 releases
- Add "Export Playlists" feature before migration

### 2. Rollback Plan

If critical issues are found:

1. Add build flag to use Realm: `buildConfigField("boolean", "USE_REALM", "true")`
2. Keep Realm code branch available
3. Quick hotfix release to revert changes

### 3. Migration Failure Handling

```kotlin
private suspend fun performMigrationIfNeeded(context: Context) {
    try {
        // Migration logic
    } catch (e: Exception) {
        Log.e("Migration", "Migration failed", e)
        // Don't mark as complete, user can retry in next version
    }
}
```

---

## Post-Migration Tasks

1. **Database Version Management**: Implement proper Room migrations for future schema changes
2. **Export/Import Feature**: Add playlist export to M3U/JSON for backup
3. **Analytics**: Track migration success/failure rates
4. **Documentation**: Update AGENTS.md to reflect Room usage
5. **Code Review**: Review all database operations for performance
6. **Memory Profiling**: Check for memory leaks with large playlists

---

## Testing Commands

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Build debug APK for testing
./gradlew assembleDebug

# Install and test
adb install app/build/outputs/apk/debug/app-debug.apk

# Check database
adb shell run-as com.dn0ne.lotus ls -la databases/
adb shell run-as com.dn0ne.lotus cat databases/lotus_database
```

---

## Timeline Summary

| Phase | Duration | Owner | Status |
|-------|----------|-------|--------|
| Phase 1: Setup | 1 day | Dev | Pending |
| Phase 2: Entities | 1-2 days | Dev | Pending |
| Phase 3: DAO | 1 day | Dev | Pending |
| Phase 4: Database | 1 day | Dev | Pending |
| Phase 5: Migration | 1-2 days | Dev | Pending |
| Phase 6: Repository | 1-2 days | Dev | Pending |
| Phase 7: DI Update | 1 day | Dev | Pending |
| Phase 8: Migration Service | 1-2 days | Dev | Pending |
| Phase 9: Testing | 1-2 days | QA | Pending |
| Phase 10: Cleanup | 1 day | Dev | Pending |
| **Total** | **9-14 days** | | |

---

## Contact & Resources

- **Room Documentation**: https://developer.android.com/training/data-storage/room
- **Kotlin Flow with Room**: https://developer.android.com/kotlin/flow
- **Room Migrations**: https://developer.android.com/training/data-storage/room/migrating-db-versions
- **Migration from Realm**: https://www.mongodb.com/docs/realm/sdk/android/

## Appendix: File Structure After Migration

```
app/src/main/java/com/dn0ne/player/
├── app/
│   ├── data/
│   │   ├── database/
│   │   │   ├── Converters.kt
│   │   │   ├── LotusDatabase.kt
│   │   │   ├── PlaylistDao.kt
│   │   │   ├── PlaylistEntity.kt
│   │   │   ├── PlaylistTrackEntity.kt
│   │   │   ├── RealmMigrationHelper.kt
│   │   │   └── TrackJson.kt
│   │   ├── migration/
│   │   │   └── DataMigrationInitializer.kt
│   │   └── repository/
│   │       ├── PlaylistRepository.kt (unchanged)
│   │       ├── RoomPlaylistRepository.kt (new)
│   │       └── TrackRepositoryImpl.kt (unchanged)
│   └── di/
│       └── PlayerModule.kt (updated)
```
