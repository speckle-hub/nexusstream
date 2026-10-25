package com.moovio.desktop.data.repo

import com.moovio.desktop.data.local.AddonStore
import com.moovio.desktop.data.remote.StremioClient
import com.moovio.desktop.domain.model.Addon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class RecommendedAddon(
    val name: String,
    val manifestUrl: String,
    val description: String,
    val kind: String,
    val adult: Boolean = false,
)

class AddonRepository(
    private val stremio: StremioClient,
    private val store: AddonStore,
) {
    suspend fun install(manifestUrl: String, markAdult: Boolean? = null): Addon = withContext(Dispatchers.IO) {
        val trimmed = manifestUrl.trim()
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "Enter a full http(s) manifest URL"
        }
        val addon = withTimeout(20_000) { stremio.fetchManifest(trimmed) }
        val installed = addon.copy(adult = markAdult ?: addon.adult)
        store.upsert(installed)
        installed
    }

    fun all(): List<Addon> = store.current

    fun recommendedAddons(): List<RecommendedAddon> = recommended

    fun setEnabled(id: String, enabled: Boolean) = store.setEnabled(id, enabled)

    fun setAdult(id: String, adult: Boolean) = store.setAdult(id, adult)

    fun remove(id: String) = store.remove(id)

    companion object {
        /**
         * Suggestions, not auto-installed. Moovio ships legal metadata only; stream providers are
         * an explicit user choice made here.
         */
        val recommended: List<RecommendedAddon> = listOf(
            RecommendedAddon(
                name = "Torrentio",
                manifestUrl = "https://torrentio.strem.fun/manifest.json",
                description = "Torrent stream provider (requires a torrent client / debrid setup for best results).",
                kind = "Streams",
            ),
            RecommendedAddon(
                name = "OpenSubtitles v3",
                manifestUrl = "https://opensubtitles-v3.strem.io/manifest.json",
                description = "Community subtitle catalog for movies and series.",
                kind = "Subtitles",
            ),
            RecommendedAddon(
                name = "Kitsu",
                manifestUrl = "https://anime-kitsu.strem.io/manifest.json",
                description = "Anime metadata and streams keyed by Kitsu ids.",
                kind = "Anime",
            ),
        )
    }
}
