package com.moovio.app.data.remote

import com.google.gson.JsonObject
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.MetaDetail
import com.moovio.app.data.model.Video

/**
 * True when [ua] impersonates a browser.
 *
 * MangaDex now fronts `api.mangadex.org` and `uploads.mangadex.org` with
 * [POWBlock](https://github.com/8Protons/POWBlock), a JavaScript proof-of-work check, and it picks
 * its targets **by User-Agent**: a browser-shaped UA gets `HTTP 200` with an HTML challenge page
 * instead of JSON, while a non-browser UA gets the normal API answer. Measured 2026-11-01, five
 * requests each:
 *
 *  - `Mozilla/5.0 … Chrome/120.0 Mobile Safari/537.36 Moovio/1.0` → challenge page, 5/5
 *  - `Mozilla/5.0 (Linux; Android 10; K) … Chrome/120.0.0.0 …` → challenge page, 5/5
 *  - `okhttp/4.12.0`, `curl/8.4.0`, `MangaDex-Client/1.0` → JSON, 5/5
 *
 * Both hosts matter: the challenge is also served for cover images under `/covers/`. This is why
 * the Manga tab reported "No manga found. Check your connection" with a perfectly good connection
 * — the body parsed as HTML, every row fetcher swallowed the exception, and the section came back
 * empty.
 *
 * Pure and Android-free so the rule is unit-tested (`MangaDexIdentityTest`).
 */
internal fun isBrowserLikeUserAgent(ua: String): Boolean =
    ua.contains("Mozilla/", ignoreCase = true) || ua.contains("AppleWebKit", ignoreCase = true)

/**
 * The User-Agent to send to MangaDex: the user's configured identity when it is *not* a browser
 * impersonation, otherwise [MangaDexClient.USER_AGENT].
 *
 * Settings → Network lets the user spoof a browser UA for hosts that demand one; MangaDex is the
 * exact opposite, so that override must not follow them here.
 */
internal fun mangaDexUserAgent(configured: String): String =
    if (configured.isBlank() || isBrowserLikeUserAgent(configured)) MangaDexClient.USER_AGENT else configured

/** MangaDex API client for normal + NSFW manga. */
object MangaDexClient {
    private const val BASE = "https://api.mangadex.org"
    private const val UPLOADS = "https://uploads.mangadex.org"

    /**
     * Identity every MangaDex request carries. Deliberately **not** browser-shaped: MangaDex's
     * bot check answers a browser UA with an HTML challenge page (see [isBrowserLikeUserAgent]),
     * so a descriptive client UA is both what its API guidelines ask for and the only thing that
     * reaches the JSON endpoints.
     */
    const val USER_AGENT = "Moovio/1.0 (Android; +https://github.com/speckle-hub/nexusstream)"

    /** Per-request headers; applied after the global identity so this UA always wins. */
    private fun headers(): Map<String, String> =
        mapOf("User-Agent" to mangaDexUserAgent(Http.customUserAgent))

    private suspend fun get(path: String, params: List<Pair<String, String>>): JsonObject {
        val q = params.joinToString("&") { "${java.net.URLEncoder.encode(it.first, "UTF-8")}=${java.net.URLEncoder.encode(it.second, "UTF-8")}" }
        return parseJson(httpGetCached("$BASE$path?$q", headers())).asJsonObject
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
        val ratings = MangaDexQuery.ratings(nsfw)
        val params = mutableListOf(
            "title" to query,
            "limit" to limit.toString(),
            "includes[]" to "cover_art",
        )
        ratings.forEach { params.add("contentRating[]" to it) }
        return parseList(get("/manga", params), nsfw)
    }

    suspend fun byGenre(tagId: String, nsfw: Boolean = false, limit: Int = 30): List<MediaItem> {
        val ratings = MangaDexQuery.ratings(nsfw)
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
        MangaDexQuery.ratingParams(MangaDexQuery.NSFW).forEach { params.add(it) }
        if (!tagId.isNullOrBlank()) params.add("includedTags[]" to tagId)
        return parseList(get("/manga", params), nsfw = true)
    }

    suspend fun nsfwPopular(limit: Int = 30): List<MediaItem> = nsfwList("followedCount", null, limit)
    suspend fun nsfwLatest(limit: Int = 30): List<MediaItem> = nsfwList("latestUploadedChapter", null, limit)
    suspend fun nsfwTopRated(limit: Int = 30): List<MediaItem> = nsfwList("rating", null, limit)
    suspend fun nsfwByTag(tagId: String, limit: Int = 30): List<MediaItem> = nsfwList("followedCount", tagId, limit)

    suspend fun details(id: String): MetaDetail? {
        val root = parseJson(httpGetCached("$BASE/manga/$id?includes[]=cover_art&includes[]=author&includes[]=artist", headers())).asJsonObject
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
        val castMembers = mutableListOf<com.moovio.app.data.model.CastMember>()
        data.getAsJsonArray("relationships")?.forEach { r ->
            if (!r.isJsonObject) return@forEach
            val ro = r.asJsonObject
            val t = ro.get("type")?.asString
            if (t == "author" || t == "artist") {
                ro.getAsJsonObject("attributes")?.get("name")?.asString?.let {
                    authors.add(it)
                    castMembers.add(
                        com.moovio.app.data.model.CastMember(
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

    /**
     * Chapters feed.
     *
     * MangaDex reports "no chapters match this [contentRating] filter" as **HTTP 200 with an
     * empty `data` array** and defaults an unfiltered feed to the SFW ratings, so a mis-set
     * filter surfaces as a permanently empty chapter list on NSFW manga. The rating sets come
     * from [MangaDexQuery.feedAttempts]: the NSFW set (never `safe`) first, and for a SFW title
     * that returns nothing, one widening retry with the same NSFW set.
     */
    suspend fun chapters(mangaId: String, nsfw: Boolean = false): List<Video> {
        val base = mutableListOf(
            "limit" to "100",
            "translatedLanguage[]" to "en",
            "order[chapter]" to "asc",
            "includes[]" to "scanlation_group",
        )
        for (ratings in MangaDexQuery.feedAttempts(nsfw)) {
            val chapters = feedChapters(mangaId, base + MangaDexQuery.ratingParams(ratings))
            if (chapters.isNotEmpty()) return chapters
        }
        return emptyList()
    }

    /** One `/manga/{id}/feed` request. */
    private suspend fun feedChapters(mangaId: String, params: List<Pair<String, String>>): List<Video> {
        val q = params.joinToString("&") { "${it.first}=${java.net.URLEncoder.encode(it.second, "UTF-8")}" }
        val root = parseJson(httpGet("$BASE/manga/$mangaId/feed?$q", headers())).asJsonObject
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
        val root = parseJson(httpGet("$BASE/at-home/server/$chapterId", headers())).asJsonObject
        val baseUrl = root.get("baseUrl")?.asString ?: return emptyList()
        val chapter = root.getAsJsonObject("chapter") ?: return emptyList()
        val hash = chapter.get("hash")?.asString ?: return emptyList()
        val data = chapter.getAsJsonArray("data") ?: return emptyList()
        return data.map { "$baseUrl/data/$hash/${it.asString}" }
    }
}
