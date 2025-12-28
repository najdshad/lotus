package com.dn0ne.player.app.presentation.components

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dn0ne.player.R

@Composable
fun TrackMenu(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onRemoveFromPlaylistClick: (() -> Unit)? = null
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = onDismissRequest,
        shape = ShapeDefaults.Medium,
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        val context = LocalContext.current
        DropdownMenuItem(
            text = {
                Text(text = context.resources.getString(R.string.play_next))
            },
            onClick = {
                onPlayNextClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.QueuePlayNext,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = {
                Text(text = context.resources.getString(R.string.add_to_queue))
            },
            onClick = {
                onAddToQueueClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.AddToQueue,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = {
                Text(text = context.resources.getString(R.string.add_to_playlist))
            },
            onClick = {
                onAddToPlaylistClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null
                )
            }
        )

        onRemoveFromPlaylistClick?.let {
            DropdownMenuItem(
                text = {
                    Text(text = context.resources.getString(R.string.remove_from_playlist))
                },
                onClick = {
                    onRemoveFromPlaylistClick()
                    onDismissRequest()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistRemove,
                        contentDescription = null
                    )
                }
            )
        }
        /*DropdownMenuItem(
            text = {
                Text(text = context.resources.getString(R.string.track_info))
            },
            onClick = {
                onViewTrackInfoClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.AudioFile,
                    contentDescription = null
                )
            }
        )*/
    }
}

@Composable
fun TrackMenuButton(
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
) {
    Box {
        var isMenuExpanded by remember {
            mutableStateOf(false)
        }

        IconButton(
            onClick = {
                isMenuExpanded = true
            }
        ) {
            val context = LocalContext.current
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = context.resources.getString(R.string.track_menu_button),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TrackMenu(
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            onPlayNextClick = onPlayNextClick,
            onAddToQueueClick = onAddToQueueClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onRemoveFromPlaylistClick = onRemoveFromPlaylistClick
        )
    }
}