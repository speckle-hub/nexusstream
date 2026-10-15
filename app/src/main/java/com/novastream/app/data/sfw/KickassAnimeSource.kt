package com.novastream.app.data.sfw

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.adult.adultRx
import com.novastream.app.data.adult.adultUnescape
import com.novastream.app.data.adult.capture
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * KickassAnime built-in anime source.
 *
 * Series index with keyword search, per-series episode listings and one player
 * embed per episode page. Streams resolve through the shared
 * [SfwEmbeds.resolveChain]; mirrors are walked with per-mirror isolation.
 */
object KickassAnimeSource : SfwAnimeSource {

    override val id = "kickassanime"
    override val name = "KickassAnime"

    private val MIRRORS = listOf(
        "https://kickassanime.com.es",
        "https://kickassanime.am",
        "https://kaa.mx",
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
        return SfwEmbeds.resolveChain(page, episode.url, id, name)
    }

    private const val MAX_SERIES = 3

    internal data class KaSeries(val title: String, val url: String, val poster: String?)

    internal suspend fun searchSeries(query: String): List<KaSeries> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        var lastError: Exception? = null
        for (mirror in MIRRORS) {
            for (path in listOf("/search?q=$q", "/?s=$q")) {
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

    /** Series cards: `/anime/{slug}` anchors with title/alt text and a poster nearby. */
    internal fun parseSeries(html: String, host: String): List<KaSeries> {
        val out = ArrayList<KaSeries>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="([^"]*/anime/[^"#?]+)"[^>]*>(.*?)</a>""").findAll(html)) {
            val href = m.groupValues[1]
            if (href.contains("/episode")) continue
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{2,200})""""))
                ?: adultUnescape(m.groupValues[2].replace(Regex("<[^>]*>"), " ").trim())
                ?: continue
            if (title.length < 2 || SfwEmbeds.isJunk(title)) continue
            val url = SfwEmbeds.absolutise(href, host)
            val window = html.substring(maxOf(0, m.range.first - 300), minOf(html.length, m.range.last + 600))
            if (seen.add(url)) out.add(KaSeries(title, url, SfwEmbeds.thumb(window)))
        }
        return out
    }

    internal suspend fun episodesOf(series: KaSeries): List<SfwAnimeEpisode> {
        val html = AdultHttp.get(series.url, referer = base())
        return parseEpisodes(html, series)
    }

    /** Episode anchors on a series page: `/anime/{slug}/episode-{n}` (or `?ep=`). */
    internal fun parseEpisodes(html: String, series: KaSeries): List<SfwAnimeEpisode> {
        val host = SfwEmbeds.rootOf(series.url).ifBlank { base() }
        val out = ArrayList<SfwAnimeEpisode>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="([^"]*(?:episode[-_]?(\d+)|[?&]ep=(\d+))[^"]*)"[^>]*>(.*?)</a>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val epNum = (m.groupValues[2].ifBlank { m.groupValues[3] }).toIntOrNull()
            val url = SfwEmbeds.absolutise(href, host)
            val rawTitle = adultUnescape(
                m.groupValues[4].replace(Regex("<[^>]*>"), " ").trim()
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
