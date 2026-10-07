package com.novastream.app.data.hentai

import com.google.gson.JsonParser
import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.adult.adultUnescape
import com.novastream.app.data.adult.capture
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.remote.httpPostJson
import com.novastream.app.data.remote.str
import java.net.URLEncoder

/**
 * Hanime (hanime.tv) built-in source.
 *
 * Hanime is a SPA behind Cloudflare, so the HTML carries no data. It is scraped through its own
 * JSON endpoints instead:
 *  - **search**: `POST https://search.htv-services.com/` with a `search_text` body -> `hits[]`
 *    (`name`, `slug`, `poster_url`).
 *  - **streams**: the video page exposes `hentai_video_id`, which
 *    `GET /api/v8/video?id={id}` answers with `videos_manifest.servers[].streams[].url` (HLS/MP4).
 *
 * Both have an HTML fallback for the case where the JSON shape changes.
 */
object HanimeSource : HentaiSource {

    override val id = "hanime"
    override val name = "Hanime"
    private const val BASE = "https://hanime.tv"
    private const val SEARCH_API = "https://search.htv-services.com/"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val fromApi = runCatching { parseSearch(searchJson(query.trim())) }.getOrDefault(emptyList())
        if (fromApi.isNotEmpty()) return fromApi
        val html = runCatching {
            AdultHttp.get("$BASE/search?search=" + URLEncoder.encode(query.trim(), "UTF-8"), referer = "$BASE/")
        }.getOrNull() ?: return emptyList()
        return parseCards(html)
    }

    private suspend fun searchJson(query: String): String = httpPostJson(
        url = SEARCH_API,
        body = """{"search_text":${jsonString(query)},"tags":[],"tags_mode":"AND","brands":[],""" +
            """"blacklist":[],"order_by":"created_at_unix","ordering":"desc","page":0}""",
        headers = mapOf(
            "Origin" to BASE,
            "Referer" to "$BASE/",
            "Accept" to "application/json",
        ),
    )

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val page = AdultHttp.get(episode.url, referer = "$BASE/")
        val videoId = page.capture(""""hentai_video_id":\s*(\d+)""")
            ?: page.capture("""/api/v8/video\?id=(\d+)""")
        if (videoId != null) {
            val api = runCatching {
                AdultHttp.get("$BASE/api/v8/video?id=$videoId", referer = episode.url)
            }.getOrNull()
            val urls = api?.let { parseManifest(it) }.orEmpty()
            if (urls.isNotEmpty()) return urls.map { HentaiTubes.stream(it, this, episode.url) }
        }
        return HentaiTubes.toStreams(page, this, "$BASE/")
    }

    // ---- Parsing (internal so the tests can pin the contract without a network call) ----

    internal fun parseSearch(json: String): List<HentaiEpisode> {
        val hits = runCatching {
            JsonParser.parseString(json).asJsonObject
                .get("hits")?.takeIf { it.isJsonArray }?.asJsonArray
        }.getOrNull() ?: return emptyList()

        return hits.mapNotNull { el ->
            val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val slug = o.str("slug") ?: return@mapNotNull null
            val raw = o.str("name") ?: return@mapNotNull null
            val title = adultUnescape(raw) ?: raw
            HentaiEpisode(
                sourceId = id,
                series = HentaiTubes.seriesOf(title),
                title = title,
                url = if (slug.startsWith("http")) slug else "$BASE/videos/hentai/$slug",
                episode = HentaiTubes.episodeOf(title),
                poster = o.str("poster_url") ?: o.str("cover_url") ?: o.str("poster"),
            )
        }.distinctBy { it.url }
    }

    /** `videos_manifest.servers[].streams[].url` -> the manifest's playable URLs. */
    internal fun parseManifest(json: String): List<String> {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return emptyList()
        val servers = root.get("videos_manifest")?.takeIf { it.isJsonObject }
            ?.asJsonObject?.get("servers")?.takeIf { it.isJsonArray }?.asJsonArray
            ?: return emptyList()

        val out = LinkedHashSet<String>()
        for (server in servers) {
            val streams = server.takeIf { it.isJsonObject }?.asJsonObject
                ?.get("streams")?.takeIf { it.isJsonArray }?.asJsonArray ?: continue
            for (s in streams) {
                val o = s.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                o.get("url")?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
                    ?.let { out.add(it) }
            }
        }
        return out.toList()
    }

    internal fun parseCards(html: String): List<HentaiEpisode> =
        HentaiTubes.parseCards(html, BASE, id) { href -> href.contains("/videos/hentai/") }

    private fun jsonString(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
