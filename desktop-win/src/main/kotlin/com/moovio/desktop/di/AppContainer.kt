package com.moovio.desktop.di

import androidx.compose.runtime.mutableStateOf
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

    /** In-memory only: the NSFW area re-locks on every launch. Compose-state so the UI
     * recomposes and shows the NSFW section immediately after a successful unlock. */
    private val _nsfwUnlocked = mutableStateOf(false)
    val isNsfwUnlocked: Boolean get() = _nsfwUnlocked.value
    fun unlockNsfw() { _nsfwUnlocked.value = true }
    fun lockNsfw() { _nsfwUnlocked.value = false }

    fun dispose() {
        scope.cancel()
    }
}
