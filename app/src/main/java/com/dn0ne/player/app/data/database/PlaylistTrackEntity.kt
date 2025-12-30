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
    val trackData: String
)