package com.dn0ne.player.app.data.repository

import androidx.compose.ui.util.fastMap
import com.dn0ne.player.app.data.database.LotusDatabase
import com.dn0ne.player.app.data.database.PlaylistEntity
import com.dn0ne.player.app.data.database.PlaylistTrackEntity
import com.dn0ne.player.app.data.database.TrackJson
import com.dn0ne.player.app.data.database.toTrack
import com.dn0ne.player.app.data.database.toTrackJson
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class RoomPlaylistRepository(
    private val database: LotusDatabase
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> {
        return database.playlistDao().getAllPlaylists().map { playlistEntities ->
            playlistEntities.map { entity ->
                val trackEntities = runBlocking<List<PlaylistTrackEntity>> {
                    database.playlistDao().getPlaylistTracks(entity.name).first()
                }
                Playlist(
                    name = entity.name,
                    trackList = trackEntities.sortedBy { it.position }.fastMap { entity ->
                        TrackJson(entity.trackData).toTrack()
                    }
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
        database.playlistDao().deletePlaylistTracks(playlist.name ?: "Unknown")

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
}