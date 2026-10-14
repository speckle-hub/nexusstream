package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * XAnimu (xanimu.com) built-in source.
 *
 * A high-volume hentai/Rule34 tube: video pages are `/{numericId}-{slug}/`
 * (confirmed pattern: `/260290-fischl-x-slime-…/`) and search is the stock
 * `/?s={query}` with `?filter=` sorting (`latest`, `most-viewed`, `longest`).
 * Category/tag/actor shelves share the same `/{id}-{slug}/` shape, so the card
 * filter requires the numeric-id prefix *and* excludes the taxonomy paths.
 * Streams resolve from the video page (or its player iframe) through
 * [HentaiTubes.resolvedStreams].
 */
object XAnimuSource : HentaiSource {

    override val id = "xanimu"
    override val name = "XAnimu"
    private const val BASE = "https://xanimu.com"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val html = runCatching { AdultHttp.get("$BASE/?s=$q", referer = "$BASE/") }.getOrNull()
            ?: runCatching { AdultHttp.get("$BASE/search/$q/", referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        return parseCards(html)
    }

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val page = runCatching { AdultHttp.get(episode.url, referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        return HentaiTubes.resolvedStreams(page, this, episode.url)
    }

    internal fun parseCards(html: String): List<HentaiEpisode> =
        HentaiTubes.parseCards(html, BASE, id) { href ->
            val path = href.substringAfter("xanimu.com", href)
            // Video permalinks start with `/<digits>-`; taxonomy shelves (`/tag/…`,
            // `/actor/…`, categories) and site chrome never do.
            path.matches(Regex("""/\d+-[^?#]*""")) && CHROME.none { path.contains(it) }
        }

    private val CHROME = listOf(
        "/tag/", "/actor/", "/categories", "/page/", "/my-history",
        "/my-favorites/", "/upload", "/contact", "/dmca", "/terms",
        "/privacy", "/rta", "/2257", "/parental",
        // Numeric taxonomy roots share the `/{id}-{slug}/` shape with videos
        // (`/24-tv-shows/`, `/22-games/`, `/23-movies/`, `/15410-comics/`).
        "/tv-shows/", "/games/", "/movies/", "/comics/",
    )
}
