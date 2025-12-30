package com.dn0ne.player.app.di

import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.data.database.LotusDatabase
import com.dn0ne.player.app.data.repository.PlaylistRepository
import com.dn0ne.player.app.data.repository.RoomPlaylistRepository
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.data.repository.TrackRepositoryImpl
import com.dn0ne.player.app.presentation.PlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val playerModule = module {

    single<TrackRepository> {
        TrackRepositoryImpl(
            context = androidContext(),
            settings = get()
        )
    }

    single<SavedPlayerState> {
        SavedPlayerState(
            context = androidContext()
        )
    }

    single<LotusDatabase> {
        LotusDatabase.getDatabase(androidContext())
    }

    single<PlaylistRepository> {
        RoomPlaylistRepository(
            database = get()
        )
    }

    viewModel<PlayerViewModel> {
        PlayerViewModel(
            savedPlayerState = get(),
            trackRepository = get(),
            playlistRepository = get(),
            unsupportedArtworkEditFormats = emptyList(),
            settings = get(),
            musicScanner = get()
        )
    }
}