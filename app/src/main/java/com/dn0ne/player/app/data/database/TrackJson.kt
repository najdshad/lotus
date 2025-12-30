package com.dn0ne.player.app.data.database

import com.dn0ne.player.app.domain.track.Track
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class TrackJson(
    val json: String
)

fun Track.toTrackJson(): TrackJson = TrackJson(Json.encodeToString(this))

fun TrackJson.toTrack(): Track = Json.decodeFromString<Track>(json)