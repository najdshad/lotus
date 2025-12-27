package com.dn0ne.player.app.presentation.components.trackinfo

import com.dn0ne.player.app.domain.track.Track

data class TrackInfoSheetState(
    val isShown: Boolean = false,
    val track: Track? = null
)
