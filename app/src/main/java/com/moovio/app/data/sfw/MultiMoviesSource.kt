package com.moovio.app.data.sfw

import com.moovio.app.data.adult.AdultHttp
import com.moovio.app.data.adult.adultRx
import com.moovio.app.data.adult.adultUnescape
import com.moovio.app.data.adult.capture
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * MultiMovies built-in source (movies + TV, SoraStream-style aggregator).
 *
 * Classic WordPress-style index: `?s={query}` search cards, a title page carrying
 * the synopsis, an episode/server list for series, and one player iframe per
 * server. Mirrors rotate frequently, so every fetch walks the mirror list with
 * per-mirror isolation and the first mirror answering wins.
 */
object MultiMoviesSource : SfwSource {

    override val id = "multimovies"
    override val name = "MultiMovies"

    private val MIRRORS = listOf(
        "https://multimovies.fun",
        "https://multimovies.co",
        "https://multimovies.art",
    )

    private fun base(): String = MIRRORS.first()

    override suspend fun home(): List<SfwRow> = sfwRows(
        this,
        MediaType.MOVIE,
        defs = listOf(
            "Latest Movies" to "/movies/",
            "Latest TV" to "/tvshows/",
        ),
    ) { path ->
        val html = get(path)
        SfwEmbeds.parseTitleCards(html, base(), id, MediaType.MOVIE, ::isTitleLink)
    }

    override suspend fun search(query: String): List<SfwTitle> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val html = get("/?s=$q")
        return SfwEmbeds.parseTitleCards(html, base(), id, MediaType.MOVIE, ::isTitleLink)
            .map { it.copy(kind = if (isTvLink(it.url)) MediaType.SERIES else MediaType.MOVIE) }
    }

    override suspend fun detail(url: String): SfwDetail {
        val html = get(url)
        val title = adultUnescape(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { strip(it) })
            ?: "MultiMovies title"
        val poster = html.capture("""<img[^>]*class="[^"]*poster[^"]*"[^>]*src="([^"]+)"""")
            ?.let { SfwEmbeds.absolutise(it, base()) }
        val kind = if (isTvLink(url)) MediaType.SERIES else MediaType.MOVIE
        val desc = html.capture("""<div[^>]*class="[^"]*(?:synopsis|description|story)[^"]*"[^>]*>(.*?)</div>""")
            ?.let { strip(it) }
        val episodes = if (kind == MediaType.SERIES) {
            episodeLinks(html, title).mapIndexed { index, (epUrl, epTitle) ->
                SfwEpisode(
                    sourceId = id,
                    series = title,
                    title = epTitle.ifBlank { "Episode ${index + 1}" },
                    url = epUrl,
                    season = seasonOf(epUrl, index),
                    episode = episodeOf(epUrl, index + 1),
                )
            }
        } else emptyList()
        val streams = if (kind == MediaType.MOVIE) {
            SfwEmbeds.resolveChain(html, url, id, name)
        } else emptyList()
        return SfwDetail(
            title = SfwTitle(id, url, title, kind, poster),
            description = desc,
            poster = poster,
            episodes = episodes,
            streams = streams,
        )
    }

    override suspend fun streamsForEpisode(episode: SfwEpisode): List<StreamSource> {
        val html = get(episode.url)
        return SfwEmbeds.resolveChain(html, episode.url, id, name)
    }

    // ---- Parsing (internal so tests pin the contract without network) ----

    internal fun isTitleLink(href: String): Boolean =
        href.contains("/movies/") || href.contains("/tvshows/") || href.contains("/episode/")

    internal fun isTvLink(href: String): Boolean =
        href.contains("/tvshows/") || href.contains("/episode/")

    internal fun episodeLinks(html: String, series: String): List<Pair<String, String>> =
        adultRx("""<a\s[^>]*href="([^"]*(?:/episode/|/episodes/)[^"]*)"[^>]*>(.*?)</a>""")
            .findAll(html)
            .map { SfwEmbeds.absolutise(it.groupValues[1], base()) to strip(it.groupValues[2]) }
            .filter { it.first.startsWith("http") }
            .distinctBy { it.first }
            .toList()

    internal fun seasonOf(url: String, index: Int): Int? =
        Regex("""season[-_ ]?(\d{1,2})""", RegexOption.IGNORE_CASE).find(url)
            ?.groupValues?.get(1)?.toIntOrNull()

    internal fun episodeOf(url: String, fallback: Int): Int? =
        Regex("""episode[-_ ]?(\d{1,3})""", RegexOption.IGNORE_CASE).find(url)
            ?.groupValues?.get(1)?.toIntOrNull() ?: fallback

    private suspend fun get(path: String): String {
        var lastError: Exception? = null
        val mirrors = if (path.startsWith("http")) listOf("") else MIRRORS
        for (mirror in mirrors) {
            val url = if (path.startsWith("http")) path else mirror + path
            val referer = if (path.startsWith("http")) SfwEmbeds.rootOf(path) else mirror
            try {
                return AdultHttp.get(url, referer = referer)
            } catch (e: Exception) {
                lastError = e as? Exception ?: Exception(e)
            }
        }
        throw lastError ?: SfwSourceException("MultiMovies unreachable")
    }

    private fun strip(s: String): String =
        s.replace(Regex("<[^>]*>"), " ").replace(Regex("""\s+"""), " ").trim()
}
