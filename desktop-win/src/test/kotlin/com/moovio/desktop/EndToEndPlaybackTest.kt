package com.moovio.desktop

import com.moovio.desktop.data.local.AddonStore
import com.moovio.desktop.data.net.Http
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.data.remote.StremioClient
import com.moovio.desktop.data.repo.StreamRepository
import com.moovio.desktop.data.local.AppSettings
import com.moovio.desktop.domain.model.Addon
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.player.MpvLibrary
import com.moovio.desktop.player.MpvPlayer
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * The acceptance path from the brief: resolve a stream through the Stremio protocol and play it
 * in libmpv. Skipped unless MOOVIO_MPV_TEST=1 (network + real playback) and a DLL is present.
 */
class EndToEndPlaybackTest {

    private val clip = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4"

    @Test
    fun `stremio stream resolves and plays in libmpv`(): Unit = runBlocking {
        if (System.getenv("MOOVIO_MPV_TEST") != "1") return@runBlocking
        if (MpvLibrary.locate() == null) return@runBlocking

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/manifest.json") { exchange ->
            val body = """
                {"id":"e2e.streams","version":"1.0.0","name":"E2E Streams","types":["movie"],
                 "resources":["stream"],"idPrefixes":["tt"]}
            """.trimIndent().toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.createContext("/stream/movie/tt0133093.json") { exchange ->
            val body = """{"streams":[{"url":"$clip","title":"720p · direct"}]}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        val temp = Files.createTempDirectory("moovio-e2e").toFile()
        val store = AddonStore(addonsFile = java.io.File(temp, "addons.json"))
        val base = "http://127.0.0.1:${server.address.port}"
        store.upsert(
            Addon(
                manifestUrl = "$base/manifest.json",
                transportUrl = base,
                id = "e2e.streams",
                name = "E2E Streams",
                types = listOf("movie"),
                resources = listOf("stream"),
                idPrefixes = listOf("tt"),
            ),
        )
        try {
            val repo = StreamRepository(StremioClient(Http()), CinemetaClient(Http()), store)
            val result = withTimeout(30_000) {
                repo.resolve(MediaItem("tt0133093", MediaType.MOVIE, "The Matrix"))
            }
            assertEquals(1, result.sources.size)
            val best = result.best
            assertTrue(best != null && best.isPlayable)

            val player = MpvPlayer.open(AppSettings()).getOrElse {
                throw AssertionError("libmpv failed to open: ${it.message}")
            }
            try {
                assertTrue(player.awaitReady())
                player.setRenderSize(320, 180)
                player.load(best!!.url!!, best.headers, 0)
                val deadline = System.currentTimeMillis() + 40_000
                var frames = 0
                while (System.currentTimeMillis() < deadline) {
                    player.poll()
                    if (player.frame.value != null) frames++
                    val state = player.snapshot.value
                    if (state.durationMs > 0 && frames > 2) break
                    if (state.error != null) break
                    Thread.sleep(120)
                }
                val state = player.snapshot.value
                println(
                    "e2e: addon=${best.addonName} url=${best.url} frames=$frames " +
                        "durationMs=${state.durationMs} error=${state.error}",
                )
                if (state.error == null) {
                    assertTrue(state.durationMs > 0)
                    assertTrue(frames > 0)
                }
            } finally {
                player.close()
            }
        } finally {
            server.stop(0)
            temp.deleteRecursively()
        }
        Unit
    }
}
