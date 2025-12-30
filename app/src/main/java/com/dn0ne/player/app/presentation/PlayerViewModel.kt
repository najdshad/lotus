package com.dn0ne.player.app.presentation

import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.dn0ne.player.R
import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.data.repository.PlaylistRepository
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.domain.playback.PlaybackMode
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.TrackSort
import com.dn0ne.player.app.domain.sort.sortedBy
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.format
import com.dn0ne.player.app.presentation.PlayerScreenEvent.*
import com.dn0ne.player.app.presentation.components.playback.PlaybackState
import com.dn0ne.player.app.presentation.components.settings.SettingsSheetState
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarController
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarEvent
import com.dn0ne.player.app.presentation.components.trackinfo.TrackInfoSheetState
import com.dn0ne.player.core.data.MusicScanner
import com.dn0ne.player.core.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(
    private val savedPlayerState: SavedPlayerState,
    private val trackRepository: TrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val unsupportedArtworkEditFormats: List<String>,
    val settings: Settings,
    private val musicScanner: MusicScanner
) : ViewModel() {
    var player: Player? = null

    private val _settingsSheetState = MutableStateFlow(
        SettingsSheetState(
            settings = settings,
            musicScanner = musicScanner
        )
    )
    val settingsSheetState = _settingsSheetState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = _settingsSheetState.value
    )

    private val _trackSort = MutableStateFlow(settings.trackSort)
    val trackSort = _trackSort.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = _trackSort.value
    )

    private val _trackSortOrder = MutableStateFlow(settings.trackSortOrder)
    val trackSortOrder = _trackSortOrder.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = _trackSortOrder.value
    )

    private val _playlistSort = MutableStateFlow(settings.playlistSort)
    val playlistSort = _playlistSort.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = _playlistSort.value
    )

    private val _playlistSortOrder = MutableStateFlow(settings.playlistSortOrder)
    val playlistSortOrder = _playlistSortOrder.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = _playlistSortOrder.value
    )

    private val _trackList = MutableStateFlow(emptyList<Track>())
    val trackList = _trackList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val albumPlaylists = _trackList.map {
        it.groupBy { it.album }.entries.map {
            Playlist(
                name = it.key,
                trackList = it.value.sortedBy(TrackSort.TrackNumber, SortOrder.Ascending)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )
    val artistPlaylists = _trackList.map {
        it.groupBy { it.artist }.entries.map {
            Playlist(
                name = it.key,
                trackList = it.value.sortedBy(TrackSort.TrackNumber, SortOrder.Ascending)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val playlists = playlistRepository.getPlaylists().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist = _selectedPlaylist.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    private val _selectedPlaylistIsAlbum = MutableStateFlow(false)
    val selectedPlaylistIsAlbum = _selectedPlaylistIsAlbum.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = false
    )

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PlaybackState()
        )

    private var positionUpdateJob: Job? = null

    private val _trackInfoSheetState = MutableStateFlow(TrackInfoSheetState())
    val trackInfoSheetState = _trackInfoSheetState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TrackInfoSheetState()
    )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = TrackInfoSheetState()
        )

    private val _pendingTrackUris = Channel<Uri>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val tracks = trackRepository.getTracks()

                if (_trackList.value.size != tracks.size || !_trackList.value.containsAll(tracks)) {
                    _trackList.update {
                        tracks.sortedBy(_trackSort.value, _trackSortOrder.value)
                    }

                    if (_trackInfoSheetState.value.track != null) {
                        _trackInfoSheetState.update {
                            it.copy(
                                track = _trackList.value.fastFirstOrNull { track -> it.track?.uri == track.uri }
                            )
                        }

                        _playbackState.update {
                            PlaybackState()
                        }

                        withContext(Dispatchers.Main) {
                            player?.stop()
                            player?.clearMediaItems()
                        }
                    }

                }
                delay(5000L)
            }
        }

        viewModelScope.launch {
            while (player == null) delay(500L)

            val playlist = savedPlayerState.playlist
            playlist?.let { playlist ->
                val trackMediaItem = player?.currentMediaItem ?: savedPlayerState.track?.mediaItem
                val index = playlist.trackList.indexOfFirst { trackMediaItem == it.mediaItem }
                val track = playlist.trackList.getOrNull(index)

                if (player?.mediaItemCount == 0) {
                    player?.addMediaItems(playlist.trackList.fastMap { it.mediaItem })
                    if (index >= 0) {
                        player?.seekTo(index, 0L)
                    }
                }

                _playbackState.update {
                    it.copy(
                        playlist = playlist,
                        currentTrack = track,
                        isPlaying = player!!.isPlaying,
                        position = player!!.currentPosition
                    )
                }

                if (player!!.isPlaying) {
                    positionUpdateJob = startPositionUpdate()
                }
            }

            val playbackMode = savedPlayerState.playbackMode
            setPlayerPlaybackMode(playbackMode)
            _playbackState.update {
                it.copy(
                    playbackMode = playbackMode
                )
            }

            player?.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playbackState.update {
                            it.copy(
                                isPlaying = isPlaying
                            )
                        }

                        positionUpdateJob?.cancel()
                        if (isPlaying) {
                            positionUpdateJob = startPositionUpdate()
                        }
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int
                    ) {
                        _playbackState.update {
                            it.copy(
                                currentTrack = it.playlist?.trackList?.fastFirstOrNull {
                                    it.mediaItem == mediaItem
                                }.also { savedPlayerState.track = it },
                                position = 0L
                            )
                        }

                        positionUpdateJob?.cancel()
                        positionUpdateJob = startPositionUpdate()
                    }
                }
            )

        }

        viewModelScope.launch {
            while (_trackList.value.isEmpty() || player == null) delay(500)

            _pendingTrackUris.receiveAsFlow().collectLatest { uri ->
                val path = "/storage" + Uri.decode(uri.toString().substringAfter("storage"))
                val track = _trackList.value.fastFirstOrNull { it.data == path || it.uri == uri }
                track?.let {
                    onEvent(
                        OnTrackClick(
                            track = it,
                            playlist = Playlist(
                                name = null,
                                trackList = _trackList.value
                            )
                        )
                    )
                } ?: run {
                    SnackbarController.sendEvent(
                        SnackbarEvent(
                            message = R.string.track_is_not_found_in_media_store
                        )
                    )
                }
            }
        }
    }

    fun onEvent(event: PlayerScreenEvent) {

        when (event) {
            is OnTrackClick -> {
                player?.let { player ->
                    if (_playbackState.value.playlist != event.playlist) {
                        player.clearMediaItems()
                        player.addMediaItems(
                            event.playlist.trackList.fastMap { track -> track.mediaItem }
                        )
                        player.prepare()
                    }
                    player.seekTo(
                        event.playlist.trackList.indexOfFirst { it == event.track },
                        0L
                    )
                    player.play()

                    _playbackState.update {
                        it.copy(
                            playlist = event.playlist,
                            currentTrack = event.track,
                            position = 0
                        )
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        savedPlayerState.playlist = event.playlist
                        savedPlayerState.track = event.track
                    }
                }
            }

            OnPauseClick -> {
                player?.run {
                    pause()
                }
            }

            OnPlayClick -> {
                player?.let { player ->
                    if (player.currentMediaItem == null) return

                    player.play()
                }
            }

            OnSeekToNextClick -> {
                player?.let { player ->
                    if (!player.hasNextMediaItem()) return

                    player.seekToNextMediaItem()
                }
            }

            OnSeekToPreviousClick -> {
                player?.let { player ->
                    if (settings.jumpToBeginning && player.currentPosition >= 3000) {
                        player.seekTo(0)
                        _playbackState.update {
                            it.copy(
                                position = 0
                            )
                        }
                    } else {
                        player.seekToPreviousMediaItem()
                    }
                }
            }

            is OnSeekTo -> {
                player?.let { player ->
                    if (player.currentMediaItem == null) return

                    player.seekTo(event.position)
                    _playbackState.update {
                        it.copy(
                            position = event.position
                        )
                    }
                }
            }

            OnResetPlayback -> {
                player?.clearMediaItems()
                _playbackState.update {
                    PlaybackState(
                        playbackMode = it.playbackMode
                    )
                }

                viewModelScope.launch(Dispatchers.IO) {
                    savedPlayerState.playlist = null
                    savedPlayerState.track = null
                }
            }

            OnPlaybackModeClick -> {
                val currentMode = _playbackState.value.playbackMode
                val newMode = when (currentMode) {
                    PlaybackMode.Repeat -> PlaybackMode.RepeatOne
                    PlaybackMode.RepeatOne -> PlaybackMode.PlayQueueOnce
                    PlaybackMode.PlayQueueOnce -> PlaybackMode.Repeat
                }
                setPlayerPlaybackMode(newMode)
                _playbackState.update {
                    it.copy(
                        playbackMode = newMode
                    )
                }
                savedPlayerState.playbackMode = newMode
            }

            is OnPlayerExpandedChange -> {
                _playbackState.update {
                    it.copy(
                        isPlayerExpanded = event.isExpanded
                    )
                }
            }

            is OnRemoveFromQueueClick -> {
                player?.let { player ->
                    if (event.index == player.currentMediaItemIndex) {
                        onEvent(OnSeekToNextClick)
                    }

                    player.removeMediaItem(event.index)

                    _playbackState.update {
                        it.copy(
                            playlist = it.playlist?.copy(
                                trackList = it.playlist.trackList.toMutableList().apply {
                                    removeAt(event.index)
                                }
                            )
                        )
                    }
                }
            }

            is OnAddToQueueClick -> {
                player?.let { player ->
                    val currentPlaylist = _playbackState.value.playlist ?: return@let
                    player.addMediaItems(event.tracks.fastMap { it.mediaItem })

                    _playbackState.update {
                        it.copy(
                            playlist = currentPlaylist.copy(
                                trackList = currentPlaylist.trackList + event.tracks
                            )
                        )
                    }
                }
            }

            is OnReorderingQueue -> {
                player?.let { player ->
                    if (event.from == event.to) return@let

                    player.moveMediaItem(event.from, event.to)

                    val currentState = _playbackState.value
                    val currentPlaylist = currentState.playlist ?: return@let
                    val tracks = currentPlaylist.trackList.toMutableList()
                    val track = tracks.removeAt(event.from)
                    tracks.add(event.to, track)

                    _playbackState.update {
                        currentState.copy(
                            playlist = currentPlaylist.copy(trackList = tracks)
                        )
                    }
                }
            }

            is OnPlayNextClick -> {
                player?.let { player ->
                    val currentIndex = player.currentMediaItemIndex
                    player.addMediaItem(currentIndex + 1, event.track.mediaItem)

                    val currentState = _playbackState.value
                    val currentPlaylist = currentState.playlist ?: return@let
                    val tracks = currentPlaylist.trackList.toMutableList()
                    tracks.add(currentIndex + 1, event.track)

                    _playbackState.update {
                        currentState.copy(
                            playlist = currentPlaylist.copy(trackList = tracks)
                        )
                    }
                }
            }

            is OnViewTrackInfoClick -> {
                _trackInfoSheetState.update {
                    it.copy(
                        isShown = true,
                        track = event.track
                    )
                }
            }

            is OnGoToAlbumClick -> {
                _selectedPlaylist.update {
                    albumPlaylists.value.fastFirstOrNull {
                        it.name == event.track.album
                    }
                }
            }

            is OnGoToArtistClick -> {
                _selectedPlaylist.update {
                    artistPlaylists.value.fastFirstOrNull {
                        it.name == event.track.artist
                    }
                }
            }

            OnCloseTrackInfoSheetClick -> {
                _trackInfoSheetState.update {
                    it.copy(
                        isShown = false
                    )
                }
            }

            is OnPlaylistSelection -> {
                _selectedPlaylist.update {
                    event.playlist
                }
                _selectedPlaylistIsAlbum.update {
                    event.isAlbum
                }
            }

            is OnTrackSortChange -> {
                event.sort?.let { sort ->
                    settings.trackSort = sort
                    _trackSort.update {
                        sort
                    }
                }

                event.order?.let { order ->
                    settings.trackSortOrder = order
                    _trackSortOrder.update {
                        order
                    }
                }

                _trackList.update {
                    it.sortedBy(
                        sort = _trackSort.value,
                        order = _trackSortOrder.value
                    )
                }

                _selectedPlaylist.update {
                    it?.copy(
                        trackList = it.trackList.sortedBy(
                            sort = _trackSort.value,
                            order = _trackSortOrder.value
                        )
                    )
                }
            }

            is OnPlaylistSortChange -> {
                event.sort?.let { sort ->
                    settings.playlistSort = sort
                    _playlistSort.update {
                        sort
                    }
                }

                event.order?.let { order ->
                    settings.playlistSortOrder = order
                    _playlistSortOrder.update {
                        order
                    }
                }
            }

            is OnCreatePlaylistClick -> {
                viewModelScope.launch {
                    if (playlists.value.map { it.name }.contains(event.name)) return@launch
                    playlistRepository.insertPlaylist(
                        Playlist(
                            name = event.name,
                            trackList = emptyList()
                        )
                    )
                }
            }

            is OnRenamePlaylistClick -> {
                viewModelScope.launch {
                    if (playlists.value.map { it.name }.contains(event.name)) return@launch
                    playlistRepository.renamePlaylist(
                        playlist = event.playlist,
                        name = event.name
                    )

                    _selectedPlaylist.update {
                        it?.copy(
                            name = event.name
                        )
                    }
                }
            }

            is OnDeletePlaylistClick -> {
                viewModelScope.launch {
                    playlistRepository.deletePlaylist(
                        playlist = event.playlist
                    )

                    _selectedPlaylist.update { null }
                }
            }

            is OnAddToPlaylist -> {
                viewModelScope.launch {
                    val existingTracks = event.playlist.trackList
                    val newTracksNotInList = event.tracks.filter { it !in existingTracks }

                    if (newTracksNotInList.isEmpty() && event.tracks.any { it in existingTracks }) {
                        SnackbarController.sendEvent(
                            SnackbarEvent(
                                message = R.string.track_is_already_on_playlist
                            )
                        )
                    }

                    val newTrackList = existingTracks + newTracksNotInList
                    playlistRepository.updatePlaylistTrackList(
                        playlist = event.playlist,
                        trackList = newTrackList
                    )
                }
            }

            is OnRemoveFromPlaylist -> {
                viewModelScope.launch {
                    val newTrackList = event.playlist.trackList.toMutableList().apply {
                        removeAll(event.tracks)
                    }

                    playlistRepository.updatePlaylistTrackList(
                        playlist = event.playlist,
                        trackList = newTrackList
                    )

                    _selectedPlaylist.update {
                        it?.copy(
                            trackList = newTrackList
                        )
                    }
                }
            }

            is OnPlaylistReorder -> {
                if (event.playlist.trackList != event.trackList) {
                    viewModelScope.launch {
                        playlistRepository.updatePlaylistTrackList(
                            playlist = event.playlist,
                            trackList = event.trackList
                        )
                    }

                    _selectedPlaylist.update {
                        it?.copy(
                            trackList = event.trackList
                        )
                    }
                }
            }

            OnSettingsClick -> {
                _settingsSheetState.update {
                    it.copy(
                        isShown = true
                    )
                }
            }

            OnCloseSettingsClick -> {
                _settingsSheetState.update {
                    it.copy(
                        isShown = false
                    )
                }
            }

            OnScanFoldersClick -> {
                _settingsSheetState.update {
                    it.copy(
                        foldersWithAudio = trackRepository.getFoldersWithAudio()
                    )
                }
            }
        }
    }

    fun playTrackFromUri(uri: Uri) {
        viewModelScope.launch {
            _pendingTrackUris.send(uri)
        }
    }

    fun onFolderPicked(path: String) {
        if (settings.isScanModeInclusive.value) {
            settings.updateExtraScanFolders(settings.extraScanFolders.value + path)
        } else {
            settings.updateExcludedScanFolders(settings.extraScanFolders.value + path)
        }
    }

    private fun startPositionUpdate(): Job {
        return viewModelScope.launch {
            player?.let { player ->
                while (_playbackState.value.isPlaying) {
                    _playbackState.update {
                        it.copy(
                            position = player.currentPosition
                        )
                    }
                    delay(50)
                }
            }
        }
    }

    private fun setPlayerPlaybackMode(playbackMode: PlaybackMode) {
        when (playbackMode) {
            PlaybackMode.Repeat -> {
                player?.repeatMode = Player.REPEAT_MODE_ALL
            }

            PlaybackMode.RepeatOne -> {
                player?.repeatMode = Player.REPEAT_MODE_ONE
            }

            PlaybackMode.PlayQueueOnce -> {
                player?.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    fun parseM3U(playlistName: String, fileContent: String) {
        viewModelScope.launch {
            val paths = fileContent.lines().fastFilter { it.startsWith("/") }
            val tracks = paths.map { path ->
                _trackList.value.fastFirstOrNull { path == it.data }
            }.filterNotNull()
            val name = playlistName.filter { it.isDigit() || it.isLetter() || it.isWhitespace() }

            playlistRepository.insertPlaylist(
                Playlist(
                    name = name,
                    trackList = tracks
                )
            )

            SnackbarController.sendEvent(
                SnackbarEvent(
                    message = R.string.imported_successfully
                )
            )
        }
    }

    /**
     * Returns next element after [index]. If next element index is out of bounds returns first element.
     * If index is negative returns `null`
     */
    private fun <T> List<T>.nextAfterOrNull(index: Int): T? {
        return getOrNull((index + 1) % size)
    }
}