package com.novastream.app.data.remote

import com.google.gson.JsonObject
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.MetaDetail
import com.novastream.app.data.model.Video

/** MangaDex API client for normal + NSFW manga. */
object MangaDexClient {
    private const val BASE = "https://api.mangadex.org"
    private const val UPLOADS = "https://uploads.mangadex.org"

    private val SAFE = listOf("safe", "suggestive")
    private val NSFW = listOf("erotica", "pornographic")

    private suspend fun get(path: String, params: List<Pair<String, String>>): JsonObject {
        val q = params.joinToString("&") { "${java.net.URLEncoder.encode(it.first, "UTF-8")}=${java.net.URLEncoder.encode(it.second, "UTF-8")}" }
        return parseJson(httpGetCached("$BASE$path?$q")).asJsonObject
    }

    private fun coverUrl(mangaId: String, o: JsonObject): String? {
        val rel = o.getAsJsonArray("relationships") ?: return null
        for (r in rel) {
            if (!r.isJsonObject) continue
            val ro = r.asJsonObject
            if (ro.get("type")?.asString == "cover_art") {
                val file = ro.getAsJsonObject("attributes")?.get("fileName")?.asString ?: return null
                return "$UPLOADS/covers/$mangaId/$file.512.jpg"
            }
        }
        return null
    }

    private fun item(o: JsonObject, nsfw: Boolean = false): MediaItem? {
        val id = o.get("id")?.asString ?: return null
        val attr = o.getAsJsonObject("attributes") ?: return null
        val title = attr.getAsJsonObject("title")?.let { t ->
            t.get("en")?.asString ?: t.entrySet().firstOrNull()?.value?.asString
        } ?: return null
        val desc = attr.getAsJsonObject("description")?.let { d ->
            d.get("en")?.asString ?: d.entrySet().firstOrNull()?.value?.asString
        }
        val year = attr.get("year")?.takeIf { it.isJsonPrimitive }?.asInt?.toString()
        return MediaItem(
            id = id,
            type = if (nsfw) MediaType.NSFW_MANGA else MediaType.MANGA,
            title = title,
            poster = coverUrl(id, o),
            year = year,
            description = desc,
            addonId = "mangadex",
            addonName = "MangaDex",
        )
    }

    private fun parseList(root: JsonObject, nsfw: Boolean = false): List<MediaItem> =
        root.getAsJsonArray("data")?.mapNotNull { if (it.isJsonObject) item(it.asJsonObject, nsfw) else null } ?: emptyList()

    suspend fun popular(limit: Int = 30): List<MediaItem> = parseList(
        get("/manga", listOf(
            "limit" to limit.toString(),
            "includes[]" to "cover_art",
            "order[followedCount]" to "desc",
            "contentRating[]" to "safe",
            "contentRating[]" to "suggestive",
        ))
    )

    suspend fun latest(limit: Int = 30): List<MediaItem> = parseList(
        get("/manga", listOf(
            "limit" to limit.toString(),
            "includes[]" to "cover_art",
            "order[latestUploadedChapter]" to "desc",
            "contentRating[]" to "safe",
        ))
    )

    suspend fun search(query: String, nsfw: Boolean = false, limit: Int = 30): List<MediaItem> {
        val ratings = if (nsfw) NSFW else SAFE
        val params = mutableListOf(
            "title" to query,
            "limit" to limit.toString(),
            "includes[]" to "cover_art",
        )
        ratings.forEach { params.add("contentRating[]" to it) }
        return parseList(get("/manga", params), nsfw)
    }

    suspend fun byGenre(tagId: String, nsfw: Boolean = false, limit: Int = 30): List<MediaItem> {
        val ratings = if (nsfw) NSFW else SAFE
        val params = mutableListOf(
            "limit" to limit.toString(),
            "includes[]" to "cover_art",
            "includedTags[]" to tagId,
        )
        ratings.forEach { params.add("contentRating[]" to it) }
        return parseList(get("/manga", params), nsfw)
    }

    // ---- NSFW catalogue (strictly erotica + pornographic content ratings) --------

    /** Generic NSFW listing with a MangaDex order key and optional tag filter. */
    private suspend fun nsfwList(order: String, tagId: String?, limit: Int): List<MediaItem> {
        val params = mutableListOf(
            "limit" to limit.toString(),
            "includes[]" to "cover_art",
            "order[$order]" to "desc",
        )
        NSFW.forEach { params.add("contentRating[]" to it) }
        if (!tagId.isNullOrBlank()) params.add("includedTags[]" to tagId)
        return parseList(get("/manga", params), nsfw = true)
    }

    suspend fun nsfwPopular(limit: Int = 30): List<MediaItem> = nsfwList("followedCount", null, limit)
    suspend fun nsfwLatest(limit: Int = 30): List<MediaItem> = nsfwList("latestUploadedChapter", null, limit)
    suspend fun nsfwTopRated(limit: Int = 30): List<MediaItem> = nsfwList("rating", null, limit)
    suspend fun nsfwByTag(tagId: String, limit: Int = 30): List<MediaItem> = nsfwList("followedCount", tagId, limit)

    suspend fun details(id: String): MetaDetail? {
        val root = parseJson(httpGetCached("$BASE/manga/$id?includes[]=cover_art&includes[]=author&includes[]=artist")).asJsonObject
        val data = root.getAsJsonObject("data") ?: return null
        val attr = data.getAsJsonObject("attributes") ?: return null
        val title = attr.getAsJsonObject("title")?.let { t -> t.get("en")?.asString ?: t.entrySet().firstOrNull()?.value?.asString } ?: return null
        val desc = attr.getAsJsonObject("description")?.let { d -> d.get("en")?.asString ?: d.entrySet().firstOrNull()?.value?.asString }

        val altTitles = attr.getAsJsonArray("altTitles")?.mapNotNull { t ->
            if (!t.isJsonObject) return@mapNotNull null
            val o = t.asJsonObject
            o.get("en")?.asString ?: o.entrySet().firstOrNull()?.value?.asString
        }?.filter { it.isNotBlank() && it != title }?.distinct()?.take(6) ?: emptyList()

        val authors = mutableListOf<String>()
        val castMembers = mutableListOf<com.novastream.app.data.model.CastMember>()
        data.getAsJsonArray("relationships")?.forEach { r ->
            if (!r.isJsonObject) return@forEach
            val ro = r.asJsonObject
            val t = ro.get("type")?.asString
            if (t == "author" || t == "artist") {
                ro.getAsJsonObject("attributes")?.get("name")?.asString?.let {
                    authors.add(it)
                    castMembers.add(
                        com.novastream.app.data.model.CastMember(
                            name = it,
                            character = if (t == "author") "Author" else "Artist",
                        )
                    )
                }
            }
        }

        val status = attr.get("status")?.asString?.let {
            when (it) {
                "ongoing" -> "Ongoing"
                "completed" -> "Completed"
                "hiatus" -> "On Hiatus"
                "cancelled" -> "Cancelled"
                else -> it.replaceFirstChar { c -> c.uppercase() }
            }
        }

        return MetaDetail(
            id = id,
            type = "manga",
            name = title,
            poster = coverUrl(id, data),
            description = desc,
            releaseInfo = attr.get("year")?.takeIf { it.isJsonPrimitive }?.asInt?.toString(),
            genres = attr.getAsJsonArray("tags")?.mapNotNull {
                it.takeIf { e -> e.isJsonObject }?.asJsonObject?.getAsJsonObject("attributes")?.getAsJsonObject("name")?.get("en")?.asString
            } ?: emptyList(),
            cast = authors,
            addonId = "mangadex",
            altTitles = altTitles,
            status = status,
            totalChapters = attr.get("lastChapter")?.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull(),
            castMembers = castMembers,
        )
    }

    /** Chapters feed. */
    suspend fun chapters(mangaId: String, nsfw: Boolean = false): List<Video> {
        val params = mutableListOf(
            "limit" to "100",
            "translatedLanguage[]" to "en",
            "order[chapter]" to "asc",
            "includes[]" to "scanlation_group",
        )
        val ratings = if (nsfw) NSFW else SAFE
        ratings.forEach { params.add("contentRating[]" to it) }
        val q = params.joinToString("&") { "${it.first}=${java.net.URLEncoder.encode(it.second, "UTF-8")}" }
        val root = parseJson(httpGet("$BASE/manga/$mangaId/feed?$q")).asJsonObject
        val arr = root.getAsJsonArray("data") ?: return emptyList()
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            val attr = o.getAsJsonObject("attributes") ?: return@mapNotNull null
            val chId = o.get("id")?.asString ?: return@mapNotNull null
            // Chapters hosted elsewhere (externalUrl, e.g. MangaPlus) have no at-home pages,
            // so skip them rather than opening the reader to a blank chapter.
            if (!attr.get("externalUrl")?.takeIf { !it.isJsonNull }?.asString.isNullOrBlank()) {
                return@mapNotNull null
            }
            val num = attr.get("chapter")?.takeIf { it.isJsonPrimitive }?.asString
            val title = attr.get("title")?.takeIf { !it.isJsonNull }?.asString
            Video(
                id = chId,
                title = (if (num != null) "Chapter $num" else "Chapter") + (if (!title.isNullOrBlank()) " – $title" else ""),
                episode = num?.toFloatOrNull()?.toInt(),
                released = attr.get("publishAt")?.asString,
            )
        }.distinctBy { it.id }
    }

    /** Page image URLs for a chapter via the at-home server. */
    suspend fun chapterPages(chapterId: String): List<String> {
        val root = parseJson(httpGet("$BASE/at-home/server/$chapterId")).asJsonObject
        val baseUrl = root.get("baseUrl")?.asString ?: return emptyList()
        val chapter = root.getAsJsonObject("chapter") ?: return emptyList()
        val hash = chapter.get("hash")?.asString ?: return emptyList()
        val data = chapter.getAsJsonArray("data") ?: return emptyList()
        return data.map { "$baseUrl/data/$hash/${it.asString}" }
    }
}
