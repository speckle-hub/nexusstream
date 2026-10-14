package com.novastream.app.data.adult

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.remote.str

/**
 * Beeg built-in source.
 *
 * Beeg's pages are a JS app, so listings and search go through its public v6 JSON API
 * (`api.beeg.com/api/v6/us/…`) with the HTML cards as a fallback: home rows from the index
 * endpoints, search from the search endpoint, and streams from the per-video endpoint's
 * `file` map (quality-labelled MP4/HLS URLs), falling back to [TubeMedia] on the video page.
 * Every hop is defensive — an API shape change degrades to a per-source notice, never a tab
 * failure.
 */
object BeegSource : AdultSource {

    override val id = "beeg"
    override val name = "Beeg"
    private const val BASE = "https://beeg.com"
    private const val API = "https://api.beeg.com/api/v6/us"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to "$API/index/main/0/pc",
            "Popular" to "$API/index/top/0/pc",
            "Trending" to "$API/index/trending/0/pc",
        ),
    ) { url -> parseApi(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> {
        val api = runCatching { parseApi(fetch("$API/search/" + encode(query) + "/1/pc")) }
            .getOrDefault(emptyList())
        if (api.isNotEmpty()) return api
        // HTML fallback: the search page embeds the same cards server-side for crawlers.
        return runCatching { parseList(AdultHttp.get("$BASE/search?q=" + encode(query), referer = BASE)) }
            .getOrDefault(emptyList())
    }

    override suspend fun categories(): List<AdultCategory> =
        STATIC_SECTIONS.map { (slug, label) -> AdultCategory(slug, label, "$API/index/$slug/0/pc") }

    override suspend fun category(category: AdultCategory): List<AdultVideo> {
        if (category.url.contains("api.beeg.com")) {
            return runCatching { parseApi(fetch(category.url)) }.getOrDefault(emptyList())
        }
        return parseList(fetch(category.url))
    }

    override suspend fun detail(url: String): AdultDetail {
        val videoId = Regex("""/(\d+)/?$""").find(url)?.groupValues?.get(1)
            ?: url.trimEnd('/').substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() }
        val apiStreams = videoId?.let { vid ->
            runCatching { fetch("$API/video/$vid") }.getOrNull()?.let { json ->
                runCatching {
                    JsonParser.parseString(json).let { root ->
                        val o = when {
                            root.isJsonObject && root.asJsonObject.has("file") -> root.asJsonObject
                            root.isJsonObject -> root.asJsonObject.get("video")?.takeIf { it.isJsonObject }?.asJsonObject
                                ?: root.asJsonObject
                            else -> null
                        }
                        o?.let { streamsOf(it, url) }
                    }
                }.getOrNull()
            }
        }.orEmpty()
        val html = runCatching { fetch(url) }.getOrNull()
        val title: String = html?.let {
            adultUnescape(it.ogContent("og:title"))
                ?: adultCollapse(it.capture("""<h1[^>]*>(.*?)</h1>""")?.let { h -> stripTags(h) })
        } ?: "Beeg video"
        val thumb = html?.let { adultUnescapeJson(it.ogContent("og:image")) }
        val pageStreams = html?.let { TubeMedia.streams(it, this, referer = BASE) }.orEmpty()
        val streams = (apiStreams + pageStreams).distinctBy { it.url }
        return AdultDetail(
            video = AdultVideo(sourceId = id, url = url, title = title, thumbnail = thumb),
            streams = streams,
            related = html?.let { runCatching { parseList(it).filterNot { v -> v.url == url }.take(12) }.getOrDefault(emptyList()) }
                .orEmpty(),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    /** Parse a v6 index/search response: root `videos[]` (tolerates `items[]` too). */
    internal fun parseApi(json: String): List<AdultVideo> {
        val array = runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            (root.get("videos") ?: root.get("items"))?.takeIf { it.isJsonArray }?.asJsonArray
        }.getOrNull() ?: return emptyList()
        return array
            .mapNotNull { el -> el.takeIf { it.isJsonObject }?.asJsonObject?.let { parseVideo(it) } }
            .distinctBy { it.url }
    }

    internal fun parseVideo(o: JsonObject): AdultVideo? {
        val rawId = o.str("id") ?: return null
        val title = adultUnescape(o.str("title") ?: o.str("name")) ?: return null
        val thumb = adultUnescape(o.str("image") ?: o.str("poster") ?: o.str("thumb") ?: o.str("thumbnail"))
            ?.let { absoluteMedia(it) }
        val seconds = (o.str("duration") ?: o.str("length_sec") ?: o.str("length"))?.toLongOrNull()
        return AdultVideo(
            sourceId = id,
            url = "$BASE/$rawId",
            title = title,
            thumbnail = thumb,
            duration = seconds?.takeIf { it > 0 }?.let { adultDuration(it) },
            views = o.str("views"),
        )
    }

    /** Streams from a v6 video/file object: the `file` map (quality → URL), highest first. */
    internal fun streamsOf(o: JsonObject, pageUrl: String): List<StreamSource> {
        val file = o.get("file") ?: return emptyList()
        val entries: List<Pair<String, String>> = when {
            file.isJsonObject -> file.asJsonObject.entrySet().mapNotNull { (k, v) ->
                val raw = v.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
                val url = absoluteMedia(raw) ?: return@mapNotNull null
                k to url
            }
            file.isJsonPrimitive -> listOf("Auto" to (absoluteMedia(file.asString) ?: return emptyList()))
            else -> return emptyList()
        }
        val headers = adultPlayHeaders(pageUrl)
        return entries.map { (key, url) ->
            val quality = Regex("""(\d{3,4})p?""").find(key)?.groupValues?.get(1)?.plus("p")
                ?: adultQualityFromUrl(url)
                ?: if (url.contains(".m3u8", ignoreCase = true)) "HLS" else "Auto"
            StreamSource(
                url = url,
                quality = quality,
                name = name,
                title = quality,
                addonId = id,
                addonName = name,
                headers = headers,
            )
        }.distinctBy { it.url }
            .sortedByDescending { it.quality?.removeSuffix("p")?.toIntOrNull() ?: -1 }
    }

    /** Parse HTML cards as a fallback (crawler markup links `/…/<id>-<slug>` video pages). */
    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*beeg\.com)?/\d+[^"#?]*)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2500))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: continue
            if (title.isBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = absolute(href),
                    title = adultCollapse(title) ?: continue,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""(?:duration|time)[^>]*>\s*([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    private fun absolute(href: String): String = when {
        href.startsWith("http") -> href
        href.startsWith("//") -> "https:$href"
        else -> BASE + href
    }

    private fun absoluteMedia(raw: String): String? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        return adultAbsoluteUrl(clean)
            ?: adultAbsoluteUrl("https://video.beeg.com/$clean")
            ?: ("https://video.beeg.com/" + clean.trimStart('/'))
    }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val STATIC_SECTIONS = listOf(
        "main" to "Latest",
        "top" to "Popular",
        "trending" to "Trending",
    )
}
