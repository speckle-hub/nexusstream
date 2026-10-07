package com.novastream.app.data.local

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.WatchEntry
import java.util.Locale

/**
 * Element-wise tolerant codec for the two persisted library lists (favourites + history).
 *
 * Gson maps a missing JSON field onto a Kotlin non-null slot as a silent `null` (it does not
 * understand Kotlin nullability), so a favourite written by an older build — or written by a
 * source that omitted `type` — used to decode into a `MediaItem` whose `type` field was `null`
 * at runtime. Touching that field later (`item.type.id` in `Routes.detail`, `WatchStats.compute`,
 * the history mapping in `LibraryScreen`) threw an NPE and took the whole Library tab down.
 *
 * Worse, the old whole-list `fromJson` was an all-or-nothing operation: one malformed element
 * made the `catch` discard the user's entire library. This codec instead sanitizes **per
 * element** — bad entries are dropped, everything else survives — and defaults every optional
 * field so no decoded object can hold a null in a non-null slot.
 *
 * Pure JVM (no Android types) so it is directly unit-testable.
 */
object LibraryCodec {

    private val gson = Gson()

    fun encodeFavorites(items: List<MediaItem>): String = gson.toJson(items)

    fun encodeHistory(items: List<WatchEntry>): String = gson.toJson(items)

    /** Favourites, in stored order, minus malformed entries and duplicate keys. */
    fun decodeFavorites(json: String?): List<MediaItem> =
        elements(json).mapNotNull { media(it) }.distinctBy { it.key }

    /**
     * Continue-watching history. Storage order is newest-first ([LibraryStore.recordWatch]
     * prepends), so the first entry for a key wins on dedupe.
     */
    fun decodeHistory(json: String?): List<WatchEntry> =
        elements(json).mapNotNull { watch(it) }.distinctBy { it.item.key }

    // ---- elements ----------------------------------------------------------

    /**
     * Top-level array of objects. Also tolerates a single bare object and any parse failure
     * (truncated write, encoding corruption) by yielding an empty list instead of throwing.
     */
    private fun elements(json: String?): List<JsonObject> {
        if (json.isNullOrBlank()) return emptyList()
        val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return emptyList()
        return when {
            root.isJsonArray -> root.asJsonArray.mapNotNull { el ->
                el.takeIf { it.isJsonObject }?.asJsonObject
            }
            root.isJsonObject -> listOf(root.asJsonObject)
            else -> emptyList()
        }
    }

    /** A saved title, or null when the entry is unusable (no id) and should be dropped. */
    private fun media(o: JsonObject): MediaItem? {
        val id = str(o, "id") ?: return null
        return MediaItem(
            id = id,
            type = resolveType(o),
            title = str(o, "title") ?: str(o, "name") ?: "Untitled",
            poster = str(o, "poster"),
            backdrop = str(o, "backdrop"),
            year = str(o, "year"),
            rating = number(o, "rating"),
            description = str(o, "description"),
            genres = stringList(o, "genres"),
            addonId = str(o, "addonId"),
            catalogId = str(o, "catalogId"),
            addonName = str(o, "addonName"),
            stremioType = str(o, "stremioType"),
        )
    }

    /** A saved playback entry, or null when it has no (salvageable) title attached. */
    private fun watch(o: JsonObject): WatchEntry? {
        val itemEl = o.get("item")?.takeIf { it.isJsonObject } ?: return null
        val item = media(itemEl.asJsonObject) ?: return null
        return WatchEntry(
            item = item,
            videoId = str(o, "videoId"),
            videoTitle = str(o, "videoTitle"),
            positionMs = long(o, "positionMs", 0L),
            durationMs = long(o, "durationMs", 0L),
            updatedAt = long(o, "updatedAt", 0L).takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
    }

    // ---- field readers -----------------------------------------------------

    /**
     * Resolve the declared content type. Accepts both the Kotlin enum name (`"MOVIE"`) and the
     * catalog id (`"movie"`), falls back to the add-on's own `stremioType`, and finally to
     * [MediaType.MOVIE] — never `null`.
     */
    private fun resolveType(o: JsonObject): MediaType {
        str(o, "type")?.let { raw -> matchType(raw)?.let { return it } }
        str(o, "stremioType")?.let { raw -> matchType(raw)?.let { return it } }
        return MediaType.MOVIE
    }

    private fun matchType(raw: String): MediaType? =
        MediaType.entries.firstOrNull { it.name.equals(raw, true) || it.id.equals(raw, true) }
            ?: when (raw.lowercase(Locale.ROOT)) {
                "movie" -> MediaType.MOVIE
                "series", "tv", "show" -> MediaType.SERIES
                "anime" -> MediaType.ANIME
                "manga", "comic" -> MediaType.MANGA
                "hentai" -> MediaType.NSFW_ANIME
                "nsfw_manga" -> MediaType.NSFW_MANGA
                "real", "xxx", "porn", "adult" -> MediaType.REAL
                else -> null
            }

    /** Non-blank string from a string/number primitive; null for absent, JsonNull or objects. */
    private fun str(o: JsonObject, key: String): String? {
        val el = o.get(key)?.takeIf { it.isJsonPrimitive } ?: return null
        return runCatching { el.asString }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun number(o: JsonObject, key: String): Double? {
        val el = o.get(key)?.takeIf { it.isJsonPrimitive } ?: return null
        val value = runCatching { el.asString.toDoubleOrNull() }.getOrNull() ?: return null
        return value.takeIf { it.isFinite() }
    }

    private fun long(o: JsonObject, key: String, fallback: Long): Long {
        val el = o.get(key)?.takeIf { it.isJsonPrimitive } ?: return fallback
        val value = runCatching { el.asString.toLongOrNull() }.getOrNull() ?: return fallback
        return value.coerceAtLeast(0L)
    }

    /** String list from a JSON array (or a lone string); missing/null becomes an empty list. */
    private fun stringList(o: JsonObject, key: String): List<String> {
        val el = o.get(key) ?: return emptyList()
        return when {
            el.isJsonArray -> el.asJsonArray.mapNotNull { entry ->
                entry.takeIf { it.isJsonPrimitive }
                    ?.let { runCatching { it.asString }.getOrNull() }
                    ?.takeIf { it.isNotBlank() }
            }
            el.isJsonPrimitive -> runCatching { el.asString }.getOrNull()
                ?.takeIf { it.isNotBlank() }?.let { listOf(it) }
                ?: emptyList()
            else -> emptyList()
        }
    }
}
