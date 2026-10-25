package com.moovio.desktop.data.remote

import com.moovio.desktop.data.net.Http
import com.moovio.desktop.data.net.double
import com.moovio.desktop.data.net.int
import com.moovio.desktop.data.net.obj
import com.moovio.desktop.data.net.arrayOrNull
import com.moovio.desktop.data.net.parseJsonObject
import com.moovio.desktop.data.net.plainDescription
import com.moovio.desktop.data.net.str
import com.moovio.desktop.data.net.strings
import com.moovio.desktop.domain.model.Episode
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.MetaDetail
import java.net.URLEncoder
import kotlinx.serialization.json.JsonObject

/**
 * Cinemeta is the canonical Stremio metadata add-on and needs no API key. It is also the id
 * namespace (IMDb ids) that stream add-ons like Torrentio expect.
 */
class CinemetaClient(private val http: Http) {

    private val base = "https://v3-cinemeta.strem.io"
    private val catalogCache = TtlCache<String, List<MediaItem>>(ttlMs = 10 * 60_000)
    private val metaCache = TtlCache<String, MetaDetail>(ttlMs = 30 * 60_000)
    private val inflight = kotlinx.coroutines.sync.Mutex()

    suspend fun catalog(
        type: MediaType,
        catalogId: String = "top",
        genre: String? = null,
        skip: Int = 0,
    ): List<MediaItem> {
        val key = "${type.name}:$catalogId:$genre:$skip"
        catalogCache.get(key)?.let { return it }
        val path = buildString {
            append("/catalog/").append(type.stremioType).append('/').append(catalogId)
            val extras = buildList {
                if (skip > 0) add("skip=$skip")
                genre?.takeIf { it.isNotBlank() }?.let { add("genre=${encode(it)}") }
            }
            if (extras.isNotEmpty()) append('/').append(extras.joinToString("&"))
            append(".json")
        }
        val json = parseJsonObject(http.getText(base + path))
        val metas = json.arrayOrNull("metas")?.mapNotNull { it.obj() }?.mapNotNull { parseItem(it, type) }
            ?: emptyList()
        catalogCache.put(key, metas)
        return metas
    }

    suspend fun search(type: MediaType, query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        val url = "$base/catalog/${type.stremioType}/top/search=${encode(query)}.json"
        val json = runCatching { parseJsonObject(http.getText(url)) }.getOrNull() ?: return emptyList()
        return json.arrayOrNull("metas")
            ?.mapNotNull { it.obj() }
            ?.mapNotNull { parseItem(it, type) }
            ?: emptyList()
    }

    suspend fun meta(type: MediaType, id: String): MetaDetail? {
        val key = "${type.name}:$id"
        metaCache.get(key)?.let { return it }
        val url = "$base/meta/${type.stremioType}/${encode(id)}.json"
        val json = runCatching { parseJsonObject(http.getText(url)) }.getOrNull() ?: return null
        val meta = json["meta"].obj() ?: return null
        val detail = parseDetail(meta, type)
        metaCache.put(key, detail)
        return detail
    }

    private fun parseItem(json: JsonObject, type: MediaType): MediaItem? {
        val id = json.str("id") ?: return null
        val name = json.str("name") ?: return null
        return MediaItem(
            id = id,
            type = type,
            name = name,
            poster = json.str("poster"),
            background = json.str("background"),
            logo = json.str("logo"),
            year = yearOf(json.str("releaseInfo")),
            rating = json.double("imdbRating")?.coerceIn(0.0, 10.0),
            description = json.str("description").plainDescription(),
            genres = json.arrayOrNull("genres").strings(),
            releaseInfo = json.str("releaseInfo"),
            runtime = json.str("runtime"),
            addonId = CINEMETA_ID,
        )
    }

    private fun parseDetail(meta: JsonObject, type: MediaType): MetaDetail {
        val videos = meta.arrayOrNull("videos")
            ?.mapNotNull { it.obj() }
            ?.mapNotNull { video ->
                val id = video.str("id") ?: return@mapNotNull null
                val season = video.int("season") ?: 1
                val number = video.int("number") ?: video.int("episode") ?: return@mapNotNull null
                Episode(
                    id = id,
                    season = season,
                    episode = number,
                    title = video.str("name") ?: video.str("title") ?: "Episode $number",
                    thumbnail = video.str("thumbnail"),
                    released = video.str("released"),
                    description = video.str("overview").plainDescription(),
                )
            }
            ?.sortedWith(compareBy({ it.season }, { it.episode }))
            ?: emptyList()

        return MetaDetail(
            id = meta.str("id") ?: error("meta without id"),
            type = type,
            name = meta.str("name") ?: "Unknown",
            poster = meta.str("poster"),
            background = meta.str("background"),
            logo = meta.str("logo"),
            description = meta.str("description").plainDescription(),
            year = yearOf(meta.str("releaseInfo")),
            releaseInfo = meta.str("releaseInfo"),
            rating = meta.double("imdbRating")?.coerceIn(0.0, 10.0),
            runtime = meta.str("runtime"),
            genres = meta.arrayOrNull("genres").strings(),
            cast = meta.arrayOrNull("cast").strings(),
            episodes = videos,
        )
    }

    private fun encode(raw: String): String =
        URLEncoder.encode(raw, "UTF-8").replace("+", "%20")

    companion object {
        const val CINEMETA_ID = "org.cinemeta"
        const val MANIFEST_URL = "https://v3-cinemeta.strem.io/manifest.json"
    }
}

fun yearOf(releaseInfo: String?): Int? =
    Regex("""\b(19|20)\d{2}\b""").find(releaseInfo.orEmpty())?.value?.toIntOrNull()

/** Tiny single-value TTL cache (thread-safe, bounded). */
class TtlCache<K : Any, V : Any>(private val ttlMs: Long, private val maxSize: Int = 128) {
    private data class Entry<V>(val value: V, val expiresAt: Long)
    private val map = object : LinkedHashMap<K, Entry<V>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Entry<V>>?): Boolean =
            size > maxSize
    }

    @Synchronized
    fun get(key: K): V? {
        val entry = map[key] ?: return null
        if (entry.expiresAt < System.currentTimeMillis()) {
            map.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = Entry(value, System.currentTimeMillis() + ttlMs)
    }

    @Synchronized
    fun clear() = map.clear()
}
