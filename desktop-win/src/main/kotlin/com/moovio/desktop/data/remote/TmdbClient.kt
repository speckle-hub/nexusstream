package com.moovio.desktop.data.remote

import com.moovio.desktop.data.local.SettingsStore
import com.moovio.desktop.data.net.Http
import com.moovio.desktop.data.net.arrayOrNull
import com.moovio.desktop.data.net.obj
import com.moovio.desktop.data.net.parseJsonObject
import com.moovio.desktop.data.net.str

data class TmdbArtwork(val poster: String?, val backdrop: String?)

/**
 * Optional TMDB enrichment. Moovio works without a key (Cinemeta/AniList cover metadata); a key
 * only upgrades artwork quality. Failures are silent — artwork is a nice-to-have.
 */
class TmdbClient(
    private val settings: SettingsStore,
    private val http: Http,
) {
    val isConfigured: Boolean get() = settings.current.tmdbApiKey.isNotBlank()

    suspend fun artworkForImdb(imdbId: String): TmdbArtwork? {
        val key = settings.current.tmdbApiKey.takeIf { it.isNotBlank() && imdbId.startsWith("tt") } ?: return null
        val url = "https://api.themoviedb.org/3/find/$imdbId?external_source=imdb_id&api_key=$key"
        val json = runCatching { parseJsonObject(http.getText(url, timeoutMs = 8_000)) }.getOrNull() ?: return null
        val results = listOf("movie_results", "tv_results")
            .flatMap { json.arrayOrNull(it)?.mapNotNull { item -> item.obj() } ?: emptyList() }
        val best = results.firstOrNull() ?: return null
        val poster = best.str("poster_path")?.let { "https://image.tmdb.org/t/p/w500$it" }
        val backdrop = best.str("backdrop_path")?.let { "https://image.tmdb.org/t/p/original$it" }
        return TmdbArtwork(poster, backdrop)
    }
}
