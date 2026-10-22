package com.moovio.app.data.sfw

import com.moovio.app.data.adult.AdultHttp
import com.moovio.app.data.adult.adultUnescape
import com.moovio.app.data.adult.capture
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * Rive built-in source (movies + TV).
 *
 * Search-first aggregator: `/search?q={query}` cards lead to a title page with the
 * synopsis and a season/episode picker for series, and each watch page embeds one
 * player iframe per server. Resolution follows the shared [SfwEmbeds.resolveChain]
 * (direct media → iframe → nested → packed scripts), so new mirrors only need a
 * hostname added to [MIRRORS].
 */
object RiveSource : SfwSource {

    override val id = "rive"
    override val name = "Rive"

    private val MIRRORS = listOf(
        "https://rive.to",
        "https://rive.ac",
        "https://rive.stream",
    )

    private fun base(): String = MIRRORS.first()

    override suspend fun home(): List<SfwRow> = sfwRows(
        this,
        MediaType.MOVIE,
        defs = listOf(
            "Trending" to "/trending",
            "Latest" to "/latest",
        ),
    ) { path ->
        val html = get(path)
        SfwEmbeds.parseTitleCards(html, base(), id, MediaType.MOVIE, ::isTitleLink)
    }

    override suspend fun search(query: String): List<SfwTitle> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val html = get("/search?q=$q")
        return SfwEmbeds.parseTitleCards(html, base(), id, MediaType.MOVIE, ::isTitleLink)
            .map { it.copy(kind = if (isTvLink(it.url)) MediaType.SERIES else MediaType.MOVIE) }
    }

    override suspend fun detail(url: String): SfwDetail {
        val html = get(url)
        val title = adultUnescape(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { strip(it) })
            ?: "Rive title"
        val poster = html.capture("""<meta[^>]*property="og:image"[^>]*content="([^"]+)"""")
            ?.let { SfwEmbeds.absolutise(it, base()) }
        val kind = if (isTvLink(url)) MediaType.SERIES else MediaType.MOVIE
        val desc = html.capture("""<meta[^>]*property="og:description"[^>]*content="([^"]+)"""")
            ?.let { adultUnescape(it) }
        val episodes = if (kind == MediaType.SERIES) {
            MultiMoviesSource.episodeLinks(html, title).mapIndexed { index, (epUrl, epTitle) ->
                SfwEpisode(
                    sourceId = id,
                    series = title,
                    title = epTitle.ifBlank { "Episode ${index + 1}" },
                    url = epUrl,
                    season = MultiMoviesSource.seasonOf(epUrl, index),
                    episode = MultiMoviesSource.episodeOf(epUrl, index + 1),
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

    internal fun isTitleLink(href: String): Boolean =
        href.contains("/movie/") || href.contains("/tv/") || href.contains("/watch/")

    internal fun isTvLink(href: String): Boolean =
        href.contains("/tv/") || (href.contains("/watch/") && href.contains("season"))

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
        throw lastError ?: SfwSourceException("Rive unreachable")
    }

    private fun strip(s: String): String =
        s.replace(Regex("<[^>]*>"), " ").replace(Regex("""\s+"""), " ").trim()
}
