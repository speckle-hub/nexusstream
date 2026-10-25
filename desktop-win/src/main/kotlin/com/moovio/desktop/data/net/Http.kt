package com.moovio.desktop.data.net

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small HTTP layer over the JDK client (no extra dependency, HTTP/2, redirects, timeouts).
 * Every call is isolated: a timeout or non-2xx response throws a message the caller can surface
 * per add-on without taking the rest of the app down.
 */
class Http(
    private val userAgent: String = "Moovio/1.0 (Windows; +https://github.com/speckle-hub/nexusstream)",
) {
    private val client: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 15_000,
    ): String = withContext(Dispatchers.IO) {
        val request = requestBuilder(url, timeoutMs)
            .header("User-Agent", userAgent)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .GET()
            .build()
        send(request)
    }

    suspend fun postJson(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 15_000,
    ): String = withContext(Dispatchers.IO) {
        val request = requestBuilder(url, timeoutMs)
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        send(request)
    }

    private fun requestBuilder(url: String, timeoutMs: Long): HttpRequest.Builder =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMillis(timeoutMs))

    private fun send(request: HttpRequest): String {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw HttpException(response.statusCode(), request.uri().toString())
        }
        return response.body()
    }
}

class HttpException(val status: Int, val url: String) :
    RuntimeException("HTTP $status for $url")
