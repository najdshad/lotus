package com.dn0ne.player.app.presentation.components.trackinfo

import android.icu.text.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.CoverArt
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackInfoSheet(
    state: TrackInfoSheetState,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isShown,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + slideInVertically(
            initialOffsetY = { it / 10 }),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + slideOutVertically(
            targetOffsetY = { it / 10 }),
    ) {
        Box(
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .clickable(
                    enabled = false,
                    onClick = {}
                )
        ) {
            val context = LocalContext.current
            val collapseFractionState = remember { mutableStateOf(0f) }
            var collapseFraction by collapseFractionState

            ColumnWithCollapsibleTopBar(
                topBarContent = {
                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = context.resources.getString(R.string.close_track_info_sheet)
                        )
                    }

                    Text(
                        text = context.resources.getString(R.string.track_info),
                        fontSize = lerp(
                            MaterialTheme.typography.titleLarge.fontSize,
                            MaterialTheme.typography.displaySmall.fontSize,
                            collapseFraction
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 16.dp)
                    )
                },
                collapseFraction = {
                    collapseFraction = it
                },
                contentPadding = PaddingValues(horizontal = 28.dp),
                contentHorizontalAlignment = Alignment.CenterHorizontally,
                contentVerticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                state.track?.run {
                    CoverArt(
                        uri = coverArtUri,
                        modifier = Modifier
                            .requiredWidthIn(max = 400.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp)
                            .clip(ShapeDefaults.Medium)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    TagRow(
                        tag = context.resources.getString(R.string.title),
                        value = title ?: context.resources.getString(R.string.unknown_title)
                    )

                    TagRow(
                        tag = context.resources.getString(R.string.album),
                        value = album ?: context.resources.getString(R.string.unknown_album)
                    )

                    TagRow(
                        tag = context.resources.getString(R.string.artist),
                        value = artist
                            ?: context.resources.getString(R.string.unknown_artist)
                    )

                    TagRow(
                        tag = context.resources.getString(R.string.album_artist),
                        value = albumArtist
                            ?: context.resources.getString(R.string.unknown_album_artist)
                    )

                    TagRow(
                        tag = context.resources.getString(R.string.genre),
                        value = genre ?: context.resources.getString(R.string.unknown_genre)
                    )

                    TagRow(
                        tag = context.resources.getString(R.string.year),
                        value = year ?: context.resources.getString(R.string.unknown_year)
                    )

                    TagRow(
                        tag = context.resources.getString(R.string.track_number),
                        value = trackNumber
                            ?: context.resources.getString(R.string.unknown_track_number)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    TagRow(
                        tag = context.resources.getString(R.string.bitrate),
                        value = bitrate?.let { "$it kbps" }
                            ?: context.resources.getString(
                                R.string.unknown_bitrate
                            )
                    )

                    val durationString = remember {
                        val durationMinutes = duration / 1000 / 60
                        val durationSeconds = duration / 1000 % 60
                        "$durationMinutes".padStart(2, '0') +
                                ":" + "$durationSeconds".padStart(2, '0')
                    }
                    TagRow(
                        tag = context.resources.getString(R.string.duration),
                        value = durationString
                    )

                    val sizeString = remember {
                        "${(size / 1024f / 1024 * 100).roundToInt() / 100f} MB"
                    }
                    TagRow(
                        tag = context.resources.getString(R.string.size),
                        value = sizeString
                    )

                    val dateString = remember {
                        DateFormat.getDateInstance().format(Date(dateModified * 1000L))
                    }
                    TagRow(
                        tag = context.resources.getString(R.string.date_modified),
                        value = dateString
                    )

                    TagRow(
                        tag = context.resources.getString(R.string.path),
                        value = data
                    )

                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}
