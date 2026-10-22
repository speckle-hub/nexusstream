package com.moovio.app.data.sfw

import com.google.gson.JsonParser
import com.moovio.app.data.adult.AdultHttp
import com.moovio.app.data.adult.adultRx
import com.moovio.app.data.adult.adultUnescape
import com.moovio.app.data.adult.capture
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.remote.str
import java.net.URLEncoder

/**
 * FlixHQ built-in source (movies + TV).
 *
 * FlixHQ mirrors share one layout: `/search/{query}` cards, a watch page
 * (`/movie/watch-{slug}-{id}` / `/tv/watch-{slug}-{id}`), and a JSON ajax ladder
 * (`movie/episodes` or `season/list` → `season/episodes` → `episode/servers` →
 * `server/{id}` → `{link}` embed). The final `{link}` is resolved through
 * [SfwEmbeds.resolveChain] like any other embed. Mirrors rotate, so every fetch
 * tries the mirror list in order with per-mirror isolation.
 */
object FlixhqSource : SfwSource {

    override val id = "flixhq"
    override val name = "FlixHQ"

    private val MIRRORS = listOf(
        "https://flixhq.to",
        "https://flixhq.bz",
        "https://flixhq.li",
    )

    private fun base(): String = MIRRORS.first()

    override suspend fun home(): List<SfwRow> {
        val rows = ArrayList<SfwRow>()
        var lastError: Exception? = null
        for ((title, path, kind) in listOf(
            Triple("Trending Movies", "/trending/movies", MediaType.MOVIE),
            Triple("Trending TV", "/trending/tv-shows", MediaType.SERIES),
            Triple("Latest Movies", "/movies", MediaType.MOVIE),
        )) {
            val titles = runCatching {
                val html = get(path)
                SfwEmbeds.parseTitleCards(html, base(), id, kind, ::isWatchLink)
            }.onFailure { lastError = it as? Exception ?: Exception(it) }.getOrDefault(emptyList())
            if (titles.isNotEmpty()) rows.add(SfwRow("$name · $title", id, kind, titles))
        }
        if (rows.isEmpty() && lastError != null) throw lastError!!
        return rows
    }

    override suspend fun search(query: String): List<SfwTitle> {
        val q = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "-")
        val html = get("/search/$q")
        val movies = SfwEmbeds.parseTitleCards(html, base(), id, MediaType.MOVIE, ::isMovieLink)
        val series = SfwEmbeds.parseTitleCards(html, base(), id, MediaType.SERIES, ::isTvLink)
        return (movies + series).distinctBy { it.url }
    }

    override suspend fun detail(url: String): SfwDetail {
        val html = get(url)
        val title = adultUnescape(html.capture("""<h2[^>]*class="[^"]*heading[^"]*"[^>]*>(.*?)</h2>""")?.let { strip(it) })
            ?: adultUnescape(html.ogTitle())
            ?: "FlixHQ title"
        val poster = html.capture("""<img[^>]*class="[^"]*film-poster-img[^"]*"[^>]*src="([^"]+)"""")
            ?.let { SfwEmbeds.absolutise(it, base()) }
        val kind = if ("/tv/" in url || isTvLink(url)) MediaType.SERIES else MediaType.MOVIE
        val desc = html.capture("""<div[^>]*class="[^"]*description[^"]*"[^>]*>(.*?)</div>""")?.let { strip(it) }
        val dataId = html.capture("""data-id="(\d+)"""") ?: Regex("""/(\d+)$""").find(url)?.groupValues?.get(1)
            ?: throw SfwSourceException("No FlixHQ id")
        val episodes = if (kind == MediaType.SERIES) seriesEpisodes(dataId, title) else emptyList()
        val streams = if (kind == MediaType.MOVIE) movieStreams(dataId) else emptyList()
        return SfwDetail(
            title = SfwTitle(id, url, title, kind, poster),
            description = desc,
            poster = poster,
            episodes = episodes,
            streams = streams,
        )
    }

    override suspend fun streamsForEpisode(episode: SfwEpisode): List<StreamSource> {
        // Series episodes carry their ajax season id in the stored page URL
        // (`…/ajax/episode/list/{seasonId}`), so no extra lookup is needed.
        val seasonId = Regex("""/ajax/episode/list/(\d+)""").find(episode.url)?.groupValues?.get(1)
            ?: throw SfwSourceException("No season for episode")
        val episodeId = episodeIdFor(seasonId, episode.episode)
            ?: throw SfwSourceException("Episode not listed")
        return serverStreams(episodeId)
    }

    // ---- Ajax ladder (internal so tests pin each hop without network) ----

    internal suspend fun seriesEpisodes(dataId: String, series: String): List<SfwEpisode> {
        val seasonsHtml = ajaxHtml("/ajax/season/list/$dataId")
        val seasonIds = seasonIds(seasonsHtml)
        val out = ArrayList<SfwEpisode>()
        for ((seasonNum, seasonId) in seasonIds) {
            val epsHtml = ajaxHtml("/ajax/season/episodes/$seasonId")
            out += episodeEntries(epsHtml).map { (epNum, epTitle) ->
                SfwEpisode(
                    sourceId = id,
                    series = series,
                    title = epTitle.ifBlank { "Episode $epNum" },
                    url = "${base()}/ajax/episode/list/$seasonId",
                    season = seasonNum,
                    episode = epNum,
                )
            }
        }
        return out
    }

    internal suspend fun movieStreams(dataId: String): List<StreamSource> {
        // Movies list a single default episode entry — its data-id feeds the server ladder.
        val epsHtml = ajaxHtml("/ajax/movie/episodes/$dataId")
        val episodeId = epsHtml.capture("""data-id="(\d+)"""")
            ?: throw SfwSourceException("No FlixHQ streams")
        return serverStreams(episodeId)
    }

    internal suspend fun serverStreams(episodeId: String): List<StreamSource> {
        val serversHtml = ajaxHtml("/ajax/episode/servers/$episodeId")
        val servers = serverEntries(serversHtml)
        for ((serverId, _) in servers) {
            val link = ajaxLink("/ajax/server/$serverId")
            if (link.isNullOrBlank() || !link.startsWith("http")) continue
            val streams = runCatching {
                val embed = AdultHttp.get(link, referer = base())
                SfwEmbeds.resolveChain(embed, link, id, name)
            }.getOrDefault(emptyList())
            if (streams.isNotEmpty()) return streams
        }
        return emptyList()
    }

    private suspend fun episodeIdFor(seasonId: String, episode: Int?): String? {
        val epsHtml = runCatching { ajaxHtml("/ajax/season/episodes/$seasonId") }.getOrNull()
            ?: return null
        val entries = episodeEntries(epsHtml)
        val epNum = episode ?: 1
        return entries.firstOrNull { (n, _) -> n == epNum }?.let {
            epsHtml.capture("""data-id="(\d+)"[^>]*data-num="$epNum"""")
                ?: epsHtml.capture("""data-num="$epNum"[^>]*data-id="(\d+)"""")
                ?: epsHtml.capture("""data-id="(\d+)"""")
        }
    }

    internal fun seasonIds(html: String): List<Pair<Int, String>> =
        adultRx("""data-id="(\d+)"[^>]*>.*?Season\s*(\d+)""").findAll(html)
            .map { it.groupValues[1] to (it.groupValues[2].toIntOrNull() ?: 0) }
            .filter { it.second > 0 }
            .map { (id, num) -> num to id }
            .distinct()
            .toList()

    internal fun episodeEntries(html: String): List<Pair<Int, String>> =
        adultRx("""data-id="(\d+)"[^>]*data-num="(\d+)"[^>]*>(.*?)</a>""").findAll(html)
            .mapNotNull { m ->
                val num = m.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                num to (strip(m.groupValues[3]).ifBlank { "Episode $num" })
            }
            .distinctBy { it.first }
            .toList()

    internal fun serverEntries(html: String): List<Pair<String, String>> =
        adultRx("""data-id="(\d+)"[^>]*>(.*?)</a>""").findAll(html)
            .map { it.groupValues[1] to strip(it.groupValues[2]) }
            .filter { it.first.isNotBlank() }
            .distinctBy { it.first }
            .toList()

    /** The `{link}` embed URL behind a server entry (`{"link": "https://…"}`). */
    internal suspend fun ajaxLink(path: String): String? {
        val json = runCatching { get(path) }.getOrNull() ?: return null
        return runCatching {
            JsonParser.parseString(json).asJsonObject.str("link")
                ?.takeIf { it.startsWith("http") }
        }.getOrNull()
    }

    private suspend fun ajaxHtml(path: String): String {
        val json = get(path)
        return runCatching {
            JsonParser.parseString(json).asJsonObject.str("html").orEmpty()
        }.getOrDefault(json)
    }

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
        throw lastError ?: SfwSourceException("FlixHQ unreachable")
    }

    internal fun isWatchLink(href: String): Boolean =
        href.contains("/movie/watch-") || href.contains("/tv/watch-")

    internal fun isMovieLink(href: String): Boolean = href.contains("/movie/watch-")
    internal fun isTvLink(href: String): Boolean = href.contains("/tv/watch-")

    private fun strip(s: String): String =
        s.replace(Regex("<[^>]*>"), " ").replace(Regex("""\s+"""), " ").trim()

    private fun String.ogTitle(): String? =
        capture("""<meta[^>]*property="og:title"[^>]*content="([^"]+)""")
            ?: capture("""<meta[^>]*content="([^"]+)"[^>]*property="og:title"""")
}
