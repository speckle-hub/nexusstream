package com.moovio.desktop

import com.moovio.desktop.data.local.AddonStore
import com.moovio.desktop.data.net.Http
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.data.remote.StremioClient
import com.moovio.desktop.data.repo.StreamRepository
import com.moovio.desktop.domain.model.Addon
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * End-to-end Stremio protocol check against a local mock add-on, including the "one failing
 * add-on must not break the page" rule.
 */
class StremioStreamPathTest {

    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/good/manifest.json") { exchange ->
            val body = """
                {
                  "id": "test.good",
                  "version": "1.0.0",
                  "name": "Good Add-on",
                  "types": ["movie"],
                  "resources": ["stream"],
                  "idPrefixes": ["tt"]
                }
            """.trimIndent().toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        createContext("/good/stream/movie/tt0133093.json") { exchange ->
            val body = """
                {
                  "streams": [
                    {"url": "https://example.com/sd.mp4", "title": "480p direct"},
                    {"url": "https://example.com/fullHD.m3u8", "title": "1080p direct"},
                    {"infoHash": "deadbeef", "fileIdx": 0, "title": "1080p torrent · 👤 500"}
                  ]
                }
            """.trimIndent().toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        createContext("/broken/manifest.json") { exchange ->
            val body = """
                {"id":"test.broken","version":"1.0.0","name":"Broken Add-on","types":["movie"],
                 "resources":["stream"],"idPrefixes":["tt"]}
            """.trimIndent().toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        createContext("/broken/stream/movie/tt0133093.json") { exchange ->
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        }
        start()
    }

    @AfterTest
    fun stop() {
        server.stop(0)
    }

    private fun addon(path: String, name: String): Addon = Addon(
        manifestUrl = "http://127.0.0.1:${server.address.port}$path/manifest.json",
        transportUrl = "http://127.0.0.1:${server.address.port}$path",
        id = "test.${name.lowercase()}",
        name = name,
        types = listOf("movie"),
        resources = listOf("stream"),
        idPrefixes = listOf("tt"),
    )

    @Test
    fun `resolves ranked streams and isolates a failing add-on`(): Unit = runBlocking {
        val temp = Files.createTempDirectory("moovio-addons").toFile()
        val store = AddonStore(addonsFile = java.io.File(temp, "addons.json"))
        val client = StremioClient(Http())
        store.upsert(addon("/good", "Good"))
        store.upsert(addon("/broken", "Broken"))
        val repo = StreamRepository(client, CinemetaClient(Http()), store)

        val result = withTimeout(20_000) {
            repo.resolve(MediaItem("tt0133093", MediaType.MOVIE, "The Matrix"))
        }

        assertEquals(3, result.sources.size)
        assertEquals("https://example.com/fullHD.m3u8", result.best?.url)
        assertTrue(result.sources.any { it.isTorrent })
        assertEquals(1, result.failures.size)
        assertEquals("Broken", result.failures.single().addon)
        assertTrue(result.failures.single().message.contains("500") || result.failures.single().message.isNotBlank())
        temp.deleteRecursively()
        Unit
    }
}
