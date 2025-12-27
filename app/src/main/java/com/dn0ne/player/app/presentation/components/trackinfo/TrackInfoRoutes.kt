package com.dn0ne.player.app.presentation.components.trackinfo

import kotlinx.serialization.Serializable

@Serializable
sealed interface TrackInfoRoutes {
    @Serializable
    data object Info: TrackInfoRoutes
}