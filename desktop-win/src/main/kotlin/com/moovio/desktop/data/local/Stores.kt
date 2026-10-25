package com.moovio.desktop.data.local

import com.moovio.desktop.data.net.MoovioJson
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.domain.model.Addon
import com.moovio.desktop.domain.model.AddonCatalog
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.WatchProgress
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

object AppPaths {
    val dataDir: File by lazy {
        val override = System.getProperty("moovio.data.dir")?.takeIf { it.isNotBlank() }
        val base = if (override != null) {
            File(override)
        } else {
            val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
            File(appData, "Moovio")
        }
        base.apply { mkdirs() }
    }

    val libraryFile: File get() = File(dataDir, "library.json")
    val settingsFile: File get() = File(dataDir, "settings.json")
    val addonsFile: File get() = File(dataDir, "addons.json")
    val logsDir: File get() = File(dataDir, "logs").apply { mkdirs() }
}

/** Atomic, tolerant JSON persistence: a corrupt file never prevents the app from starting. */
class JsonFile<T>(
    private val file: File,
    private val serializer: kotlinx.serialization.KSerializer<T>,
    private val default: () -> T,
) {
    private val mutex = Mutex()

    fun read(): T = runCatching {
        if (!file.exists()) default() else MoovioJson.decodeFromString(serializer, file.readText())
    }.getOrElse { default() }

    suspend fun write(value: T) = mutex.withLock {
        withContextIo {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(MoovioJson.encodeToString(serializer, value))
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }
    }
}

private suspend fun <T> withContextIo(block: () -> T): T =
    kotlinx.coroutines.withContext(Dispatchers.IO) { block() }

@Serializable
data class LibraryState(
    val favorites: List<MediaItem> = emptyList(),
    val progress: List<WatchProgress> = emptyList(),
)

class LibraryStore(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    libraryFile: File = AppPaths.libraryFile,
) {

    private val file = JsonFile(libraryFile, LibraryState.serializer(), { LibraryState() })
    private val _state = MutableStateFlow(sanitize(file.read()))
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private fun sanitize(value: LibraryState): LibraryState = value.copy(
        favorites = value.favorites.distinctBy { it.key },
        progress = value.progress
            .distinctBy { it.mediaKey }
            .sortedByDescending { it.updatedAt }
            .take(60),
    )

    fun isFavorite(item: MediaItem): Boolean = _state.value.favorites.any { it.key == item.key }

    fun toggleFavorite(item: MediaItem): Boolean {
        var nowFavorite = false
        _state.update { current ->
            val existing = current.favorites.any { it.key == item.key }
            nowFavorite = !existing
            current.copy(
                favorites = if (existing) current.favorites.filterNot { it.key == item.key }
                else listOf(item) + current.favorites,
            )
        }
        persist()
        return nowFavorite
    }

    fun isInLibrary(item: MediaItem): Boolean = isFavorite(item)

    /** Removes a title from favourites and its Continue Watching entry in one action. */
    fun remove(item: MediaItem) {
        _state.update { current ->
            current.copy(
                favorites = current.favorites.filterNot { it.key == item.key },
                progress = current.progress.filterNot { it.mediaKey == item.key },
            )
        }
        persist()
    }

    fun removeProgress(mediaKey: String) {
        _state.update { current -> current.copy(progress = current.progress.filterNot { it.mediaKey == mediaKey }) }
        persist()
    }

    fun clearHistory() {
        _state.update { it.copy(progress = emptyList()) }
        persist()
    }

    fun recordProgress(progress: WatchProgress) {
        if (progress.durationMs <= 0) return
        _state.update { current ->
            val filtered = current.progress.filterNot { it.mediaKey == progress.mediaKey }
            current.copy(progress = (listOf(progress) + filtered).take(60))
        }
        persist()
    }

    fun progressFor(mediaKey: String): WatchProgress? =
        _state.value.progress.firstOrNull { it.mediaKey == mediaKey }

    fun continueWatching(adult: Boolean = false): List<WatchProgress> =
        _state.value.progress
            .filter { it.adult == adult && !it.finished && it.positionMs > 15_000 }
            .sortedByDescending { it.updatedAt }

    private fun persist() {
        val snapshot = _state.value
        scope.launch { file.write(snapshot) }
    }
}

@Serializable
data class AppSettings(
    val accent: String = "violet",
    val reduceMotion: Boolean = false,
    val autoPlayBest: Boolean = true,
    val preferredQuality: String = "1080p",
    val playerVolume: Int = 100,
    val tmdbApiKey: String = "",
    val mpvLibPath: String = "",
    val nsfwPinHash: String = "",
    val nsfwPinSalt: String = "",
    val nsfwEnabled: Boolean = true,
    val autoResume: Boolean = true,
    val hardwareDecoding: Boolean = true,
    val showAddonDiagnostics: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
) {
    val hasNsfwPin: Boolean get() = nsfwPinHash.isNotBlank() && nsfwPinSalt.isNotBlank()
}

class SettingsStore(private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) {

    private val file = JsonFile(AppPaths.settingsFile, AppSettings.serializer(), { AppSettings() })
    private val _state = MutableStateFlow(file.read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    val current: AppSettings get() = _state.value

    fun update(transform: (AppSettings) -> AppSettings) {
        _state.update(transform)
        val snapshot = _state.value
        scope.launch { file.write(snapshot) }
    }
}

@Serializable
data class AddonState(
    val addons: List<Addon> = emptyList(),
)

class AddonStore(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    addonsFile: File = AppPaths.addonsFile,
) {

    private val file = JsonFile(addonsFile, AddonState.serializer(), {
        AddonState(defaultAddons())
    })
    private val _state = MutableStateFlow(file.read().let { state ->
        if (state.addons.none { it.id == CinemetaClient.CINEMETA_ID }) {
            AddonState(defaultAddons() + state.addons)
        } else {
            state
        }
    })
    val state: StateFlow<AddonState> = _state.asStateFlow()

    val current: List<Addon> get() = _state.value.addons

    fun upsert(addon: Addon) {
        _state.update { state ->
            val filtered = state.addons.filterNot { it.id == addon.id || it.manifestUrl == addon.manifestUrl }
            AddonState(filtered + addon)
        }
        persist()
    }

    fun remove(addonId: String) {
        if (addonId == CinemetaClient.CINEMETA_ID) return
        _state.update { state -> AddonState(state.addons.filterNot { it.id == addonId }) }
        persist()
    }

    fun setEnabled(addonId: String, enabled: Boolean) {
        _state.update { state ->
            AddonState(state.addons.map { if (it.id == addonId) it.copy(enabled = enabled) else it })
        }
        persist()
    }

    fun setAdult(addonId: String, adult: Boolean) {
        _state.update { state ->
            AddonState(state.addons.map { if (it.id == addonId) it.copy(adult = adult) else it })
        }
        persist()
    }

    private fun persist() {
        val snapshot = _state.value
        scope.launch { file.write(snapshot) }
    }

    companion object {
        fun defaultAddons(): List<Addon> = listOf(
            Addon(
                manifestUrl = CinemetaClient.MANIFEST_URL,
                transportUrl = "https://v3-cinemeta.strem.io",
                id = CinemetaClient.CINEMETA_ID,
                name = "Cinemeta",
                version = "3.0",
                description = "Official Stremio metadata catalog for movies and series (IMDb ids).",
                types = listOf("movie", "series"),
                resources = listOf("catalog", "meta"),
                idPrefixes = listOf("tt"),
                catalogs = listOf(
                    AddonCatalog("movie", "top", "Top Movies"),
                    AddonCatalog("series", "top", "Top Series"),
                ),
                builtIn = true,
            ),
        )
    }
}
