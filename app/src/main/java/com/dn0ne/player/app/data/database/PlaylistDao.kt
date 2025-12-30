package com.dn0ne.player.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
        val existingTracks = mutableListOf<PlaylistTrackEntity>()

        getPlaylistTracks(oldName)

        deletePlaylistTracks(oldName)
        deletePlaylist(oldName)
        insertPlaylist(playlist.copy(name = newName))
    }
}