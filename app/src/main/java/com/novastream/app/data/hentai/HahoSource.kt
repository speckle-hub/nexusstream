package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.adult.adultRx
import com.novastream.app.data.adult.adultUnescape
import com.novastream.app.data.adult.capture
import com.novastream.app.data.model.StreamSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder

/**
 * Haho (haho.moe) built-in source.
 *
 * Haho indexes **series**: `GET /anime?q={query}` returns series cards, and each series page
 * (`/anime/{slug}`) lists its episodes as `/anime/{slug}/{n}` links. Search therefore expands the
 * top matches into episodes — the [HentaiSource] contract — so callers still only ever see
 * episodes, and streams resolve from the episode page through [HentaiTubes].
 *
 * The site's older `/api/search?v=` JSON and `/search?v=` HTML endpoints are both 404 now, so this
 * source targets the live `/anime?q=` path only.
 */
object HahoSource : HentaiSource {

    override val id = "haho"
    override val name = "Haho"
    private const val BASE = "https://haho.moe"

    /** How many series a search expands into episodes — bounds search's network cost. */
    private const val MAX_SERIES = 3

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val html = runCatching { AdultHttp.get("$BASE/anime?q=$q", referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        val series = parseSeries(html)
        if (series.isEmpty()) return emptyList()
        // Expand in parallel: the repository caps the whole source lookup, so 1 + N sequential
        // fetches would blow the budget. A series whose page can't be read simply contributes
        // nothing (the repository treats that as "this source has no match").
        return coroutineScope {
            series.take(MAX_SERIES)
                .map { s -> async { runCatching { episodesOf(s) }.getOrDefault(emptyList()) } }
                .awaitAll()
                .flatten()
                .distinctBy { it.url }
        }
    }

    private suspend fun episodesOf(series: HahoSeries): List<HentaiEpisode> =
        parseEpisodes(AdultHttp.get(series.url, referer = "$BASE/"), series)

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val page = runCatching { AdultHttp.get(episode.url, referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        return HentaiTubes.resolvedStreams(page, this, episode.url)
    }

    // ---- Parsing (internal so the tests can pin the contract without a network call) ----

    /** One series card from a search page. */
    internal data class HahoSeries(val title: String, val url: String)

    /**
     * Series cards from `/anime?q=`:
     * `<a href="https://haho.moe/anime/{slug}" title="{Title}" data-content="…">`.
     */
    internal fun parseSeries(html: String): List<HahoSeries> {
        val out = ArrayList<HahoSeries>()
        val seen = HashSet<String>()
        for (tag in adultRx("""<a\b[^>]*>""").findAll(html).map { it.value }) {
            val href = tag.capture("""\shref="([^"]+)"""") ?: continue
            if (!SERIES_PATH.matches(href) || href.endsWith("/anime/search")) continue
            val title = adultUnescape(tag.capture("""\stitle="([^"]{2,200})"""")) ?: continue
            if (title.isBlank()) continue
            val url = if (href.startsWith("http")) href else HentaiTubes.absolutise(href, BASE)
            if (seen.add(url)) out.add(HahoSeries(title, url))
        }
        return out
    }

    /**
     * Episode cards from a series page:
     * `<a href="…/anime/{slug}/{n}" title="Watch {Series} Ep. {n} - {Episode}">`.
     */
    internal fun parseEpisodes(html: String, series: HahoSeries): List<HentaiEpisode> {
        val out = ArrayList<HentaiEpisode>()
        val seen = HashSet<String>()
        for (tag in adultRx("""<a\b[^>]*>""").findAll(html).map { it.value }) {
            val href = tag.capture("""\shref="([^"]+)"""") ?: continue
            if (!EPISODE_PATH.matches(href)) continue
            val url = if (href.startsWith("http")) href else HentaiTubes.absolutise(href, BASE)
            val number = href.trimEnd('/').substringAfterLast('/').toIntOrNull()
            val raw = tag.capture("""\stitle="([^"]{2,200})"""")?.removePrefix("Watch ")?.trim()
            val title = adultUnescape(raw) ?: series.title
            if (seen.add(url)) {
                out.add(
                    HentaiEpisode(
                        sourceId = id,
                        series = series.title,
                        title = title,
                        url = url,
                        episode = number ?: HentaiTubes.episodeOf(title),
                        poster = null,
                    )
                )
            }
        }
        return out
    }

    private val SERIES_PATH =
        Regex("""^https?://haho\.moe/anime/[a-z0-9]+$""", RegexOption.IGNORE_CASE)
    private val EPISODE_PATH =
        Regex("""^https?://haho\.moe/anime/[a-z0-9]+/\d+$""", RegexOption.IGNORE_CASE)
}
