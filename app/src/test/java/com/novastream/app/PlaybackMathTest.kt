package com.novastream.app

import com.novastream.app.ui.player.activeCueIndex
import com.novastream.app.ui.player.clampSubtitleSyncMs
import com.novastream.app.ui.player.formatBitrate
import com.novastream.app.ui.player.formatFps
import com.novastream.app.ui.player.formatSubtitleSync
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackMathTest {

    // ---- activeCueIndex ---------------------------------------------------------

    @Test
    fun `picks the last cue set at or before the target`() {
        val times = listOf(0L, 1_000L, 2_500L)
        assertEquals(0, activeCueIndex(times, 0))
        assertEquals(0, activeCueIndex(times, 999))
        assertEquals(1, activeCueIndex(times, 1_000))
        assertEquals(1, activeCueIndex(times, 2_499))
        assertEquals(2, activeCueIndex(times, 9_999))
    }

    @Test
    fun `returns minus one before the first cue event`() {
        assertEquals(-1, activeCueIndex(listOf(5_000L, 6_000L), 4_999))
        assertEquals(-1, activeCueIndex(emptyList(), 1_000))
    }

    @Test
    fun `a positive delay looks back and a negative offset reaches forward`() {
        val times = listOf(0L, 5_000L, 10_000L)
        // position 12s, +2s delay -> show the 10s cue set
        assertEquals(2, activeCueIndex(times, 12_000 - 2_000))
        // position 8s, -2s (advance) -> target 10s -> show the 10s cue set
        assertEquals(2, activeCueIndex(times, 8_000 + 2_000))
    }

    // ---- subtitle sync clamp / label -------------------------------------------

    @Test
    fun `subtitle sync clamps to plus or minus ten seconds`() {
        assertEquals(10_000, clampSubtitleSyncMs(99_000))
        assertEquals(-10_000, clampSubtitleSyncMs(-99_000))
        assertEquals(1_500, clampSubtitleSyncMs(1_500))
    }

    @Test
    fun `subtitle sync labels read naturally`() {
        assertEquals("Off", formatSubtitleSync(0))
        assertEquals("+1.5 s", formatSubtitleSync(1_500))
        assertEquals("-2.0 s", formatSubtitleSync(-2_000))
    }

    // ---- stats formatting ------------------------------------------------------

    @Test
    fun `bitrate and fps readouts handle missing values`() {
        assertEquals("N/A", formatBitrate(0))
        assertEquals("N/A", formatBitrate(-5))
        assertEquals("850 kbps", formatBitrate(850_000))
        assertEquals("4.2 Mbps", formatBitrate(4_200_000))
        assertEquals("", formatFps(0f))
        assertEquals("24 fps", formatFps(23.976f))
    }
}
