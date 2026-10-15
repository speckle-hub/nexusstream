package com.novastream.app.data.sfw

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.adult.adultRx
import com.novastream.app.data.adult.adultUnescape
import com.novastream.app.data.adult.capture
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * Gogo built-in anime source (Gogo / Anichi-style).
 *
 * The Gogo layout is shared by a family of mirrors: an ajax keyword search
 * returning anchor cards, a category page listing `/episode-{n}` links, and an
 * episode page embedding a vidstream/gogocdn-style player whose document carries
 * the `.m3u8` (directly, in a `"sources"` blob, or packed). Mirrors are walked
 * with per-mirror isolation.
 */
object GogoSource : SfwAnimeSource {

    override val id = "gogo"
    override val name = "Gogo"

    private val MIRRORS = listOf(
        "https://gogoanime3.co",
        "https://gogoanime.by",
        "https://anichi.to",
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

    override fun seriesUrlOf(episode: SfwAnimeEpisode): String? =
        Regex("""/category/[^"]+""").find(episode.url)?.value
            ?.let { SfwEmbeds.absolutise(it, SfwEmbeds.rootOf(episode.url)) }

    // ---- Parsing (internal so tests pin the contract without network) ----

    private const val MAX_SERIES = 3

    internal data class GogoSeries(val title: String, val url: String, val poster: String?)

    internal suspend fun searchSeries(query: String): List<GogoSeries> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        var lastError: Exception? = null
        for (mirror in MIRRORS) {
            try {
                val html = AdultHttp.get("$mirror/ajax-search.html?keyword=$q", referer = mirror)
                val found = parseSearch(html, mirror)
                if (found.isNotEmpty()) return found
            } catch (e: Exception) {
                lastError = e as? Exception ?: Exception(e)
            }
        }
        if (lastError != null) throw lastError!!
        return emptyList()
    }

    /** Ajax-search anchor cards: `<a href="/category/{slug}" title="{Title}">`. */
    internal fun parseSearch(html: String, host: String): List<GogoSeries> {
        val out = ArrayList<GogoSeries>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="(/category/[^"]+)"[^>]*>(.*?)</a>""").findAll(html)) {
            val href = m.groupValues[1]
            val inner = m.groupValues[2]
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{2,200})""""))
                ?: adultUnescape(inner.capture("""alt="([^"]{2,200})""""))
                ?: continue
            if (title.isBlank()) continue
            val url = SfwEmbeds.absolutise(href, host)
            val window = html.substring(maxOf(0, m.range.first - 200), minOf(html.length, m.range.last + 400))
            if (seen.add(url)) {
                out.add(GogoSeries(title, url, SfwEmbeds.thumb(window)))
            }
        }
        return out
    }

    internal suspend fun episodesOf(series: GogoSeries): List<SfwAnimeEpisode> {
        val html = AdultHttp.get(series.url, referer = base())
        return parseEpisodes(html, series)
    }

    /** Episode links on a category page: `/…-episode-{n}` anchors. */
    internal fun parseEpisodes(html: String, series: GogoSeries): List<SfwAnimeEpisode> {
        val host = SfwEmbeds.rootOf(series.url).ifBlank { base() }
        val out = ArrayList<SfwAnimeEpisode>()
        val seen = HashSet<String>()
        for (m in adultRx("""<a\s[^>]*href="([^"]*episode-\d+[^"]*)"[^>]*>(.*?)</a>""").findAll(html)) {
            val href = m.groupValues[1]
            val url = SfwEmbeds.absolutise(href, host)
            val epNum = Regex("""episode-(\d+)""").find(href)?.groupValues?.get(1)?.toIntOrNull()
            val rawTitle = adultUnescape(
                m.groupValues[2].replace(Regex("<[^>]*>"), " ").trim()
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
