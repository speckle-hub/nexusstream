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
 * AnimeKai built-in anime source.
 *
 * AnimeKai pairs a JSON ajax search (`/ajax/anime/search?keyword=`) returning
 * detail-page URLs with watch pages (`/watch/{slug}-{id}`) whose episode list
 * and per-server embeds follow the documented ajax ladder (`/ajax/episode/list`,
 * `/ajax/server/{id}` → `{link}`). Mirrors rotate, so every hop walks [MIRRORS]
 * with per-mirror isolation.
 */
object AnimeKaiSource : SfwAnimeSource {

    override val id = "animekai"
    override val name = "AnimeKai"

    private val MIRRORS = listOf(
        "https://animekai.to",
        "https://animekai.li",
        "https://animekai.ac",
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
        // Episode URLs carry the ajax watch id (`…/watch/{slug}-{watchId}?ep={n}`):
        // resolve the server ladder directly instead of scraping the watch shell.
        val (watchId, epNum) = watchRef(episode.url)
        if (watchId != null) {
            val direct = runCatching { serverStreams(watchId, epNum ?: episode.episode) }
                .getOrDefault(emptyList())
            if (direct.isNotEmpty()) return direct
        }
        val page = AdultHttp.get(episode.url, referer = base())
        return SfwEmbeds.resolveChain(page, episode.url, id, name)
    }

    private const val MAX_SERIES = 3

    internal data class KaiSeries(val title: String, val url: String, val poster: String?)

    internal suspend fun searchSeries(query: String): List<KaiSeries> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        var lastError: Exception? = null
        for (mirror in MIRRORS) {
            try {
                val json = AdultHttp.get("$mirror/ajax/anime/search?keyword=$q", referer = mirror)
                val found = parseAjaxSearch(json, mirror)
                if (found.isNotEmpty()) return found
            } catch (e: Exception) {
                lastError = e as? Exception ?: Exception(e)
            }
            try {
                val html = AdultHttp.get("$mirror/browser?keyword=$q", referer = mirror)
                val found = parseSeries(html, mirror)
                if (found.isNotEmpty()) return found
            } catch (e: Exception) {
                lastError = e as? Exception ?: Exception(e)
            }
        }
        if (lastError != null) throw lastError!!
        return emptyList()
    }

    /** Ajax-search result HTML (`{"html": "<a href=\"/watch/…\">…"}`, or raw anchors). */
    internal fun parseAjaxSearch(json: String, host: String): List<KaiSeries> {
        val html = runCatching {
            JsonParser.parseString(json).asJsonObject.str("html").orEmpty()
        }.getOrDefault(json)
        if (html.isBlank()) return emptyList()
        return parseSeries(html, host)
    }

    /** Series cards: `/watch/{slug}-{id}` anchors (no `?ep=` — those are episodes). */
    internal fun parseSeries(html: String, host: String): List<KaiSeries> {
        val out = ArrayList<KaiSeries>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="([^"]*/watch/[^"#?]+)"[^>]*>(.*?)</a>""").findAll(html)) {
            val href = m.groupValues[1]
            if (href.contains("?ep=") || Regex("""episode[-_ ]?\d+""", RegexOption.IGNORE_CASE).containsMatchIn(href)) continue
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{2,200})""""))
                ?: adultUnescape(m.groupValues[2].replace(Regex("<[^>]*>"), " ").trim())
                ?: continue
            if (title.length < 2 || SfwEmbeds.isJunk(title)) continue
            val url = SfwEmbeds.absolutise(href, host)
            val window = html.substring(maxOf(0, m.range.first - 300), minOf(html.length, m.range.last + 600))
            if (seen.add(url)) out.add(KaiSeries(title, url, SfwEmbeds.thumb(window)))
        }
        return out
    }

    internal suspend fun episodesOf(series: KaiSeries): List<SfwAnimeEpisode> {
        val html = AdultHttp.get(series.url, referer = base())
        return parseEpisodes(html, series)
    }

    /** Episode entries on a watch page: `?ep={n}` links and `data-ep`/`data-id` hooks. */
    internal fun parseEpisodes(html: String, series: KaiSeries): List<SfwAnimeEpisode> {
        val host = SfwEmbeds.rootOf(series.url).ifBlank { base() }
        val out = ArrayList<SfwAnimeEpisode>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="([^"]*\?ep=(\d+)[^"]*)"[^>]*>(.*?)</a>""").findAll(html)) {
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

    /** `…/watch/{slug}-{watchId}?ep={n}` reference carried by episode URLs. */
    internal fun watchRef(url: String): Pair<String?, Int?> {
        val watchId = Regex("""/watch/.+-([A-Za-z0-9]+)(?:\?|$)""").find(url)?.groupValues?.get(1)
        val ep = Regex("""[?&]ep=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
        return watchId to ep
    }

    internal suspend fun serverStreams(watchId: String, episode: Int?): List<StreamSource> {
        val ep = episode ?: 1
        for (mirror in MIRRORS) {
            try {
                val json = AdultHttp.get("$mirror/ajax/episode/list/$watchId?ep=$ep", referer = mirror)
                val servers = serverIds(json)
                for (serverId in servers) {
                    val link = runCatching {
                        JsonParser.parseString(AdultHttp.get("$mirror/ajax/server/$serverId", referer = mirror))
                            .asJsonObject.str("link")
                    }.getOrNull()
                    if (link.isNullOrBlank() || !link.startsWith("http")) continue
                    val embed = AdultHttp.get(link, referer = mirror)
                    val found = SfwEmbeds.resolveChain(embed, link, id, name)
                    if (found.isNotEmpty()) return found
                }
            } catch (e: Exception) {
                // Per-mirror isolation: a dead mirror degrades silently.
                continue
            }
        }
        return emptyList()
    }

    internal fun serverIds(json: String): List<String> {
        val html = runCatching {
            JsonParser.parseString(json).asJsonObject.str("html").orEmpty()
        }.getOrDefault(json)
        return adultRx("""data-(?:id|server-id)="(\d+)"""").findAll(html)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }
}
