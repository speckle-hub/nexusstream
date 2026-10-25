package com.moovio.desktop.data.remote

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import com.moovio.desktop.data.local.SettingsStore
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Thin Stremio add-on protocol client.
 *
 * Every call is bounded by a hard timeout and wrapped in `runCatching` so a single dead or slow
 * add-on can never break the whole app — failures degrade gracefully to empty results.
 */
class StremioClient(
    private val settings: SettingsStore,
    private val connectTimeoutMs: Long = 8_000,
    private val readTimeoutMs: Long = 12_000,
) {
    private val gson = Gson()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE))
        .build()

    private suspend fun <T> get(path: String, parser: com.squareup.okhttp3.ResponseBody.() -> T): T {
        val request = okhttp3.Request.Builder()
            .url(path)
            .header("User-Agent", "Moovio/0.1.0 (desktop)")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code}")
            parser(response.body!!)
        }
    }

    suspend fun fetchManifest(manifestUrl: String) =
        runCatching {
            get("${manifestUrl.trimEnd('/')}/manifest.json") {
                json.decodeFromString<StremioManifest>(charStream().readText())
            }
        }.getOrDefault(StremioManifest(
            id = "unknown",
            version = "0",
            name = manifestUrl,
            types = emptyList(),
            catalogs = emptyList(),
            resources = emptyList(),
        ))

    suspend fun fetchCatalog(manifestUrl: String, catalogId: String, type: String, extra: Map<String, String> = emptyMap()) =
        runCatching {
            val url = buildUrl(manifestUrl, "catalog/$type/$catalogId", extra)
            get(url) {
                json.decodeFromString<StremioCatalogResponse>(charStream().readText())
            }
        }.getOrDefault(StremioCatalogResponse())

    suspend fun fetchMeta(manifestUrl: String, type: String, id: String) =
        runCatching {
            get("${manifestUrl.trimEnd('/')}/meta/$type/$id") {
                json.decodeFromString<StremioMetaResponse>(charStream().readText())
            }
        }.getOrDefault(StremioMetaResponse(null))

    suspend fun fetchStreams(manifestUrl: String, type: String, id: String) =
        runCatching {
            get("${manifestUrl.trimEnd('/')}/stream/$type/$id") {
                json.decodeFromString<StremioStreamsResponse>(charStream().readText())
            }
        }.getOrDefault(StremioStreamsResponse())

    private fun buildUrl(base: String, path: String, extra: Map<String, String>): String {
        val encoded = extra.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "${base.trimEnd('/')}/$path${if (encoded.isNotEmpty()) "?$encoded" else ""}"
    }

    /** Converts catalog entries from a single add-on into app media items. */
    fun StremioCatalogMeta.toMediaItem(sourceName: String): MediaItem {
        val type = when (type?.lowercase()) {
            "series", "tv" -> MediaType.TV
            "anime" -> MediaType.ANIME
            "manga" -> MediaType.MANGA
            else -> MediaType.MOVIE
        }
        return MediaItem(
            id = id,
            type = type,
            title = name ?: id,
            poster = poster,
            backdrop = background,
            genre = genre,
            rating = rating,
            source = sourceName,
            year = released?.take(4).toIntOrNull(),
            synopsis = null,
        )
    }
}
