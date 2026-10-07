package com.novastream.app.data.adult

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.remote.str

/**
 * Avgle built-in JAV source.
 *
 * Avgle exposes a plain JSON API (`api.avgle.com/v1`), which makes it the cleanest of the JAV
 * sources when it is reachable: `/videos/{page}?o=mv|bw|tr` for the home rows,
 * `/search/{query}/{page}` for search, `/categories` for categories and `/video/{vid}` for the
 * playable `video_url`. The `o` parameter selects the sort — `bv` = most viewed, `bw` = newest,
 * `tr` = top rated.
 *
 * Avgle has been intermittent for years; when the API is unreachable [AdultHttp] raises an
 * [AdultSourceException] and Real 18+ simply marks this one source unavailable.
 */
object AvgleSource : AdultSource {

    override val id = "avgle"
    override val name = "Avgle"
    private const val BASE = "https://avgle.com"
    private const val API = "https://api.avgle.com/v1"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Most viewed" to "$API/videos/0?o=bv&limit=24",
            "Newest" to "$API/videos/0?o=bw&limit=24",
            "Top rated" to "$API/videos/0?o=tr&limit=24",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$API/search/" + encode(query) + "/0"))

    override suspend fun categories(): List<AdultCategory> {
        val json = fetch("$API/categories")
        val list = runCatching {
            val array = JsonParser.parseString(json).asJsonObject
                .get("response")?.asJsonObject
                ?.get("categories")?.takeIf { it.isJsonArray }?.asJsonArray
            array?.mapNotNull { el ->
                val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val cid = o.str("id") ?: return@mapNotNull null
                val label = adultUnescape(o.str("name")) ?: return@mapNotNull null
                AdultCategory(cid, label, "$API/videos/0?c=$cid&limit=24")
            } ?: emptyList()
        }.getOrDefault(emptyList())
        return list.distinctBy { it.id }
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val vid = Regex("""/video/([^/?#]+)""").find(url)?.groupValues?.get(1)
            ?: url.substringAfterLast('/').takeIf { it.isNotBlank() }

        val videoObj = vid?.let { id ->
            runCatching { fetch("$API/video/$id") }.getOrNull()?.let { json ->
                runCatching {
                    JsonParser.parseString(json).asJsonObject
                        .get("response")?.asJsonObject?.get("video")?.asJsonObject
                }.getOrNull()
            }
        }

        val base = videoObj?.let { parseVideo(it) } ?: AdultVideo(sourceId = id, url = url, title = "Avgle video")
        val description = adultUnescape(videoObj?.str("keyword"))

        val streams = videoObj?.str("video_url")?.let { direct ->
            val quality = adultQualityFromUrl(direct) ?: "Auto"
            listOf(
                StreamSource(
                    url = direct,
                    quality = quality,
                    name = name,
                    title = quality,
                    addonId = id,
                    addonName = name,
                    headers = adultPlayHeaders(BASE),
                )
            )
        }.orEmpty()

        return AdultDetail(
            video = base.copy(url = url),
            description = description,
            streams = streams,
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    /** Parse an API list response: `response.videos[]`. */
    internal fun parseList(json: String): List<AdultVideo> {
        val videos = runCatching {
            JsonParser.parseString(json).asJsonObject
                .get("response")?.asJsonObject
                ?.get("videos")?.takeIf { it.isJsonArray }?.asJsonArray
        }.getOrNull() ?: return emptyList()
        return videos
            .mapNotNull { el -> el.takeIf { it.isJsonObject }?.asJsonObject?.let { parseVideo(it) } }
            .distinctBy { it.url }
    }

    internal fun parseVideo(o: JsonObject): AdultVideo? {
        val vid = o.str("vid") ?: return null
        val title = adultUnescape(o.str("title")) ?: return null
        val seconds = o.str("duration")?.toLongOrNull()
        return AdultVideo(
            sourceId = id,
            url = "$BASE/video/$vid",
            title = title,
            thumbnail = adultUnescape(o.str("thumbnail")),
            duration = seconds?.takeIf { it > 0 }?.let { adultDuration(it) },
            views = o.str("views"),
        )
    }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")
}
