# Migration Plan: Realm to Room

## Executive Summary

Migrate from Realm database to Room to achieve:
- **Smaller memory footprint**: Eliminate JSON serialization/deserialization overhead
- **Better performance**: Use native SQL queries and proper indexing
- **Type safety**: Leverage Room's compile-time SQL verification
- **Android ecosystem alignment**: Better integration with Jetpack components

## Current State Analysis

### Realm Implementation
- **Schema**: Single entity `PlaylistJson`
  - `name` (String, primary key)
  - `json` (String, serialized entire Playlist as JSON)
- **Storage approach**: Entire playlist (including all tracks) stored as JSON blob
- **Problems**:
  - JSON serialization/deserialization on every read/write
  - No relational integrity
  - Can't query individual tracks efficiently
  - Large memory usage (entire JSON loaded into memory)
  - No foreign key relationships
  - Unnecessary duplication of track data

### Data Flow
```
PlayerViewModel → PlaylistRepository (interface) → RealmPlaylistRepository → Realm DB
                                              ↓
                                    JSON serialization/deserialization
```

## Target Architecture

### Room Schema Design

**Entities:**
```kotlin
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["trackUri"])
    ]
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val trackUri: String,
    val position: Int
)
```

**Benefits of this design:**
- **Minimal storage**: Only store track URIs, not full track data
- **Efficient queries**: Indexed foreign keys enable fast lookups
- **Relational integrity**: CASCADE deletion ensures cleanup
- **Small memory footprint**: Load only necessary data
- **Scalability**: Efficiently handle large playlists

### New Data Flow
```
PlayerViewModel → PlaylistRepository (interface) → RoomPlaylistRepository → Room Database
                                              ↓
                                      Direct SQL queries with Flow
```

## Migration Strategy: Phased Approach

### Phase 1: Preparation & Design (Days 1-2)

**Step 1.1: Add Room dependencies**
- Update `gradle/libs.versions.toml`
  - Add `room = "2.6.1"` (or latest stable)
  - Add Room library entries
- Update `app/build.gradle.kts`
  - Add ksp plugin
  - Add Room dependencies
  - Keep Realm for now (dual implementation period)

**Step 1.2: Create Room entities and DAOs**
- Create `app/src/main/java/com/dn0ne/player/app/data/local/entity/`
  - `PlaylistEntity.kt`
  - `PlaylistTrackEntity.kt`
- Create `app/src/main/java/com/dn0ne/player/app/data/local/dao/`
  - `PlaylistDao.kt` with methods:
    - `getAllPlaylists(): Flow<List<PlaylistEntity>>`
    - `insertPlaylist(playlist: PlaylistEntity): Long`
    - `updatePlaylistName(playlistId: Long, name: String)`
    - `deletePlaylist(playlistId: Long)`
    - `getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>`
    - `insertTrackToPlaylist(track: PlaylistTrackEntity)`
    - `removeTrackFromPlaylist(playlistId: Long, trackUri: String)`
    - `updateTrackPosition(playlistId: Long, trackUri: String, newPosition: Int)`

**Step 1.3: Create Room database**
- Create `app/src/main/java/com/dn0ne/player/app/data/local/LotusDatabase.kt`
- Define database version and migration strategy
- Initial version: 1

### Phase 2: Room Implementation (Days 3-4)

**Step 2.1: Create RoomPlaylistRepository**
- Create `app/src/main/java/com/dn0ne/player/app/data/repository/RoomPlaylistRepository.kt`
- Implement `PlaylistRepository` interface
- Methods to implement:
  - `getPlaylists()`: Query all playlists, join with track entities, convert to domain models
  - `insertPlaylist()`: Insert playlist entity, then track entities in transaction
  - `updatePlaylistTrackList()`: Delete all tracks for playlist, insert new ones in transaction
  - `renamePlaylist()`: Simple UPDATE query
  - `deletePlaylist()`: CASCADE handles track deletion automatically

**Step 2.2: Update DI module for dual implementation**
- Modify `app/src/main/java/com/dn0ne/player/app/di/PlayerModule.kt`
- Add qualifier: `@Qualifier annotation @OldRealm and @NewRoom`
- Provide both implementations:
  ```kotlin
  single<PlaylistRepository>(qualifier = named("old")) {
      RealmPlaylistRepository(realm = get())
  }
  single<PlaylistRepository>(qualifier = named("new")) {
      RoomPlaylistRepository(playlistDao = get())
  }
  ```
- Configure flag in Settings for switching between implementations

**Step 2.3: Create data migration utility**
- Create `app/src/main/java/com/dn0ne/player/app/data/migration/MigrateToRoom.kt`
- Function to:
  - Read all playlists from Realm
  - Convert to Room entities
  - Insert into Room database
  - Verify migration success

### Phase 3: Migration Execution (Days 5-6)

**Step 3.1: Implement migration in App setup**
- Modify `SetupViewModel` or create `MigrationViewModel`
- Check if Room database is empty
- If empty and Realm has data, trigger migration
- Show progress indicator during migration
- Save migration completion flag in Settings

**Step 3.2: Update ViewModel to use Room implementation**
- Modify `PlayerViewModel` to accept `PlaylistRepository` (still interface)
- Remove Realm-specific imports
- Test all playlist operations

**Step 3.3: Write migration tests**
- Create unit tests for migration logic
- Test edge cases:
  - Empty playlists
  - Large playlists (1000+ tracks)
  - Duplicate track URIs
  - Playlist renaming before migration
  - Migration interruption handling

### Phase 4: Cleanup & Optimization (Days 7-8)

**Step 4.1: Remove Realm dependencies**
- Once migration is verified and working:
  - Remove Realm plugin from `app/build.gradle.kts`
  - Remove Realm dependencies
  - Remove Realm import from `PlayerModule.kt`
  - Delete `RealmPlaylistRepository.kt`
  - Delete `PlaylistJson` class
  - Remove `PlaylistJson.toPlaylistJson()` extension

**Step 4.2: Clean up Koin module**
- Remove qualifiers from DI
- Simplify to single `PlaylistRepository` provider:
  ```kotlin
  single<PlaylistRepository> {
      RoomPlaylistRepository(playlistDao = get())
  }
  ```

**Step 4.3: Performance optimization**
- Add database indexes on frequently queried columns
- Implement query result paging if playlists are very large
- Consider using `@RawQuery` for complex queries if needed
- Monitor database size and add cleanup routines for orphaned tracks

**Step 4.4: Add Room database migrations**
- Create `Migration_1_to_2.kt` (placeholder for future schema changes)
- Test migration path

### Phase 5: Testing & Validation (Days 9-10)

**Step 5.1: Unit tests**
- Test `RoomPlaylistRepository` methods
- Test `PlaylistDao` queries
- Mock `TrackRepository` for integration tests

**Step 5.2: Instrumented tests**
- Test database operations on real device/emulator
- Test migration from Realm to Room
- Test large playlists performance
- Test concurrent access

**Step 5.3: Manual testing**
- Test all playlist operations:
  - Create playlist
  - Add/remove tracks
  - Rename playlist
  - Delete playlist
  - Reorder tracks
  - Persist across app restarts
- Verify no data loss after migration

**Step 5.4: Performance profiling**
- Compare memory usage before/after migration
- Measure query times for:
  - Loading playlists
  - Adding/removing tracks
  - Large playlist operations
- Profile with Android Studio Profiler

### Phase 6: Documentation & Deployment (Days 11-12)

**Step 6.1: Update documentation**
- Update AGENTS.md with Room-specific guidelines
- Document database schema in docs/
- Add migration guide for future schema changes

**Step 6.2: Version bump and changelog**
- Increment version code and version name
- Document changes in changelog
- Test release build

**Step 6.3: Deploy**
- Run tests: `./gradlew test`
- Run instrumented tests: `./gradlew connectedAndroidTest`
- Build release APK: `./gradlew assembleRelease`
- Verify APK size impact

## Performance Optimizations

### Memory Footprint Reduction
1. **Eliminate JSON serialization**: Save ~30-40% memory on playlist operations
2. **Lazy loading**: Room Flow loads data incrementally
3. **Efficient queries**: Use indexed foreign keys for fast joins
4. **Minimal storage**: Store only track URIs, not full track data

### Query Optimizations
1. **Add indexes**:
   - `playlist_tracks.playlistId` (for JOIN operations)
   - `playlist_tracks.trackUri` (for checking track existence)
2. **Use `@Transaction`** for multi-step operations to prevent partial writes
3. **Use `Flow`** for reactive data loading
4. **Implement query result caching** if needed

## Risk Assessment & Mitigation

### High Risk
- **Data loss during migration**:
  - Mitigation: Backup Realm data before migration
  - Mitigation: Verify migration success before deleting Realm
  - Mitigation: Keep Realm data for 2 app versions after migration

### Medium Risk
- **Migration failure on large datasets**:
  - Mitigation: Test with large playlists (1000+ tracks)
  - Mitigation: Implement batch migration (100 tracks at a time)
  - Mitigation: Show progress to user, allow retry

### Low Risk
- **Performance regression**:
  - Mitigation: Profile before and after
  - Mitigation: Add indexes proactively
  - Mitigation: Optimize queries based on profiling results

## Success Criteria

### Functional
- [ ] All playlist operations work correctly
- [ ] No data loss after migration
- [ ] Migration completes successfully on all test devices
- [ ] App crashes: 0 increase

### Performance
- [ ] Memory usage: ≤ 70% of current implementation
- [ ] Playlist load time: ≤ 80% of current implementation
- [ ] Add/remove track operations: ≤ 70% of current
- [ ] APK size increase: < 1 MB

### Code Quality
- [ ] All tests pass
- [ ] No Realm dependencies remain
- [ ] Code follows project conventions
- [ ] Documentation updated

## Rollback Plan

If issues arise after migration:
1. Keep Realm migration flag in Settings
2. Allow users to revert to Realm implementation via settings
3. Keep Realm schema for 2 versions (3.0 and 3.1)
4. Implement "restore from backup" if migration fails

## Estimated Timeline

- **Phase 1**: 2 days
- **Phase 2**: 2 days
- **Phase 3**: 2 days
- **Phase 4**: 2 days
- **Phase 5**: 2 days
- **Phase 6**: 2 days

**Total**: 12 days

## Next Steps

1. Review and approve this plan
2. Begin Phase 1: Add Room dependencies
3. Create entities and DAOs
4. Implement RoomPlaylistRepository
5. Execute migration strategy
6. Test thoroughly before removing Realm

## Appendix: Code Snippets

### Example PlaylistDao Implementation

```kotlin
@Dao
interface PlaylistDao {
    @Transaction
    @Query("""
        SELECT p.* FROM playlists p
        ORDER BY p.createdAt DESC
    """)
    fun getAllPlaylists(): Flow<List<PlaylistWithTracks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun updatePlaylistName(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Transaction
    suspend fun insertPlaylistWithTracks(
        playlist: PlaylistEntity,
        tracks: List<PlaylistTrackEntity>
    ) {
        val playlistId = insertPlaylist(playlist)
        tracks.forEach { it.playlistId = playlistId }
        insertAllTracks(tracks)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTracks(tracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)
}

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val tracks: List<PlaylistTrackEntity>
)
```

### Example RoomPlaylistRepository

```kotlin
class RoomPlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val trackRepository: TrackRepository
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { playlistsWithTracks ->
            playlistsWithTracks.map { it.toPlaylist(trackRepository) }
        }
    }

    override suspend fun insertPlaylist(playlist: Playlist) {
        val trackEntities = playlist.trackList.mapIndexed { index, track ->
            PlaylistTrackEntity(
                trackUri = track.uri.toString(),
                position = index
            )
        }
        val playlistEntity = PlaylistEntity(
            name = playlist.name ?: ""
        )
        playlistDao.insertPlaylistWithTracks(playlistEntity, trackEntities)
    }

    override suspend fun updatePlaylistTrackList(playlist: Playlist, trackList: List<Track>) {
        val trackEntities = trackList.mapIndexed { index, track ->
            PlaylistTrackEntity(
                trackUri = track.uri.toString(),
                position = index
            )
        }
        // Transaction: clear existing tracks and insert new ones
        playlistDao.clearPlaylistTracks(playlist.id)
        playlistDao.insertAllTracks(trackEntities)
    }

    override suspend fun renamePlaylist(playlist: Playlist, name: String) {
        playlistDao.updatePlaylistName(playlist.id, name)
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist.id)
    }
}

suspend fun PlaylistWithTracks.toPlaylist(
    trackRepository: TrackRepository
): Playlist {
    val tracks = tracks
        .sortedBy { it.position }
        .mapNotNull { trackEntity ->
            trackRepository.getTrackByUri(Uri.parse(trackEntity.trackUri))
        }
    return Playlist(
        name = playlist.name,
        trackList = tracks
    )
}
```
