package com.moovio.desktop.di

import com.moovio.desktop.data.local.AddonStore
import com.moovio.desktop.data.local.LibraryStore
import com.moovio.desktop.data.local.SettingsStore
import com.moovio.desktop.data.net.Http
import com.moovio.desktop.data.remote.AniListClient
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.data.remote.StremioClient
import com.moovio.desktop.data.remote.TmdbClient
import com.moovio.desktop.data.repo.AddonRepository
import com.moovio.desktop.data.repo.CatalogRepository
import com.moovio.desktop.data.repo.MetadataRepository
import com.moovio.desktop.data.repo.StreamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Deliberately small manual DI container: one instance per app process, constructed in `main`
 * and passed down through a CompositionLocal.
 */
class AppContainer {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val http = Http()

    val settings = SettingsStore(scope)
    val library = LibraryStore(scope)
    val addonStore = AddonStore(scope)

    val cinemeta = CinemetaClient(http)
    val aniList = AniListClient(http)
    val stremio = StremioClient(http)
    val tmdb = TmdbClient(settings, http)

    val addons = AddonRepository(stremio, addonStore)
    val catalog = CatalogRepository(cinemeta, aniList, stremio, addonStore)
    val metadata = MetadataRepository(cinemeta, aniList, stremio, addonStore, tmdb)
    val streams = StreamRepository(stremio, cinemeta, addonStore)

    /** In-memory only: the NSFW area re-locks on every launch. */
    private var nsfwUnlocked = false
    val isNsfwUnlocked: Boolean get() = nsfwUnlocked
    fun unlockNsfw() { nsfwUnlocked = true }
    fun lockNsfw() { nsfwUnlocked = false }

    fun dispose() {
        scope.cancel()
    }
}
