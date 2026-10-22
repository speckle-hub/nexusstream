package com.moovio.app.data.sfw

import com.moovio.app.data.adult.AdultHttp
import com.moovio.app.data.adult.adultRx
import com.moovio.app.data.adult.adultUnescape
import com.moovio.app.data.adult.capture
import com.moovio.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * YugenAnime-style built-in anime source.
 *
 * The original `yugenanime.tv` shut down in December 2024; successor mirrors keep
 * the same shape — a `/discover`/search index, `/anime/{id}/{slug}` series pages
 * with episode listings, and a yugen-style player embed carrying a `"sources"`
 * blob. Every fetch walks [MIRRORS] with per-mirror isolation, so dead mirrors
 * degrade silently while live ones resolve.
 */
object YugenAnimeSource : SfwAnimeSource {

    override val id = "yugenanime"
    override val name = "YugenAnime"

    private val MIRRORS = listOf(
        "https://yugenanime.tv",
        "https://yugenanime.to",
        "https://yugenanime.sx",
    )

    private fun base(): String = MIRRORS.first()

    override suspend fun search(query: String): List<SfwAnimeEpisode> {
        val series = runCatching { searchSeries(query) }.getOrDefault(emptyList())
        if (series.isEmpty()) return emptyList()
        val out = ArrayList<SfwAnimeEpisode>()
        for (s in series.take(MAX_SERIES)) {
            out += runCatching { episodesOf(s) }.getOrDefault(emptyList())
        }
        return out.distinctBy { it.url }
    }

    override suspend fun streams(episode: SfwAnimeEpisode): List<StreamSource> {
        val page = AdultHttp.get(episode.url, referer = base())
        // Yugen player embeds carry a "sources" blob — resolveChain picks it up
        // before falling back to iframe hops.
        return SfwEmbeds.resolveChain(page, episode.url, id, name)
    }

    private const val MAX_SERIES = 3

    internal data class YugenSeries(val title: String, val url: String, val poster: String?)

    internal suspend fun searchSeries(query: String): List<YugenSeries> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        var lastError: Exception? = null
        for (mirror in MIRRORS) {
            for (path in listOf("/discover?q=$q", "/search/?query=$q")) {
                try {
                    val html = AdultHttp.get(mirror + path, referer = mirror)
                    val found = parseSeries(html, mirror)
                    if (found.isNotEmpty()) return found
                } catch (e: Exception) {
                    lastError = e as? Exception ?: Exception(e)
                }
            }
        }
        if (lastError != null) throw lastError!!
        return emptyList()
    }

    /** Series cards: `/anime/{id}/{slug}` anchors. */
    internal fun parseSeries(html: String, host: String): List<YugenSeries> {
        val out = ArrayList<YugenSeries>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="([^"]*/anime/\d+/[^"#?]+)"[^>]*>(.*?)</a>""").findAll(html)) {
            val href = m.groupValues[1]
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{2,200})""""))
                ?: adultUnescape(m.groupValues[2].replace(Regex("<[^>]*>"), " ").trim())
                ?: continue
            if (title.length < 2 || SfwEmbeds.isJunk(title)) continue
            val url = SfwEmbeds.absolutise(href, host)
            val window = html.substring(maxOf(0, m.range.first - 300), minOf(html.length, m.range.last + 600))
            if (seen.add(url)) out.add(YugenSeries(title, url, SfwEmbeds.thumb(window)))
        }
        return out
    }

    internal suspend fun episodesOf(series: YugenSeries): List<SfwAnimeEpisode> {
        val html = AdultHttp.get(series.url, referer = base())
        return parseEpisodes(html, series)
    }

    /** Episode anchors on a series page: `/watch/{id}/{slug}/{n}`. */
    internal fun parseEpisodes(html: String, series: YugenSeries): List<SfwAnimeEpisode> {
        val host = SfwEmbeds.rootOf(series.url).ifBlank { base() }
        val out = ArrayList<SfwAnimeEpisode>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="([^"]*/watch/\d+/[^"#?]+/(\d+)/?)"[^>]*>(.*?)</a>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val epNum = m.groupValues[2].toIntOrNull()
            val url = SfwEmbeds.absolutise(href, host)
            val rawTitle = adultUnescape(
                m.groupValues[3].replace(Regex("<[^>]*>"), " ").trim()
                    .ifBlank { m.value.capture("""\stitle="([^"]{2,200})"""") }
            ) ?: "Episode ${epNum ?: "?"}"
            if (seen.add(url)) {
                out.add(
                    SfwAnimeEpisode(
                        sourceId = id,
                        series = series.title,
                        title = rawTitle,
                        url = url,
                        episode = epNum,
                        poster = series.poster,
                    )
                )
            }
        }
        return out.sortedBy { it.episode ?: Int.MAX_VALUE }
    }
}
