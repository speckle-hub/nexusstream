package com.moovio.desktop.di

import com.moovio.desktop.data.local.LibraryStore
import com.moovio.desktop.data.local.SettingsStore
import com.moovio.desktop.data.remote.StremioClient
import com.moovio.desktop.data.remote.StremioRepository
import com.moovio.desktop.data.repo.CatalogRepository
import com.moovio.desktop.data.repo.StreamRepository
import com.moovio.desktop.player.LibMpvPlayer
import com.moovio.desktop.player.MpvPlayer

/**
 * Lightweight manual dependency container (no annotation processing required).
 *
 * Mirrors the Android app's `di/AppContainer.kt` so the two targets stay conceptually aligned.
 */
class AppContainer {
    val settings = SettingsStore()
    val library = LibraryStore(settings)

    val stremioClient = StremioClient(settings)
    val stremioRepository = StremioRepository(stremioClient, settings, library)

    val catalogRepository = CatalogRepository(stremioRepository)
    val streamRepository = StreamRepository(stremioRepository)

    // One libmpv instance per app lifecycle — desktop owns the native player handle.
    val player: MpvPlayer = LibMpvPlayer()

    fun close() {
        player.close()
    }
}
