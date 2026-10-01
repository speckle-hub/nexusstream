package com.novastream.app.data.remote

import com.google.gson.JsonObject
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType

/** Jikan (MyAnimeList) client — fallback NSFW anime metadata source. */
object JikanClient {
    private const val BASE = "https://api.jikan.moe/v4"

    private fun item(o: JsonObject): MediaItem? {
        val id = o.get("mal_id")?.asInt?.toString() ?: return null
        val title = o.get("title_english")?.takeIf { !it.isJsonNull }?.asString
            ?: o.get("title")?.asString ?: return null
        val images = o.getAsJsonObject("images")?.getAsJsonObject("jpg")
        return MediaItem(
            id = id,
            type = MediaType.NSFW_ANIME,
            title = title,
            poster = images?.get("large_image_url")?.asString ?: images?.get("image_url")?.asString,
            year = o.getAsJsonObject("aired")?.getAsJsonObject("prop")?.getAsJsonObject("from")?.get("year")?.takeIf { !it.isJsonNull }?.asInt?.toString(),
            rating = o.get("score")?.takeIf { !it.isJsonNull }?.asDouble,
            description = o.get("synopsis")?.takeIf { !it.isJsonNull }?.asString,
            addonId = "jikan",
            addonName = "Jikan (MAL)",
        )
    }

    private suspend fun listRaw(path: String): List<JsonObject> {
        val root = parseJson(httpGet("$BASE$path")).asJsonObject
        val arr = root.getAsJsonArray("data") ?: return emptyList()
        return arr.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
    }

    private suspend fun list(path: String): List<MediaItem> = listRaw(path).mapNotNull { item(it) }

    /** Adult entries by MAL rating: "Rx - Hentai" or "R+ - Mild Nudity". */
    private fun isAdult(o: JsonObject): Boolean {
        val r = o.get("rating")?.asString?.lowercase() ?: return false
        return r.startsWith("rx") || r.startsWith("r+")
    }

    private suspend fun listAdult(path: String): List<MediaItem> =
        listRaw(path).filter { isAdult(it) }.mapNotNull { item(it) }

    /**
     * Jikan intermittently returns 504 when MyAnimeList is unreachable — retry once.
     */
    private suspend fun withRetry(block: suspend () -> List<MediaItem>): List<MediaItem> {
        val first = runCatching { block() }.getOrDefault(emptyList())
        if (first.isNotEmpty()) return first
        kotlinx.coroutines.delay(700)
        return runCatching { block() }.getOrDefault(emptyList())
    }

    suspend fun nsfwSearch(query: String): List<MediaItem> {
        val enc = java.net.URLEncoder.encode(query, "UTF-8")
        val strict = withRetry { list("/anime?q=$enc&rating=rx&sfw=false&limit=25") }
        if (strict.isNotEmpty()) return strict
        // Fallback when the filtered endpoint is down: search normally, keep adult-rated entries.
        return withRetry { listAdult("/anime?q=$enc&limit=25") }
    }

    suspend fun nsfwTop(): List<MediaItem> {
        val strict = withRetry { list("/top/anime?filter=bypopularity&rating=rx&sfw=false&limit=25") }
        if (strict.isNotEmpty()) return strict
        // Fallback: page through the general top list and filter adult entries client-side.
        return withRetry {
            listAdult("/top/anime?page=1&limit=25") + listAdult("/top/anime?page=2&limit=25")
        }
    }
}
