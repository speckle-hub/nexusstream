package com.moovio.app.data.sfw

import com.google.gson.JsonParser
import com.moovio.app.data.adult.AdultHttp
import com.moovio.app.data.adult.adultRx
import com.moovio.app.data.adult.adultUnescape
import com.moovio.app.data.adult.capture
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.remote.str
import java.net.URLEncoder

/**
 * Miruro built-in anime source.
 *
 * Miruro serves a search API plus series pages (`/series/{slug}`) with episode
 * listings; each episode page embeds the player, whose document carries the
 * media URL directly, in a `"sources"` blob, or packed. Mirrors are walked with
 * per-mirror isolation.
 */
object MiruroSource : SfwAnimeSource {

    override val id = "miruro"
    override val name = "Miruro"

    private val MIRRORS = listOf(
        "https://www.miruro.com",
        "https://miruro.ro",
        "https://miruro.space",
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

    internal data class MiruroSeries(val title: String, val url: String, val poster: String?)

    internal suspend fun searchSeries(query: String): List<MiruroSeries> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        var lastError: Exception? = null
        for (mirror in MIRRORS) {
            // JSON search API first, HTML search fallback.
            try {
                val json = AdultHttp.get("$mirror/api/search?q=$q", referer = mirror)
                val found = parseApiSearch(json, mirror)
                if (found.isNotEmpty()) return found
            } catch (e: Exception) {
                lastError = e as? Exception ?: Exception(e)
            }
            try {
                val html = AdultHttp.get("$mirror/search?q=$q", referer = mirror)
                val found = parseSeries(html, mirror)
                if (found.isNotEmpty()) return found
            } catch (e: Exception) {
                lastError = e as? Exception ?: Exception(e)
            }
        }
        if (lastError != null) throw lastError!!
        return emptyList()
    }

    /** API search records: `[{id/slug, title/name, image/poster}]`. */
    internal fun parseApiSearch(json: String, host: String): List<MiruroSeries> {
        val arr = runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            (root.get("data") ?: root.get("results"))?.takeIf { it.isJsonArray }?.asJsonArray
        }.getOrNull() ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val title = o.str("title") ?: o.str("name") ?: return@mapNotNull null
            if (title.length < 2) return@mapNotNull null
            val slug = o.str("slug") ?: o.str("id") ?: return@mapNotNull null
            MiruroSeries(
                title = title,
                url = "$host/series/$slug",
                poster = (o.str("image") ?: o.str("poster"))?.takeIf { it.startsWith("http") },
            )
        }.distinctBy { it.url }
    }

    /** Series cards: `/series/{slug}` anchors. */
    internal fun parseSeries(html: String, host: String): List<MiruroSeries> {
        val out = ArrayList<MiruroSeries>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="([^"]*/series/[^"#?]+)"[^>]*>(.*?)</a>""").findAll(html)) {
            val href = m.groupValues[1]
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{2,200})""""))
                ?: adultUnescape(m.groupValues[2].replace(Regex("<[^>]*>"), " ").trim())
                ?: continue
            if (title.length < 2 || SfwEmbeds.isJunk(title)) continue
            val url = SfwEmbeds.absolutise(href, host)
            val window = html.substring(maxOf(0, m.range.first - 300), minOf(html.length, m.range.last + 600))
            if (seen.add(url)) out.add(MiruroSeries(title, url, SfwEmbeds.thumb(window)))
        }
        return out
    }

    internal suspend fun episodesOf(series: MiruroSeries): List<SfwAnimeEpisode> {
        val html = AdultHttp.get(series.url, referer = base())
        return parseEpisodes(html, series)
    }

    /** Episode anchors on a series page: `/watch/…/{n}` or `?ep={n}` links. */
    internal fun parseEpisodes(html: String, series: MiruroSeries): List<SfwAnimeEpisode> {
        val host = SfwEmbeds.rootOf(series.url).ifBlank { base() }
        val out = ArrayList<SfwAnimeEpisode>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="([^"]*(?:/watch/[^"#?]+?(\d+)/?|[?&]ep=(\d+))[^"]*)"[^>]*>(.*?)</a>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val epNum = m.groupValues[2].ifBlank { m.groupValues[3] }.toIntOrNull()
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
