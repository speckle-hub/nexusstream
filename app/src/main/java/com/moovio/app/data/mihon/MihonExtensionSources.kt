package com.moovio.app.data.mihon

import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.model.Video
import kotlinx.coroutines.withTimeout

/**
 * Adapts a loaded tachiyomi / Aniyomi / Mihon extension source to the app's own model types.
 *
 * All tachiyomi API calls and data-object field reads go through [MihonExtensionRuntime]'s
 * reflection helpers, so this module never references `eu.kanade.tachiyomi.*` at compile time.
 * Every call is wrapped in `runCatching` — a source that throws or returns unexpected shapes
 * simply yields no results, never a crash.
 *
 * Source → MediaType mapping (driven by the `MangaExt.nsfw` flag carried by the caller):
 *   Aniyomi (anime)   + nsfw  → NSFW_ANIME   | not nsfw → ANIME
 *   Mihon / Keiyoushi (manga) + nsfw  → NSFW_MANGA | not nsfw → MANGA
 */
object MihonExtensionSources {

    const val ADDON_PREFIX = MihonExtensionRuntime.ADDON_PREFIX

    /** Default page size used when the source API takes a page parameter. */
    private const val DEFAULT_PAGE = 1

    /** Per-call timeout so one hung extension can't stall search or detail. */
    private const val SOURCE_TIMEOUT_MS = 10_000L

    /** MediaType for a source instance, based on the repo's NSFW flag and the source kind. */
    fun mediaType(source: Any, isNsfw: Boolean): MediaType {
        val isAnime = isAnimeSource(source)
        return when {
            isAnime && isNsfw -> MediaType.NSFW_ANIME
            isAnime -> MediaType.ANIME
            isNsfw -> MediaType.NSFW_MANGA
            else -> MediaType.MANGA
        }
    }

    /** Heuristic: source class name or package suggests anime rather than manga. */
    private fun isAnimeSource(source: Any): Boolean {
        val name = source.javaClass.name.lowercase()
        return name.contains("anime") || name.contains("ani")
    }

    // ---- Browse / search ----------------------------------------------------------

    /**
     * Latest / trending titles from a source. Tries `getLatestManga(int)` then
     * `getLatestManga()` then `getLatestAnime(int)` then `getLatestAnime()`.
     */
    suspend fun latest(source: Any, page: Int = DEFAULT_PAGE, timeoutMs: Long = SOURCE_TIMEOUT_MS): List<MediaItem> =
        withTimeout(timeoutMs) {
            runCatching {
                MihonExtensionRuntime.reflectInvokeInt(source, "getLatestManga", page)
                    ?: MihonExtensionRuntime.reflectInvoke(source, "getLatestManga")
                    ?: MihonExtensionRuntime.reflectInvokeInt(source, "getLatestAnime", page)
                    ?: MihonExtensionRuntime.reflectInvoke(source, "getLatestAnime")
            }.getOrNull()?.let { asList(it) as List<Any?> } ?: emptyList()
        }.mapNotNull { mangaToItem(it, source) }

    /**
     * Popular titles from a source. Tries `getPopularManga(int)` then `getPopularManga()`
     * then `getPopularAnime(int)` then `getPopularAnime()`.
     */
    suspend fun popular(source: Any, page: Int = DEFAULT_PAGE, timeoutMs: Long = SOURCE_TIMEOUT_MS): List<MediaItem> =
        withTimeout(timeoutMs) {
            runCatching {
                MihonExtensionRuntime.reflectInvokeInt(source, "getPopularManga", page)
                    ?: MihonExtensionRuntime.reflectInvoke(source, "getPopularManga")
                    ?: MihonExtensionRuntime.reflectInvokeInt(source, "getPopularAnime", page)
                    ?: MihonExtensionRuntime.reflectInvoke(source, "getPopularAnime")
            }.getOrNull()?.let { asList(it) as List<Any?> } ?: emptyList()
        }.mapNotNull { mangaToItem(it, source) }

    /**
     * Search a source for [query]. Tries `searchManga(String, int)` then
     * `searchManga(String)` then `searchAnime(String, int)` then `searchAnime(String)`.
     */
    suspend fun search(
        source: Any,
        query: String,
        page: Int = DEFAULT_PAGE,
        timeoutMs: Long = SOURCE_TIMEOUT_MS,
    ): List<MediaItem> =
        withTimeout(timeoutMs) {
            runCatching {
                MihonExtensionRuntime.reflectInvokeStringInt(source, "searchManga", query, page)
                    ?: MihonExtensionRuntime.reflectInvokeString(source, "searchManga", query)
                    ?: MihonExtensionRuntime.reflectInvokeStringInt(source, "searchAnime", query, page)
                    ?: MihonExtensionRuntime.reflectInvokeString(source, "searchAnime", query)
            }.getOrNull()?.let { asList(it) as List<Any?> } ?: emptyList()
        }.mapNotNull { mangaToItem(it, source) }

    // ---- Detail -------------------------------------------------------------------

    /** Full detail for a title. Tries `getMangaDetails(String)` then `getAnimeDetails(String)`. */
    suspend fun details(
        source: Any,
        titleUrl: String,
        timeoutMs: Long = SOURCE_TIMEOUT_MS,
    ): Map<String, Any?>? =
        withTimeout(timeoutMs) {
            runCatching {
                MihonExtensionRuntime.reflectInvokeString(source, "getMangaDetails", titleUrl)
                    ?: MihonExtensionRuntime.reflectInvokeString(source, "getAnimeDetails", titleUrl)
            }.getOrNull()?.let { mangaToMap(it) }
        }

    // ---- Chapters / episodes ------------------------------------------------------

    /** Chapter / episode list for a title URL. Tries `getChapterList(String)` then `getEpisodeList(String)`. */
    suspend fun chapters(
        source: Any,
        titleUrl: String,
        timeoutMs: Long = SOURCE_TIMEOUT_MS,
    ): List<Map<String, Any?>> =
        withTimeout(timeoutMs) {
            runCatching {
                MihonExtensionRuntime.reflectInvokeString(source, "getChapterList", titleUrl)
                    ?: MihonExtensionRuntime.reflectInvokeString(source, "getEpisodeList", titleUrl)
            }.getOrNull()?.let { asList(it) as List<Any?> } ?: emptyList()
        }.mapNotNull { chapterToMap(it) }

    /** Page list for a chapter/episode URL. Tries `getPageList(String)` then `getVideoList(String)`. */
    suspend fun pages(
        source: Any,
        chapterUrl: String,
        timeoutMs: Long = SOURCE_TIMEOUT_MS,
    ): List<Map<String, Any?>> =
        withTimeout(timeoutMs) {
            runCatching {
                MihonExtensionRuntime.reflectInvokeString(source, "getPageList", chapterUrl)
                    ?: MihonExtensionRuntime.reflectInvokeString(source, "getVideoList", chapterUrl)
            }.getOrNull()?.let { asList(it) as List<Any?> } ?: emptyList()
        }.mapNotNull { pageToMap(it) }

    // ---- Adaptation ---------------------------------------------------------------

    /** A tachiyomi `Manga` / `Anime` object → [MediaItem]. */
    private fun mangaToItem(obj: Any?, source: Any): MediaItem? {
        val map = mangaToMap(obj) ?: return null
        val title = map["title"] as? String ?: return null
        val url = map["url"] as? String ?: ""
        return MediaItem(
            id = url,
            type = mediaType(source, false), // caller overrides with the repo's NSFW flag
            title = title,
            poster = map["thumbnail_url"] as? String,
            year = map["updated_at"] as? String,
            rating = (map["rating"] as? Number)?.toDouble(),
            description = map["description"] as? String,
            addonId = MihonExtensionRuntime.addonId(source),
            addonName = sourceName(source),
        )
    }

    /** A tachiyomi `Manga` / `Anime` object → a flat map of public fields. */
    private fun mangaToMap(obj: Any?): Map<String, Any?>? {
        if (obj == null) return null
        val fields = listOf("url", "title", "thumbnail_url", "genre", "status", "views",
            "followers", "rating", "description", "updated_at", "manga_url")
        val map = mutableMapOf<String, Any?>()
        fields.forEach { field ->
            val value = MihonExtensionRuntime.reflectGetField(obj, field)
            if (value != null) map[field] = value
        }
        return map.takeIf { it.isNotEmpty() }
    }

    /** A tachiyomi `Chapter` / `Episode` object → a flat map of public fields. */
    private fun chapterToMap(obj: Any?): Map<String, Any?>? {
        if (obj == null) return null
        val fields = listOf("url", "chapter_title", "chapter_number", "date_upload",
            "manga_url", "scanlator", "group_name", "lang", "page_count",
            "episode_title", "episode_number", "anime_url")
        val map = mutableMapOf<String, Any?>()
        fields.forEach { field ->
            val value = MihonExtensionRuntime.reflectGetField(obj, field)
            if (value != null) map[field] = value
        }
        return map.takeIf { it.isNotEmpty() }
    }

    /** A tachiyomi `Page` / `Video` object → a flat map of public fields. */
    private fun pageToMap(obj: Any?): Map<String, Any?>? {
        if (obj == null) return null
        val fields = listOf("page_url", "page_url_orig", "page_url_img", "page_url_viewer",
            "url", "quality", "video_url")
        val map = mutableMapOf<String, Any?>()
        fields.forEach { field ->
            val value = MihonExtensionRuntime.reflectGetField(obj, field)
            if (value != null) map[field] = value
        }
        return map.takeIf { it.isNotEmpty() }
    }

    /** A chapter map → [Video]. */
    fun chapterToVideo(chapter: Map<String, Any?>): Video {
        val url = chapter["url"] as? String ?: ""
        val title = chapter["chapter_title"] as? String
            ?: chapter["episode_title"] as? String
            ?: "Chapter"
        val number = (chapter["chapter_number"] as? Number)
            ?: (chapter["episode_number"] as? Number)?.toInt()
        return Video(
            id = url,
            title = title,
            episode = number?.toInt(),
            thumbnail = null,
        )
    }

    /** A page map → [StreamSource]. The page URL is the direct media URL. */
    fun pageToStreamSource(page: Map<String, Any?>, source: Any): StreamSource {
        val url = page["page_url"] as? String
            ?: page["page_url_orig"] as? String
            ?: page["page_url_img"] as? String
            ?: page["page_url_viewer"] as? String
            ?: page["url"] as? String
            ?: ""
        val quality = page["quality"] as? String
        return StreamSource(
            url = url.takeIf { it.isNotEmpty() },
            title = quality?.let { "$quality" } ?: null,
            name = sourceName(source),
            description = "mihon",
            quality = quality,
            addonId = MihonExtensionRuntime.addonId(source),
            addonName = sourceName(source),
        )
    }

    // ---- Helpers ------------------------------------------------------------------

    /** A source instance's human-readable name. */
    fun sourceName(source: Any): String =
        MihonExtensionRuntime.reflectString(source, "name")
            ?: MihonExtensionRuntime.reflectInvoke(source, "getName") as? String
            ?: source.javaClass.simpleName

    /** `Any?` as a `List<*>` — tachiyomi `List` subtypes come through as `java.util.List`. */
    private fun asList(obj: Any?): List<*> =
        (obj as? List<*>) ?: emptyList<Any>()

    /** A source's base URL (used for resolving relative chapter URLs). */
    fun baseUrl(source: Any): String? =
        MihonExtensionRuntime.reflectString(source, "baseUrl")
            ?: MihonExtensionRuntime.reflectInvoke(source, "getBaseUrl") as? String
}
