package com.novastream.app.data.remote

import com.google.gson.JsonObject
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.MetaDetail

/** AniList GraphQL client — used for anime and NSFW anime metadata. */
object AniListClient {
    private const val ENDPOINT = "https://graphql.anilist.co"

    private const val FIELDS = """
        id idMal
        title { romaji english native }
        coverImage { extraLarge large }
        bannerImage
        description(asHtml: false)
        averageScore favourites
        startDate { year }
        status
        genres episodes
        isAdult
        studios(isMain: true) { nodes { name } }
    """

    private const val DETAIL_FIELDS = """
        id idMal
        title { romaji english native }
        synonyms
        coverImage { extraLarge large }
        bannerImage
        description(asHtml: false)
        averageScore meanScore favourites popularity
        startDate { year }
        endDate { year }
        status
        genres
        episodes duration
        isAdult
        format season
        trailer { id site }
        studios(isMain: true) { nodes { name } }
        characters(sort: [ROLE, RELEVANCE], perPage: 16) {
            edges { role node { name { full } image { large medium } } }
        }
        recommendations(sort: RATING_DESC, perPage: 12) {
            nodes { mediaRecommendation { id title { romaji english } coverImage { large } averageScore type } }
        }
    """

    private fun query(search: String?, isAdult: Boolean, page: Int, sort: String, genre: String? = null): String {
        // NOTE: page/perPage are Page-level arguments, NOT media-level arguments.
        // Passing them to media(...) makes AniList reject the whole query (empty results).
        val args = buildString {
            append("type: ANIME")
            append(", isAdult: $isAdult")
            if (!search.isNullOrBlank()) append(", search: \"${search.replace("\"", "")}\"")
            if (!genre.isNullOrBlank()) append(", genre: \"$genre\"")
            append(", sort: $sort")
        }
        return "{ Page(page: $page, perPage: 40) { media($args) { $FIELDS } } }"
    }

    private suspend fun exec(query: String, isNsfw: Boolean = false): List<MediaItem> {
        val body = Http.gson.toJson(mapOf("query" to query))
        val resp = parseJson(httpPostJsonCached(ENDPOINT, body)).asJsonObject
        val media = resp.getAsJsonObject("data")?.getAsJsonObject("Page")?.getAsJsonArray("media") ?: return emptyList()
        return media.mapNotNull { if (it.isJsonObject) toItem(it.asJsonObject, isNsfw = isNsfw) else null }
    }

    private fun toItem(o: JsonObject, isNsfw: Boolean): MediaItem? {
        val id = o.get("id")?.asInt?.toString() ?: return null
        val title = o.getAsJsonObject("title")?.let {
            it.get("english")?.takeIf { e -> !e.isJsonNull }?.asString
                ?: it.get("romaji")?.asString
                ?: it.get("native")?.asString
        } ?: return null
        val adult = o.get("isAdult")?.asBoolean ?: false
        return MediaItem(
            id = id,
            type = if (isNsfw || adult) MediaType.NSFW_ANIME else MediaType.ANIME,
            title = title,
            poster = o.getAsJsonObject("coverImage")?.get("extraLarge")?.asString
                ?: o.getAsJsonObject("coverImage")?.get("large")?.asString,
            backdrop = o.get("bannerImage")?.takeIf { !it.isJsonNull }?.asString,
            year = o.getAsJsonObject("startDate")?.get("year")?.takeIf { !it.isJsonNull }?.asInt?.toString(),
            rating = o.get("averageScore")?.takeIf { !it.isJsonNull }?.asInt?.let { it / 10.0 },
            description = o.get("description")?.takeIf { !it.isJsonNull }?.asString?.replace(Regex("<[^>]*>"), ""),
            genres = o.getAsJsonArray("genres")?.map { it.asString } ?: emptyList(),
            addonId = "anilist",
            addonName = "AniList",
        )
    }

    suspend fun popularAnime(): List<MediaItem> = exec(query(null, false, 1, "POPULARITY_DESC"))
    suspend fun trendingAnime(): List<MediaItem> = exec(query(null, false, 1, "TRENDING_DESC"))
    suspend fun searchAnime(q: String): List<MediaItem> = exec(query(q, false, 1, "POPULARITY_DESC"))
    suspend fun byGenre(genre: String): List<MediaItem> = exec(query(null, false, 1, "POPULARITY_DESC", genre))

    // ---- NSFW anime (isAdult: true) — reliable hard-coded source, no add-ons required ----
    suspend fun nsfwPopular(): List<MediaItem> = exec(query(null, true, 1, "POPULARITY_DESC"), isNsfw = true)
    suspend fun nsfwTrending(): List<MediaItem> = exec(query(null, true, 1, "TRENDING_DESC"), isNsfw = true)
    suspend fun nsfwTopRated(): List<MediaItem> = exec(query(null, true, 1, "SCORE_DESC"), isNsfw = true)
    suspend fun nsfwRecent(): List<MediaItem> = exec(query(null, true, 1, "START_DATE_DESC"), isNsfw = true)
    suspend fun nsfwByGenre(genre: String): List<MediaItem> = exec(query(null, true, 1, "POPULARITY_DESC", genre), isNsfw = true)
    suspend fun nsfwSearch(q: String): List<MediaItem> = exec(query(q, true, 1, "POPULARITY_DESC"), isNsfw = true)

    suspend fun details(id: String, byMal: Boolean = false): MetaDetail? {
        val key = if (byMal) "idMal" else "id"
        val q = "{ Media($key: $id, type: ANIME) { $DETAIL_FIELDS } }"
        val body = Http.gson.toJson(mapOf("query" to q))
        val resp = parseJson(httpPostJsonCached(ENDPOINT, body)).asJsonObject
        val m = resp.getAsJsonObject("data")?.getAsJsonObject("Media") ?: return null
        val titleObj = m.getAsJsonObject("title")
        val title = titleObj?.let {
            it.get("english")?.takeIf { e -> !e.isJsonNull }?.asString ?: it.get("romaji")?.asString
        } ?: return null

        val altTitles = buildList {
            titleObj?.get("romaji")?.takeIf { !it.isJsonNull }?.asString?.let { add(it) }
            titleObj?.get("native")?.takeIf { !it.isJsonNull }?.asString?.let { add(it) }
            m.getAsJsonArray("synonyms")?.forEach { s -> s.asString?.let { add(it) } }
        }.filter { it.isNotBlank() && it != title }.distinct().take(6)

        val studios = m.getAsJsonObject("studios")?.getAsJsonArray("nodes")
            ?.mapNotNull { it.asJsonObject.get("name")?.asString } ?: emptyList()

        val castMembers = m.getAsJsonObject("characters")?.getAsJsonArray("edges")?.mapNotNull { e ->
            if (!e.isJsonObject) return@mapNotNull null
            val eo = e.asJsonObject
            val node = eo.getAsJsonObject("node") ?: return@mapNotNull null
            val name = node.getAsJsonObject("name")?.get("full")?.asString ?: return@mapNotNull null
            com.novastream.app.data.model.CastMember(
                name = name,
                character = eo.get("role")?.asString,
                image = node.getAsJsonObject("image")?.get("large")?.asString
                    ?: node.getAsJsonObject("image")?.get("medium")?.asString,
            )
        } ?: emptyList()

        val recommendations = m.getAsJsonObject("recommendations")?.getAsJsonArray("nodes")?.mapNotNull { n ->
            if (!n.isJsonObject) return@mapNotNull null
            val rec = n.asJsonObject.getAsJsonObject("mediaRecommendation") ?: return@mapNotNull null
            val rid = rec.get("id")?.asInt?.toString() ?: return@mapNotNull null
            val rtitle = rec.getAsJsonObject("title")?.let {
                it.get("english")?.takeIf { e -> !e.isJsonNull }?.asString ?: it.get("romaji")?.asString
            } ?: return@mapNotNull null
            MediaItem(
                id = rid,
                type = MediaType.ANIME,
                title = rtitle,
                poster = rec.getAsJsonObject("coverImage")?.get("large")?.asString,
                rating = rec.get("averageScore")?.takeIf { !it.isJsonNull }?.asInt?.let { it / 10.0 },
                addonId = "anilist",
                addonName = "AniList",
            )
        } ?: emptyList()

        val trailer = m.getAsJsonObject("trailer")?.let { t ->
            val site = t.get("site")?.asString
            val vid = t.get("id")?.asString
            if (site.equals("youtube", true) && !vid.isNullOrBlank()) "https://www.youtube.com/watch?v=$vid" else null
        }

        return MetaDetail(
            id = id,
            type = "anime",
            name = title,
            poster = m.getAsJsonObject("coverImage")?.get("extraLarge")?.asString,
            background = m.get("bannerImage")?.takeIf { !it.isJsonNull }?.asString,
            description = m.get("description")?.takeIf { !it.isJsonNull }?.asString?.replace(Regex("<[^>]*>"), ""),
            releaseInfo = m.getAsJsonObject("startDate")?.get("year")?.takeIf { !it.isJsonNull }?.asInt?.toString(),
            imdbRating = m.get("averageScore")?.takeIf { !it.isJsonNull }?.asInt?.let { String.format("%.1f", it / 10.0) },
            genres = m.getAsJsonArray("genres")?.map { it.asString } ?: emptyList(),
            cast = studios,
            runtime = m.get("episodes")?.takeIf { !it.isJsonNull }?.asInt?.let { "$it episodes" },
            addonId = "anilist",
            altTitles = altTitles,
            votes = m.get("favourites")?.takeIf { !it.isJsonNull }?.asInt,
            status = prettyStatus(m.get("status")?.asString),
            totalEpisodes = m.get("episodes")?.takeIf { !it.isJsonNull }?.asInt,
            trailerUrl = trailer,
            castMembers = castMembers,
            recommendations = recommendations,
        )
    }

    private fun prettyStatus(s: String?): String? = when (s) {
        "RELEASING" -> "Airing"
        "FINISHED" -> "Finished"
        "NOT_YET_RELEASED" -> "Upcoming"
        "CANCELLED" -> "Cancelled"
        "HIATUS" -> "On Hiatus"
        else -> s
    }
}
