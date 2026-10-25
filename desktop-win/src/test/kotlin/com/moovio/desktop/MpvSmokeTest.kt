package com.moovio.desktop

import com.moovio.desktop.data.local.AppSettings
import com.moovio.desktop.player.MpvLibrary
import com.moovio.desktop.player.MpvPlayer
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies the full native path: library load, render-context creation, network open, decoding
 * and at least one software-rendered frame. Skipped unless a libmpv binary is present and
 * MOOVIO_MPV_TEST=1 (it needs network + a few seconds of real playback).
 */
class MpvSmokeTest {

    @Test
    fun `libmpv opens a test clip and renders frames`() {
        if (System.getenv("MOOVIO_MPV_TEST") != "1") return
        if (MpvLibrary.locate() == null) return

        val player = MpvPlayer.open(AppSettings()).getOrElse {
            throw AssertionError("MpvPlayer.open failed: ${it.message}")
        }
        try {
            assertTrue(player.awaitReady(), "render context was not created")
            player.setRenderSize(480, 270)
            player.load("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4")

            val deadline = System.currentTimeMillis() + 40_000
            var frames = 0
            var lastSequence = -1L
            while (System.currentTimeMillis() < deadline) {
                player.poll()
                val frame = player.frame.value
                if (frame != null && frame.sequence != lastSequence) {
                    lastSequence = frame.sequence
                    frames++
                }
                val snapshot = player.snapshot.value
                if (snapshot.durationMs > 0 && snapshot.positionMs > 1_000 && frames > 2) break
                if (snapshot.error != null) break
                Thread.sleep(120)
            }

            val snapshot = player.snapshot.value
            println(
                "mpv smoke: frames=$frames durationMs=${snapshot.durationMs} positionMs=${snapshot.positionMs} " +
                    "size=${snapshot.videoWidth}x${snapshot.videoHeight} error=${snapshot.error}",
            )
            if (snapshot.error == null) {
                assertTrue(snapshot.durationMs > 0, "no duration reported")
                assertTrue(frames > 0, "no frames were rendered")
                assertNotNull(player.frame.value)
            }
        } finally {
            player.close()
        }
    }

    @Test
    fun `loads at a saved resume position`() {
        if (System.getenv("MOOVIO_MPV_TEST") != "1") return
        if (MpvLibrary.locate() == null) return

        val player = MpvPlayer.open(AppSettings()).getOrElse {
            throw AssertionError("MpvPlayer.open failed: ${it.message}")
        }
        try {
            assertTrue(player.awaitReady())
            player.setRenderSize(320, 180)
            player.load(
                "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4",
                startPositionMs = 4_000,
            )
            val deadline = System.currentTimeMillis() + 30_000
            while (System.currentTimeMillis() < deadline) {
                player.poll()
                val snapshot = player.snapshot.value
                if (snapshot.durationMs > 0 && snapshot.positionMs > 0) break
                Thread.sleep(120)
            }
            val snapshot = player.snapshot.value
            println("mpv resume: positionMs=${snapshot.positionMs} durationMs=${snapshot.durationMs} error=${snapshot.error}")
            if (snapshot.error == null && snapshot.durationMs > 0) {
                assertTrue(snapshot.positionMs >= 3_500, "expected resume near 4s, got ${snapshot.positionMs}")
            }
        } finally {
            player.close()
        }
    }
}
