# Realm to Room Migration Plan

## Overview
This document provides a step-by-step plan for migrating the Lotus music player database from Realm to Room. Since the database is empty, this is a clean implementation without data migration.

## Current State Analysis

### Realm Usage
- **Version:** 2.3.0 (Kotlin SDK)
- **Entity:** `PlaylistJson` with fields:
  - `name: String` (Primary Key)
  - `json: String` (Serialized Playlist object)
- **Repository:** `RealmPlaylistRepository` implements `PlaylistRepository`
- **Configuration:** Single entity schema in `PlayerModule.kt`

### Affected Files
1. `gradle/libs.versions.toml` - Realm version definitions
2. `build.gradle.kts` - Realm plugin
3. `app/build.gradle.kts` - Realm plugin and dependency
4. `app/src/main/java/com/dn0ne/player/app/data/repository/RealmPlaylistRepository.kt` - Realm implementation
5. `app/src/main/java/com/dn0ne/player/app/data/repository/PlaylistRepository.kt` - Repository interface
6. `app/src/main/java/com/dn0ne/player/app/di/PlayerModule.kt` - DI configuration

## Migration Steps

### Phase 1: Add Room Dependencies

#### 1.1 Update `gradle/libs.versions.toml`
Add Room version and library definitions:
```toml
[versions]
# Update or add:
room = "2.7.0-alpha11"  # or stable version 2.6.1

[libraries]
# Add:
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

#### 1.2 Update `app/build.gradle.kts`
```kotlin
plugins {
    // Remove: alias(libs.plugins.realm)
    // Add:
    kotlin("kapt")  // For Room annotation processing
}

dependencies {
    // Remove: implementation(libs.realm.library.base)

    // Add:
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
}
```

#### 1.3 Update `build.gradle.kts`
```kotlin
plugins {
    // Remove: alias(libs.plugins.realm) apply false
}
```

---

### Phase 2: Create Room Entities

#### 2.1 Create `app/src/main/java/com/dn0ne/player/app/data/entity/PlaylistEntity.kt`
```kotlin
package com.dn0ne.player.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val name: String,
    val json: String
)
```

**File location:** Create new package `app/data/entity/`

---

### Phase 3: Create Room DAO

#### 3.1 Create `app/src/main/java/com/dn0ne/player/app/data/dao/PlaylistDao.kt`
```kotlin
package com.dn0ne.player.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dn0ne.player.app.data.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE name = :name")
    suspend fun deletePlaylistByName(name: String)

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?
}
```

**File location:** Create new package `app/data/dao/`

---

### Phase 4: Create Room Database

#### 4.1 Create `app/src/main/java/com/dn0ne/player/app/data/LotusDatabase.kt`
```kotlin
package com.dn0ne.player.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dn0ne.player.app.data.dao.PlaylistDao
import com.dn0ne.player.app.data.entity.PlaylistEntity

@Database(
    entities = [PlaylistEntity::class],
    version = 1,
    exportSchema = true
)
abstract class LotusDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
```

**File location:** `app/data/LotusDatabase.kt`

---

### Phase 5: Implement Room-based Repository

#### 5.1 Create `app/src/main/java/com/dn0ne/player/app/data/repository/RoomPlaylistRepository.kt`
```kotlin
package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.dao.PlaylistDao
import com.dn0ne.player.app.data.entity.PlaylistEntity
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomPlaylistRepository(
    private val dao: PlaylistDao
) : PlaylistRepository {
    override fun getPlaylists(): Flow<List<Playlist>> {
        return dao.getPlaylists().map { entities ->
            entities.map { it.toPlaylist() }
        }
    }

    override suspend fun insertPlaylist(playlist: Playlist) {
        dao.insertPlaylist(playlist.toPlaylistEntity())
    }

    override suspend fun updatePlaylistTrackList(playlist: Playlist, trackList: List<Track>) {
        val updatedEntity = playlist.copy(trackList = trackList).toPlaylistEntity()
        dao.updatePlaylist(updatedEntity)
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        playlist.name?.let { dao.deletePlaylistByName(it) }
    }

    override suspend fun renamePlaylist(playlist: Playlist, name: String) {
        deletePlaylist(playlist)
        insertPlaylist(playlist.copy(name = name))
    }
}

fun Playlist.toPlaylistEntity(): PlaylistEntity = PlaylistEntity(
    name = this.name ?: "",
    json = Json.encodeToString(this)
)

fun PlaylistEntity.toPlaylist(): Playlist = Json.decodeFromString(json)
```

**File location:** `app/data/repository/RoomPlaylistRepository.kt`

---

### Phase 6: Update DI Module

#### 6.1 Update `app/src/main/java/com/dn0ne/player/app/di/PlayerModule.kt`
```kotlin
package com.dn0ne.player.app.di

import android.app.Application
import androidx.room.Room
import com.dn0ne.player.app.data.LotusDatabase
import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.data.dao.PlaylistDao
import com.dn0ne.player.app.data.repository.PlaylistRepository
import com.dn0ne.player.app.data.repository.RoomPlaylistRepository
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
        Room.databaseBuilder(
            androidContext(),
            LotusDatabase::class.java,
            "lotus_database"
        ).build()
    }

    single<PlaylistDao> {
        get<LotusDatabase>().playlistDao()
    }

    single<PlaylistRepository> {
        RoomPlaylistRepository(
            dao = get()
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

**Key changes:**
- Remove `Realm` and `RealmConfiguration` imports
- Remove `RealmPlaylistRepository` import
- Add `LotusDatabase`, `Room`, `PlaylistDao`, `RoomPlaylistRepository` imports
- Replace Realm configuration with Room database configuration
- Provide `PlaylistDao` as a singleton
- Update `PlaylistRepository` to use `RoomPlaylistRepository`

---

### Phase 7: Remove Realm Files and Dependencies

#### 7.1 Delete `app/src/main/java/com/dn0ne/player/app/data/repository/RealmPlaylistRepository.kt`
```bash
rm app/src/main/java/com/dn0ne/player/app/data/repository/RealmPlaylistRepository.kt
```

#### 7.2 Verify no other Realm imports exist
```bash
# Search for remaining Realm imports
rg -i "import.*realm" --type kotlin
# Should return no results
```

---

### Phase 8: Build and Test

#### 8.1 Clean and rebuild
```bash
./gradlew clean
./gradlew assembleDebug
```

#### 8.2 Verify compilation
- Ensure no Realm-related errors
- Check Room annotation processing succeeds
- Verify Room database schema export is generated

#### 8.3 Manual testing checklist
- [ ] Create a new playlist
- [ ] Rename a playlist
- [ ] Delete a playlist
- [ ] Add tracks to a playlist
- [ ] Remove tracks from a playlist
- [ ] Reorder tracks in a playlist
- [ ] Import M3U playlist
- [ ] App restart (verify playlist persistence)

---

### Phase 9: Additional Optimizations (Optional)

#### 9.1 Add Type Converters for Track (Optional)
Consider storing tracks as a separate table with relations instead of JSON:
- Create `TrackEntity`
- Create `PlaylistTrackEntity` (junction table for many-to-many)
- Update DAO with `@Transaction` for fetching playlists with tracks
- Update repository to handle relations

#### 9.2 Add Database Migrations
When schema changes in future versions:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Migration logic
    }
}

// Add to database builder:
.addMigrations(MIGRATION_1_2)
```

#### 9.3 Add Database Export/Import
Export schema for version control verification:
```kotlin
// In build.gradle.kts:
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

---

## Verification Commands

### Check for Realm references
```bash
rg -i "realm" --type kotlin -l
```

### Check for Realm in Gradle files
```bash
rg -i "realm" build.gradle.kts app/build.gradle.kts gradle/libs.versions.toml
```

### Verify Room database creation
```bash
# After running the app, check if database is created:
adb shell run-as com.dn0ne.lotus ls -la databases/
```

---

## Rollback Plan

If issues arise during migration:

1. **Revert `gradle/libs.versions.toml`** - Restore Realm version
2. **Revert `app/build.gradle.kts`** - Restore Realm plugin and dependencies
3. **Revert `build.gradle.kts`** - Restore Realm plugin
4. **Restore `RealmPlaylistRepository.kt`** - From git
5. **Revert `PlayerModule.kt`** - Restore Realm configuration
6. **Delete new files:**
   - `LotusDatabase.kt`
   - `RoomPlaylistRepository.kt`
   - `entity/PlaylistEntity.kt`
   - `dao/PlaylistDao.kt`

---

## Timeline Estimate

- **Phase 1-2:** 30 minutes (Dependencies and Entities)
- **Phase 3-4:** 30 minutes (DAO and Database)
- **Phase 5:** 30 minutes (Repository implementation)
- **Phase 6:** 15 minutes (DI updates)
- **Phase 7-8:** 30 minutes (Cleanup and testing)
- **Total:** ~2.5 hours

---

## Notes

- No data migration is needed (database is empty)
- The JSON serialization approach for storing playlists is maintained for simplicity
- Room provides better compile-time verification and SQL support
- Consider migrating to proper relational tables in Phase 9 for better performance with large playlists
- Room's Flow support matches the current implementation in `RealmPlaylistRepository`
