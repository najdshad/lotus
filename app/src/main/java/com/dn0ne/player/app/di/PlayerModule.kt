package com.dn0ne.player.app.di

import com.dn0ne.player.EqualizerController
import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.data.repository.PlaylistJson
import com.dn0ne.player.app.data.repository.PlaylistRepository
import com.dn0ne.player.app.data.repository.RealmPlaylistRepository
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.data.repository.TrackRepositoryImpl
import com.dn0ne.player.app.presentation.PlayerViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.serialization.json.Json
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

    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 180000
            }
        }
    }

    single<Realm> {
        val configuration = RealmConfiguration.create(
            schema = setOf(PlaylistJson::class)
        )

        Realm.open(configuration)
    }

    single<PlaylistRepository> {
        RealmPlaylistRepository(
            realm = get()
        )
    }

    single<EqualizerController> {
        EqualizerController(
            context = androidContext()
        )
    }

    viewModel<PlayerViewModel> {
        PlayerViewModel(
            savedPlayerState = get(),
            trackRepository = get(),
            playlistRepository = get(),
            unsupportedArtworkEditFormats = emptyList(),
            settings = get(),
            musicScanner = get(),
            equalizerController = get()
        )
    }
}