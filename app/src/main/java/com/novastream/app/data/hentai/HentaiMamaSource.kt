package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.adult.adultAbsoluteUrl
import com.novastream.app.data.adult.adultPlayHeaders
import com.novastream.app.data.adult.adultQualityFromUrl
import com.novastream.app.data.adult.adultRx
import com.novastream.app.data.adult.adultUnescape
import com.novastream.app.data.adult.capture
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.remote.httpPostForm
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder

/**
 * HentaiMama (dooplay) built-in source — series pages with a real episode list, and a player
 * whose mirrors are resolved through WordPress admin-ajax into a direct MP4/HLS URL.
 *
 * Resolution chain, all verified against the live site:
 *  1. `GET /search/{title}` -> `article.series-card` = one **series** page.
 *  2. `GET /tvshows/{slug}` -> `a.dt-se-item` = one **episode** page (plus a `Watch Ep n` button
 *     for single-episode series).
 *  3. `GET /episodes/{slug}` -> `data-post="{id}"` + `#option-{n}` mirror boxes (loaded lazily).
 *  4. `POST /wp-admin/admin-ajax.php` (`action=get_player_contents&a={id}&i={n}`) -> JSON array
 *     whose `n-1` entry holds the player `<iframe>` pointing at `/?dt_embed=…`.
 *  5. `GET {iframe}` -> jwplayer page with the direct media URL (`… .mp4` / `… .m3u8`).
 */
object HentaiMamaSource : HentaiSource {

    override val id = "hentaimama"
    override val name = "HentaiMama"

    /** Primary host first, then its mirror, so one outage doesn't take the source down. */
    private val hosts = listOf("https://hentaimama.io", "https://www.hentaimama.com")

    /** Series expanded per search — beyond the top few the results are unrelated titles anyway. */
    private const val MAX_SERIES = 3

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        for (host in hosts) {
            val html = runCatching { AdultHttp.get("$host/search/$q", referer = "$host/") }.getOrNull()
                ?: continue
            val series = parseSeries(html, host)
            if (series.isEmpty()) continue
            return expand(series)
        }
        return emptyList()
    }

    /** Expands the top series matches into episode pages, concurrently. */
    private suspend fun expand(series: List<HentaiEpisode>): List<HentaiEpisode> = coroutineScope {
        series.take(MAX_SERIES)
            .map { s -> async { runCatching { episodesOf(s) }.getOrDefault(emptyList()) } }
            .flatMap { it.await() }
    }

    private suspend fun episodesOf(series: HentaiEpisode): List<HentaiEpisode> {
        val html = AdultHttp.get(series.url, referer = rootOf(series.url))
        return parseEpisodes(html, series)
    }

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val referer = rootOf(episode.url)
        val page = AdultHttp.get(episode.url, referer = referer)
        val postId = page.capture("""data-post="(\d+)"""") ?: return emptyList()
        val mirrors = adultRx("""#option-(\d+)""").findAll(page)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .distinct().sorted()
            .toList().ifEmpty { listOf(1) }

        for (n in mirrors) {
            val raw = runCatching {
                httpPostForm(
                    url = "$referer/wp-admin/admin-ajax.php",
                    fields = mapOf(
                        "action" to "get_player_contents",
                        "a" to postId,
                        "i" to n.toString(),
                    ),
                    headers = mapOf(
                        "User-Agent" to AdultHttp.BROWSER_UA,
                        "Referer" to episode.url,
                        "X-Requested-With" to "XMLHttpRequest",
                    ),
                )
            }.getOrNull() ?: continue
            val iframe = parsePlayerIframe(raw) ?: continue
            val player = runCatching { AdultHttp.get(iframe, referer = episode.url) }.getOrNull() ?: continue
            val urls = directMediaUrls(player)
            if (urls.isNotEmpty()) {
                return urls.map { toStream(it, referer) }
            }
        }
        return emptyList()
    }

    private fun toStream(url: String, referer: String): StreamSource = StreamSource(
        url = url,
        name = name,
        addonId = id,
        addonName = name,
        quality = adultQualityFromUrl(url),
        headers = adultPlayHeaders(referer),
    )

    // ---- Parsing (internal so the tests can pin the contract without a network call) ----

    /** `article.series-card` blocks -> one series-level entry per result. */
    internal fun parseSeries(html: String, host: String): List<HentaiEpisode> =
        adultRx("""<article class="series-card">(.*?)</article>""")
            .findAll(html)
            .mapNotNull { m ->
                val block = m.groupValues[1]
                val title = adultUnescape(
                    block.capture("""<h3 class="sc-title"><a[^>]*>([^<]+)</a>""")
                        ?: block.capture("""alt="([^"]+)"""),
                ) ?: return@mapNotNull null
                val url = block.capture("""<h3 class="sc-title"><a href="([^"]+)"""")
                    ?: block.capture("""sc-poster" href="([^"]+)""")
                    ?: return@mapNotNull null
                HentaiEpisode(
                    sourceId = id,
                    series = title,
                    title = title,
                    url = absolutise(url, host),
                    poster = block.capture("""<img[^>]*src="([^"]+)"""),
                )
            }
            .toList()

    /** `a.dt-se-item` blocks -> episode pages; falls back to the `Watch Ep n` button. */
    internal fun parseEpisodes(html: String, series: HentaiEpisode): List<HentaiEpisode> {
        val host = rootOf(series.url)
        val found = adultRx("""<a class="dt-se-item" href="([^"]+)">(.*?)</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val block = m.groupValues[2]
                val ep = block.capture("""dt-se-num">\s*EP\s*(\d+)""")?.toIntOrNull()
                HentaiEpisode(
                    sourceId = id,
                    series = series.series,
                    title = adultUnescape(block.capture("""dt-se-title">([^<]+)</a>"""))
                        ?: adultUnescape(block.capture("""alt="([^"]+)"""))
                        ?: "Episode ${ep ?: ""}".trim(),
                    url = absolutise(m.groupValues[1], host),
                    episode = ep,
                    poster = block.capture("""<img[^>]*src="([^"]+)"""),
                )
            }
            .toList()
        if (found.isNotEmpty()) return found

        // Single-episode series list only the "Watch Ep 1" button instead of the episode list.
        val play = html.capture("""<a class="dsc-play" href="([^"]+)"""") ?: return emptyList()
        val ep = html.capture("""Watch\s+Ep\s+(\d+)""")?.toIntOrNull() ?: 1
        return listOf(
            HentaiEpisode(
                sourceId = id,
                series = series.series,
                title = "Episode $ep",
                url = absolutise(play, host),
                episode = ep,
            ),
        )
    }

    /**
     * Extracts the player iframe out of one admin-ajax mirror entry. The response is a JSON
     * array of HTML fragments with `\/`-escaped URLs and `&#038;`-escaped query separators, so
     * the payload is unescaped before the `src` is pulled.
     */
    internal fun parsePlayerIframe(raw: String): String? {
        val html = raw
            .replace("\\/", "/")
            .replace("\\\"", "\"")
            .replace("&#038;", "&")
            .replace("&amp;", "&")
        return html.capture("""<iframe[^>]*src="([^"]+)"""")?.takeIf { it.startsWith("http") }
    }

    /**
     * Direct media URLs out of the jwplayer page. `_snapshot` preview images also end in `.mp4`
     * inside a longer name, so the lookahead only accepts a URL that *stops* at the extension.
     */
    internal fun directMediaUrls(playerHtml: String): List<String> =
        adultRx("""https?://[^"'\s<>\\]+?(?:\.mp4|\.m3u8)(?=[?#"'<>\s]|$)""")
            .findAll(playerHtml.replace("\\/", "/"))
            .mapNotNull { adultUnescape(it.value) }
            .distinct()
            .toList()

    private fun absolutise(url: String, host: String): String = when {
        url.startsWith("http") -> adultAbsoluteUrl(url) ?: url
        url.startsWith("//") -> adultAbsoluteUrl(url) ?: "https:$url"
        else -> host.trimEnd('/') + if (url.startsWith("/")) url else "/$url"
    }

    private fun rootOf(url: String): String =
        Regex("""^https?://[^/]+""").find(url)?.value ?: "https://hentaimama.io"
}
