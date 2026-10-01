package com.novastream.app.data.adult

import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.remote.httpGet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Browser-like fetch helper for the adult scrapers.
 *
 * Adult tube sites routinely reject non-browser user agents and hide behind Cloudflare /
 * DDoS-Guard. We send a realistic mobile Chrome header set and translate blocks into a typed
 * [AdultSourceException] so a single dead source degrades to a clear message instead of taking
 * the whole Real 18+ tab down.
 */
object AdultHttp {

    const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"

    private val challengeMarkers = listOf(
        "just a moment",
        "cf-browser-verification",
        "checking if the site connection is secure",
        "attention required!",
        "ddos-guard",
        "enable javascript and cookies to continue",
    )

    /**
     * Short-timeout client used only to *check* a candidate stream before we offer it.
     * Deliberately tighter than [com.novastream.app.data.remote.Http.client]: a probe that hangs
     * would stall the whole stream list.
     */
    private val probeClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun get(url: String, referer: String? = null): String {
        val headers = mutableMapOf(
            "User-Agent" to BROWSER_UA,
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.9",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
        )
        if (!referer.isNullOrBlank()) headers["Referer"] = referer

        val body = try {
            httpGet(url, headers)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("403") || msg.contains("503") || msg.contains("429")) {
                throw AdultSourceException("Blocked by the site's bot protection", e)
            }
            throw AdultSourceException("Couldn't reach the site ($msg)", e)
        }
        if (looksLikeChallenge(body)) {
            throw AdultSourceException("Blocked by the site's bot protection")
        }
        return body
    }

    /**
     * Outcome of pre-flighting a candidate media URL. Only [DEAD] removes a stream: a
     * [NETWORK] result (timeout, DNS, reset) means we learned nothing about the link, and hiding
     * it would grey out Play on a flaky connection even though the player would succeed.
     */
    enum class ProbeResult { ALIVE, DEAD, NETWORK }

    /**
     * Pre-flight a single media URL with the exact headers the player will use.
     *
     * 4xx means the link is dead (Pornhub's `hm-h` node answers 410 without a Referer+cookie,
     * HQporner's CDN 404s once a clip is pulled); an HTML body means we were served an error
     * page; an HLS manifest must actually start with `#EXTM3U`. 429 is kept — rate limiting the
     * probe is not evidence the stream is gone.
     */
    suspend fun probe(url: String, headers: Map<String, String> = emptyMap()): ProbeResult =
        withContext(Dispatchers.IO) {
            try {
                val builder = Request.Builder().url(url).get()
                    .header("User-Agent", headers["User-Agent"] ?: BROWSER_UA)
                    .header("Accept", "*/*")
                    .header("Range", "bytes=0-2047")
                headers.forEach { (k, v) -> if (k != "User-Agent") builder.header(k, v) }

                probeClient.newCall(builder.build()).execute().use { resp ->
                    when {
                        resp.code == 429 -> ProbeResult.ALIVE
                        !resp.isSuccessful -> ProbeResult.DEAD
                        else -> {
                            val contentType = resp.header("Content-Type").orEmpty().lowercase()
                            if (contentType.contains("text/html")) return@use ProbeResult.DEAD
                            val wantsManifest = url.contains(".m3u8", ignoreCase = true) ||
                                contentType.contains("mpegurl")
                            if (!wantsManifest) return@use ProbeResult.ALIVE
                            val head = resp.body?.source()?.let { source ->
                                val buffer = okio.Buffer()
                                source.read(buffer, 64)
                                buffer.readUtf8()
                            }.orEmpty()
                            if (head.contains("#EXTM3U")) ProbeResult.ALIVE else ProbeResult.DEAD
                        }
                    }
                }
            } catch (e: Exception) {
                ProbeResult.NETWORK
            }
        }

    /** Drop only the streams that fail with a definitive server rejection; keep order. */
    internal suspend fun verifyPlayable(streams: List<StreamSource>): List<StreamSource> {
        if (streams.isEmpty()) return streams
        if (streams.none { !it.playableUrl.isNullOrBlank() }) return emptyList()
        return coroutineScope {
            streams
                .map { stream ->
                    async {
                        val url = stream.playableUrl
                        if (url.isNullOrBlank()) return@async null
                        when (probe(url, stream.headers)) {
                            ProbeResult.DEAD -> null
                            ProbeResult.ALIVE, ProbeResult.NETWORK -> stream
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }

    private fun looksLikeChallenge(body: String): Boolean {
        val head = body.take(4000).lowercase()
        return challengeMarkers.any { head.contains(it) }
    }
}
