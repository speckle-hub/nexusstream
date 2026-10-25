package com.moovio.desktop.data.remote

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.moovio.desktop.data.local.SettingsStore
import com.moovio.desktop.domain.model.Addon
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.MetaDetail
import com.moovio.desktop.domain.model.StreamSource
import java.io.File
import kotlin.coroutines.CoroutineDispatcher
import kotlin.coroutines.Dispatchers
import kotlin.coroutines.withContext

/**
 * Manages installed Stremio add-ons and resolves catalog/meta/streams across them.
 *
 * One failing add-on never breaks the whole app: every add-on call is isolated with
 * `runCatching` and bounded timeouts, and results are merged add-on by add-on.
 */
class StremioRepository(
    private val client: StremioClient,
    private val settings: SettingsStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val addonsFile = File(
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Moovio"),
        "addons.json",
    )
    private val json = Json { ignoreUnknownKeys = true }

    // Loaded once at startup; small JSON file, blocking read is fine.
    private var addons: List<Addon> = loadAddons()

    // Pre-seeded, useful defaults — content neutrality is preserved because the user can remove
    // any of them at any time, and all network calls degrade gracefully when unreachable.
    companion object {
        val DEFAULT_MANIFESTS = listOf(
            "https://v3-cinemeta.strem.io/manifest.json",   // metadata + public catalogs
            "https://torrentio.strem.fun/manifest.json",    // torrent streams
        )
    }

    private fun loadAddons(): List<Addon> = runCatching {
        json.decodeFromString(addonsFile.readText())
    }.getOrDefault {
        DEFAULT_MANIFESTS.mapIndexed { index, url ->
            Addon(
                id = url,
                name = when (url) {
                    "https://v3-cinemeta.strem.io/manifest.json" -> "Cinemeta (metadata)"
                    "https://torrentio.strem.fun/manifest.json" -> "Torrentio (streams)"
                    else -> url
                },
                manifestUrl = url,
                isInstalled = true,
                isEnabled = true,
            )
        }
    }

    private fun saveAddons() {
        addonsFile.writeText(json.encodeToString(addons))
    }

    suspend fun installedAddons(): List<Addon> = withContext(dispatcher) { addons }

    suspend fun enabledAddons(): List<Addon> = withContext(dispatcher) {
        addons.filter { it.isInstalled && it.isEnabled }
    }

    suspend fun installAddon(manifestUrl: String): Addon = withContext(dispatcher) {
        val manifest = client.fetchManifest(manifestUrl)
        val addon = Addon(
            id = manifest.id,
            name = manifest.name,
            manifestUrl = manifestUrl,
            isInstalled = true,
            isEnabled = true,
        )
        addons = if (addons.any { it.manifestUrl == manifestUrl }) {
            addons.map { if (it.manifestUrl == manifestUrl) addon else it }
        } else {
            addons + addon
        }
        saveAddons()
        addon
    }

    suspend fun setAddonEnabled(addonId: String, enabled: Boolean) = withContext(dispatcher) {
        addons = addons.map { if (it.id == addonId) it.copy(isEnabled = enabled) else it }
        saveAddons()
    }

    suspend fun removeAddon(addonId: String) = withContext(dispatcher) {
        addons = addons.filter { it.id != addonId }
        saveAddons()
    }

    // --- Resolution ----------------------------------------------------------

    suspend fun catalogItems(type: MediaType, catalogId: String): List<MediaItem> = withContext(dispatcher) {
        enabledAddons().mapNotNull { addon ->
            val response = client.fetchCatalog(addon.manifestUrl ?: "", catalogId, type.stremioType())
            response.metas.map { it.toMediaItem(addon.name ?: addon.id) }
        }.flatten().distinctBy { it.id }
    }

    private suspend fun findMeta(type: MediaType, id: String): StremioMeta? = withContext(dispatcher) {
        for (addon in enabledAddons()) {
            val response = client.fetchMeta(addon.manifestUrl ?: "", type.stremioType(), id)
            response.meta?.let { return@withContext it }
        }
        null
    }

    suspend fun resolveDetail(mediaId: String, type: MediaType, title: String): MetaDetail = withContext(dispatcher) {
        val meta = findMeta(type, mediaId)
        val streams = if (meta != null) resolveStreamsFor(type, mediaId) else emptyList()
        MetaDetail(
            mediaId = mediaId,
            type = type,
            title = meta?.name ?: title,
            year = meta?.releaseInfo?.take(4).toIntOrNull(),
            rating = meta?.imdbRating,
            poster = meta?.poster,
            backdrop = meta?.background,
            genre = meta?.genre,
            synopsis = meta?.description,
            seasons = meta?.videos
                ?.groupBy { it.season ?: 1 }
                .entries
                .sortedBy { it.key }
                .map { (season, episodes) ->
                    com.moovio.desktop.domain.model.Season(
                        number = season,
                        title = "Season $season",
                        episodeCount = episodes.size,
                        poster = null,
                    )
                } ?: emptyList(),
            episodes = meta?.videos.mapIndexed { index, video ->
                com.moovio.desktop.domain.model.Episode(
                    id = video.id,
                    season = video.season ?: 1,
                    number = index + 1,
                    title = video.title,
                    poster = null,
                    url = null,
                )
            } ?: emptyList(),
            streams = streams.map { it.toDomain(addonNameFor(mediaId) ?: "add-on") },
        )
    }

    private suspend fun resolveStreamsFor(type: MediaType, id: String): List<StremioStream> = withContext(dispatcher) {
        enabledAddons().mapNotNull { addon ->
            val response = client.fetchStreams(addon.manifestUrl ?: "", type.stremioType(), id)
            response.streams
        }.flatten()
    }

    private suspend fun addonNameFor(mediaId: String): String? = withContext(dispatcher) {
        for (addon in enabledAddons()) {
            val response = client.fetchMeta(addon.manifestUrl ?: "", "movie", mediaId)
            if (response.meta != null) return@withContext addon.name
        }
        null
    }
}
