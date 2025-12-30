package com.dn0ne.player.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)