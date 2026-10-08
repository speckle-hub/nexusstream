package com.novastream.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.novastream.app.data.local.MetadataCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Retries idempotent (GET) requests that hit a rate limit, honouring `Retry-After` when present.
 * MangaDex (especially the at-home image server) is aggressive with 429s, so this keeps chapter
 * and catalogue loads reliable without a separate throttle on every call site.
 */
private object RetryOn429Interceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)
        var response = chain.proceed(request)
        var attempt = 0
        while (response.code == 429 && attempt < 3) {
            val retryAfterMs = response.header("Retry-After")?.trim()?.toLongOrNull()?.times(1000)
            response.close()
            val waitMs = (retryAfterMs ?: (500L shl attempt)).coerceIn(250L, 5000L)
            Thread.sleep(waitMs)
            attempt++
            response = chain.proceed(request)
        }
        return response
    }
}

/** Shared HTTP layer used by every remote client. */
object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .addInterceptor(RetryOn429Interceptor)
        .build()

    val gson: Gson = Gson()

    /**
     * On-disk metadata cache, wired up from the Application. When null (e.g. unit tests) the
     * cached helpers below simply fall through to the network.
     */
    @Volatile
    var cache: MetadataCache? = null

    /**
     * When false the "Cache metadata" setting is off and the cached helpers skip both the read and
     * the write, so every request goes straight to the network. Kept in sync from the app scope
     * (see `NovaApp`).
     */
    @Volatile
    var cacheEnabled: Boolean = true

    /**
     * Global custom HTTP identity (Phase 12), configured under Settings → Network. When set these
     * override the app's defaults on *every* outbound request, so a user can spoof a source's
     * expected User-Agent / Referer / Cookie without per-add-on code. Explicit per-request headers
     * (e.g. an adult source's own Referer) still win because they are applied afterwards.
     */
    @Volatile
    var customUserAgent: String = ""

    @Volatile
    var customReferer: String = ""

    @Volatile
    var customCookie: String = ""
}

/** Applies the optional global User-Agent / Referer / Cookie overrides to a request builder. */
private fun Request.Builder.globalIdentity(): Request.Builder {
    header("User-Agent", Http.customUserAgent.ifBlank { UA })
    if (Http.customReferer.isNotBlank()) header("Referer", Http.customReferer)
    if (Http.customCookie.isNotBlank()) header("Cookie", Http.customCookie)
    return this
}

/** Freshness window for cached metadata responses (TMDB / AniList / MangaDex). */
const val METADATA_TTL_MS = 6L * 60 * 60 * 1000

/**
 * GET with the shared on-disk cache in front of the network: a fresh cached body is returned
 * without any request, otherwise the response is stored (6 h by default).
 */
suspend fun httpGetCached(
    url: String,
    headers: Map<String, String> = emptyMap(),
    maxAgeMs: Long = METADATA_TTL_MS,
): String {
    val cache = Http.cache?.takeIf { Http.cacheEnabled }
    cache?.get(url, maxAgeMs)?.let { return it }
    val body = httpGet(url, headers)
    cache?.put(url, body)
    return body
}

/** POST variant of [httpGetCached], keyed on the URL + request body (used by AniList GraphQL). */
suspend fun httpPostJsonCached(
    url: String,
    body: String,
    maxAgeMs: Long = METADATA_TTL_MS,
): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(body.toByteArray(Charsets.UTF_8))
        .take(16)
        .joinToString("") { "%02x".format(it) }
    val key = "POST:$url:$digest"
    val cache = Http.cache?.takeIf { Http.cacheEnabled }
    cache?.get(key, maxAgeMs)?.let { return it }
    val resp = httpPostJson(url, body)
    cache?.put(key, resp)
    return resp
}

/** Default browser-like user agent so addons / APIs behave. */
const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 NexusStream/1.0"

/** Attempts for idempotent GETs, on top of OkHttp's own connection retry. */
private const val GET_ATTEMPTS = 3

/** Base pause between those attempts; grows linearly (250 ms, 500 ms). */
private const val GET_RETRY_BACKOFF_MS = 250L

/**
 * True when re-sending the exact same request could plausibly succeed: a transport failure
 * (timeout, reset, DNS hiccup) or a 5xx / 408 / 429 response. Other 4xx answers are final —
 * retrying them would only add latency to a request that can never succeed.
 */
private fun isTransientFailure(t: Throwable): Boolean {
    if (t !is java.io.IOException) return false
    val msg = t.message ?: return true
    if (!msg.startsWith("HTTP ")) return true
    val code = msg.removePrefix("HTTP ").substringBefore(' ').trim().toIntOrNull() ?: return false
    return code >= 500 || code == 408 || code == 429
}

/**
 * GET with a bounded retry for transient failures.
 *
 * Cold-boot metadata (TMDB / AniList / MangaDex posters + descriptions) used to make exactly one
 * attempt, so a single flaky handshake on launch left rows and detail pages without artwork or a
 * synopsis for the next 6 hours (the result of the failed call is not cached, but the row was
 * painted empty). Two extra attempts with a short backoff turn that into a momentary blip.
 */
suspend fun httpGet(
    url: String,
    headers: Map<String, String> = emptyMap(),
): String {
    var attempt = 0
    while (true) {
        try {
            return withContext(Dispatchers.IO) { singleHttpGet(url, headers) }
        } catch (e: CancellationException) {
            // Never retry (or swallow) a cancelled caller — see runCatchingCancellable.
            throw e
        } catch (e: Exception) {
            attempt++
            if (attempt >= GET_ATTEMPTS || !isTransientFailure(e)) throw e
            delay(GET_RETRY_BACKOFF_MS * attempt)
        }
    }
}

private suspend fun singleHttpGet(url: String, headers: Map<String, String>): String =
    withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url).get()
            .globalIdentity()
            .header("Accept", "application/json, text/plain, */*")
        headers.forEach { (k, v) -> builder.header(k, v) }
        Http.client.newCall(builder.build()).execute().use { resp: Response ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code} for $url")
            resp.body?.string() ?: ""
        }
    }

suspend fun httpGetBytes(
    url: String,
    headers: Map<String, String> = emptyMap(),
): ByteArray = withContext(Dispatchers.IO) {
    val builder = Request.Builder().url(url).get().globalIdentity()
    headers.forEach { (k, v) -> builder.header(k, v) }
    Http.client.newCall(builder.build()).execute().use { resp: Response ->
        if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code} for $url")
        resp.body?.bytes() ?: ByteArray(0)
    }
}

suspend fun httpPostJson(
    url: String,
    body: String,
    headers: Map<String, String> = emptyMap(),
): String = withContext(Dispatchers.IO) {
    val builder = Request.Builder().url(url)
        .post(okhttp3.RequestBody.create("application/json; charset=utf-8".toMediaType(), body))
        .globalIdentity()
        .header("Content-Type", "application/json")
    headers.forEach { (k, v) -> builder.header(k, v) }
    Http.client.newCall(builder.build()).execute().use { resp: Response ->
        if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code} for $url")
        resp.body?.string() ?: ""
    }
}

/**
 * Form-encoded POST (`application/x-www-form-urlencoded`) with browser-like defaults.
 *
 * WordPress admin-ajax.php endpoints — the ones the built-in scrapers use to lazily load a
 * player mirror — only accept this shape, and every header (including `User-Agent`) can be
 * overridden through [headers] so the request looks like the site's own XHR.
 */
suspend fun httpPostForm(
    url: String,
    fields: Map<String, String>,
    headers: Map<String, String> = emptyMap(),
): String = withContext(Dispatchers.IO) {
    val body = fields.entries.joinToString("&") {
        "${java.net.URLEncoder.encode(it.key, "UTF-8")}=${java.net.URLEncoder.encode(it.value, "UTF-8")}"
    }
    val builder = Request.Builder().url(url)
        .post(okhttp3.RequestBody.create("application/x-www-form-urlencoded; charset=utf-8".toMediaType(), body))
        .globalIdentity()
        .header("Accept", "*/*")
        .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
    headers.forEach { (k, v) -> builder.header(k, v) }
    Http.client.newCall(builder.build()).execute().use { resp: Response ->
        if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code} for $url")
        resp.body?.string() ?: ""
    }
}

/**
 * Transparently inflate gzip-compressed payloads.
 *
 * Some upstreams serve a *gzipped file* rather than a `Content-Encoding: gzip` response (GitHub
 * raw does this for the Keiyoushi `index.pb`), which OkHttp will not auto-decode for us.
 */
fun maybeGunzip(data: ByteArray): ByteArray {
    if (data.size < 2 || data[0] != 0x1f.toByte() || data[1] != 0x8b.toByte()) return data
    return try {
        java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(data)).use { it.readBytes() }
    } catch (e: Exception) {
        data
    }
}

fun parseJson(text: String): JsonElement = JsonParser.parseString(text)

fun JsonElement?.str(name: String): String? =
    this?.takeIf { it.isJsonObject }?.asJsonObject?.get(name)?.takeIf { !it.isJsonNull }?.asString

fun JsonElement?.obj(name: String): JsonElement? =
    this?.takeIf { it.isJsonObject }?.asJsonObject?.get(name)
