package com.moovio.app.data.sfw

import com.moovio.app.data.adult.AdultHttp
import com.moovio.app.data.model.StreamSource

/**
 * Vidsrc family built-in resolver.
 *
 * Vidsrc has no search or catalog of its own: embeds are keyed directly by TMDB id
 * (`/embed/movie/{tmdb}`, `/embed/tv/{tmdb}/{season}/{episode}`), so resolution
 * needs no title matching at all. Each mirror is tried in order with per-mirror
 * isolation — the first mirror yielding playable links wins.
 *
 * The embed page carries a player iframe (one hop, occasionally two); the final
 * document holds the media URL as a direct `.m3u8`/`.mp4`, a `"file"`/`"sources"`
 * player blob, or a packed script (unpacked by [SfwEmbeds]).
 */
object VidsrcSource : SfwSource {

    override val id = "vidsrc"
    override val name = "Vidsrc"

    private val MIRRORS = listOf(
        "https://vidsrc.xyz",
        "https://vidsrc.to",
        "https://vidsrc.cc",
        "https://vidsrc.me",
    )

    override suspend fun streamsForTmdb(tmdbId: String, season: Int?, episode: Int?): List<StreamSource> {
        val clean = tmdbId.trim()
        if (clean.isEmpty() || clean.contains(':')) return emptyList()
        for (mirror in MIRRORS) {
            val embed = if (season != null && episode != null) {
                "$mirror/embed/tv/$clean/$season/$episode"
            } else {
                "$mirror/embed/movie/$clean"
            }
            val streams = runCatching { resolveMirror(embed) }.getOrDefault(emptyList())
            if (streams.isNotEmpty()) return streams
        }
        return emptyList()
    }

    private suspend fun resolveMirror(embedUrl: String): List<StreamSource> {
        val page = AdultHttp.get(embedUrl, referer = rootOf(embedUrl))
        return SfwEmbeds.resolveChain(page, embedUrl, id, name)
    }

    internal fun embedUrl(mirror: String, tmdbId: String, season: Int?, episode: Int?): String =
        if (season != null && episode != null) "$mirror/embed/tv/$tmdbId/$season/$episode"
        else "$mirror/embed/movie/$tmdbId"

    private fun rootOf(url: String): String = SfwEmbeds.rootOf(url)
}
