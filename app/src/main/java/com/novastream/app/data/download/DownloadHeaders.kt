package com.novastream.app.data.download

import com.novastream.app.data.remote.UA

/**
 * Builds the header map a Media3 download request must send.
 *
 * The offline stack historically used a bare `DefaultHttpDataSource`
 * with only a User-Agent, so any stream that needs a page `Referer` / age `Cookie` (Pornhub's
 * `hm-h` CDN, Cloudflare-fronted sites, hotlink-protected hosts) failed immediately or mid-transfer
 * even though the *player* could play it. This mirrors the player's OkHttp interceptor exactly:
 *
 *  - the stream's own headers always win;
 *  - the user's global custom identity (Settings → Network) fills any header the stream omitted;
 *  - the User-Agent falls back to the app's browser UA when nothing else set one.
 *
 * Pure and Android-free so it can be unit-tested, and critical to the downloader working at all
 * for header-gated hosts.
 */
fun downloadRequestHeaders(
    streamHeaders: Map<String, String>,
    customUserAgent: String = "",
    customReferer: String = "",
    customCookie: String = "",
): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    streamHeaders.forEach { (k, v) ->
        if (k.isNotBlank() && v.isNotBlank()) out[k] = v
    }
    if (out["User-Agent"].isNullOrBlank()) {
        out["User-Agent"] = customUserAgent.ifBlank { UA }
    }
    if (out["Referer"].isNullOrBlank() && customReferer.isNotBlank()) {
        out["Referer"] = customReferer
    }
    if (out["Cookie"].isNullOrBlank() && customCookie.isNotBlank()) {
        out["Cookie"] = customCookie
    }
    return out
}
