package com.moovio.desktop.data.remote

import com.moovio.desktop.data.net.Http
import com.moovio.desktop.data.net.arrayOrNull
import com.moovio.desktop.data.net.double
import com.moovio.desktop.data.net.int
import com.moovio.desktop.data.net.obj
import com.moovio.desktop.data.net.objectOrNull
import com.moovio.desktop.data.net.parseJsonObject
import com.moovio.desktop.data.net.plainDescription
import com.moovio.desktop.data.net.str
import com.moovio.desktop.data.net.strings
import com.moovio.desktop.domain.model.Episode
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.MetaDetail
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.JsonObject

/**
 * Anime discovery + detail metadata from AniList's public GraphQL API (no key required).
 * Ids are namespaced as `anilist:<id>`; an IMDb id is captured from external links whenever
 * AniList exposes one, because stream add-ons key off IMDb ids.
 */
class AniListClient(private val http: Http) {

    private val endpoint = "https://graphql.anilist.co"
    private val cache = TtlCache<String, List<MediaItem>>(ttlMs = 10 * 60_000)
    private val detailCache = TtlCache<String, MetaDetail>(ttlMs = 30 * 60_000)

    private val listFields = """
        id
        idMal
        title { romaji english native }
        coverImage { extraLarge large }
        bannerImage
        description(asHtml: false)
        averageScore
        seasonYear
        genres
        episodes
        duration
        format
        status
        externalLinks { site url }
    """.trimIndent()

    suspend fun trending(limit: Int = 24): List<MediaItem> =
        page("trending:$limit", sort = listOf("TRENDING_DESC", "POPULARITY_DESC"), limit = limit)

    suspend fun popular(limit: Int = 24): List<MediaItem> =
        page("popular:$limit", sort = listOf("POPULARITY_DESC", "SCORE_DESC"), limit = limit)

    suspend fun topRated(limit: Int = 24): List<MediaItem> =
        page("top:$limit", sort = listOf("SCORE_DESC", "POPULARITY_DESC"), limit = limit)

    suspend fun search(query: String, limit: Int = 24): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        cache.get("search:$query")?.let { return it }
        val document = """
            query (${'$'}page: Int, ${'$'}perPage: Int, ${'$'}search: String) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(type: ANIME, isAdult: false, search: ${'$'}search, sort: [SEARCH_MATCH, POPULARITY_DESC]) {
                  $listFields
                }
              }
            }
        """.trimIndent()
        val body = buildJsonObject {
            put("query", document)
            putJsonObject("variables") {
                put("page", 1)
                put("perPage", limit)
                put("search", query)
            }
        }.toString()
        val items = runCatching { request(body) }.getOrDefault(emptyList())
        cache.put("search:$query", items)
        return items
    }

    private suspend fun page(key: String, sort: List<String>, limit: Int): List<MediaItem> {
        cache.get(key)?.let { return it }
        val document = """
            query (${'$'}page: Int, ${'$'}perPage: Int, ${'$'}sort: [MediaSort]) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(type: ANIME, isAdult: false, sort: ${'$'}sort) {
                  $listFields
                }
              }
            }
        """.trimIndent()
        val body = buildJsonObject {
            put("query", document)
            putJsonObject("variables") {
                put("page", 1)
                put("perPage", limit)
                putJsonArray("sort") { sort.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
            }
        }.toString()
        val items = runCatching { request(body) }.getOrDefault(emptyList())
        if (items.isNotEmpty()) cache.put(key, items)
        return items
    }

    private suspend fun request(body: String): List<MediaItem> {
        val json = parseJsonObject(http.postJson(endpoint, body))
        val media = json["data"].obj()?.get("Page").obj()?.get("media").let { it as? JsonArray }
            ?: return emptyList()
        return media.mapNotNull { it.obj() }.mapNotNull { parseItem(it) }
    }

    suspend fun detail(id: String): MetaDetail? {
        val numericId = id.removePrefix("anilist:").toIntOrNull() ?: return null
        detailCache.get("anilist:$numericId")?.let { return it }
        val document = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                $listFields
                streamingEpisodes { title thumbnail url }
                nextAiringEpisode { episode }
              }
            }
        """.trimIndent()
        val body = buildJsonObject {
            put("query", document)
            putJsonObject("variables") { put("id", numericId) }
        }.toString()
        val json = runCatching { parseJsonObject(http.postJson(endpoint, body)) }.getOrNull() ?: return null
        val media = json["data"].obj()?.get("Media").obj() ?: return null
        val detail = parseDetail(media)
        detailCache.put("anilist:$numericId", detail)
        return detail
    }

    private fun parseItem(json: JsonObject): MediaItem? {
        val id = json.int("id") ?: return null
        val titles = json["title"].obj()
        val name = titles.str("english") ?: titles.str("romaji") ?: titles.str("native") ?: return null
        val cover = json["coverImage"].obj()
        return MediaItem(
            id = "anilist:$id",
            type = MediaType.ANIME,
            name = name,
            poster = cover.str("extraLarge") ?: cover.str("large"),
            background = json.str("bannerImage"),
            year = json.int("seasonYear"),
            rating = json.double("averageScore")?.div(10.0),
            description = json.str("description").plainDescription(),
            genres = json.arrayOrNull("genres").strings(),
            releaseInfo = json.str("seasonYear") ?: json.str("format"),
            runtime = json.int("duration")?.let { "$it min" },
            imdbId = imdbIdOf(json),
        )
    }

    private fun parseDetail(json: JsonObject): MetaDetail {
        val id = json.int("id") ?: error("AniList media without id")
        val titles = json["title"].obj()
        val name = titles.str("english") ?: titles.str("romaji") ?: titles.str("native") ?: "Unknown"
        val cover = json["coverImage"].obj()
        val episodeCount = json.int("episodes")
            ?: json.objectOrNull("nextAiringEpisode")?.int("episode")?.minus(1)
            ?: 0
        val streaming = json.arrayOrNull("streamingEpisodes")?.mapNotNull { it.obj() } ?: emptyList()
        val episodes = (1..episodeCount.coerceAtMost(2000)).map { number ->
            val streamingInfo = streaming.getOrNull(number - 1)
            Episode(
                id = "anilist:$id:1:$number",
                season = 1,
                episode = number,
                title = streamingInfo.str("title")?.substringBefore(" - Episode")?.trim()
                    ?: "Episode $number",
                thumbnail = streamingInfo.str("thumbnail"),
            )
        }
        return MetaDetail(
            id = "anilist:$id",
            type = MediaType.ANIME,
            name = name,
            poster = cover.str("extraLarge") ?: cover.str("large"),
            background = json.str("bannerImage"),
            description = json.str("description").plainDescription(),
            year = json.int("seasonYear"),
            releaseInfo = buildList {
                json.str("format")?.let { add(it.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)) }
                json.int("seasonYear")?.let { add(it.toString()) }
            }.joinToString(" · ").takeIf { it.isNotBlank() },
            rating = json.double("averageScore")?.div(10.0),
            runtime = json.int("duration")?.let { "$it min" },
            genres = json.arrayOrNull("genres").strings(),
            episodes = episodes,
            malId = json.int("idMal"),
            imdbId = imdbIdOf(json),
        )
    }

    private fun imdbIdOf(json: JsonObject): String? =
        json.arrayOrNull("externalLinks")
            ?.mapNotNull { it.obj() }
            ?.firstOrNull { it.str("site").equals("IMDb", true) }
            ?.str("url")
            ?.let { Regex("""tt\d{5,10}""").find(it)?.value }
}
