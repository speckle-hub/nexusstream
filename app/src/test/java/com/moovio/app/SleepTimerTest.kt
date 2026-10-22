package com.moovio.app

import com.moovio.app.ui.player.SleepTimerOption
import com.moovio.app.ui.player.delayMs
import com.moovio.app.ui.player.label
import com.moovio.app.ui.player.sleepStatusLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 29 sleep-timer mapping and label logic (pure JVM, no player needed). */
class SleepTimerTest {

    @Test
    fun `fixed options map to the right delays`() {
        assertEquals(15 * 60_000L, SleepTimerOption.MIN_15.delayMs())
        assertEquals(30 * 60_000L, SleepTimerOption.MIN_30.delayMs())
        assertEquals(45 * 60_000L, SleepTimerOption.MIN_45.delayMs())
        assertEquals(60 * 60_000L, SleepTimerOption.MIN_60.delayMs())
    }

    @Test
    fun `off and end-of-episode have no fixed delay`() {
        assertNull(SleepTimerOption.OFF.delayMs())
        assertNull(SleepTimerOption.END_OF_EPISODE.delayMs())
    }

    @Test
    fun `only fixed options are timed`() {
        assertTrue(SleepTimerOption.MIN_15.isTimed)
        assertTrue(SleepTimerOption.MIN_30.isTimed)
        assertTrue(SleepTimerOption.MIN_45.isTimed)
        assertTrue(SleepTimerOption.MIN_60.isTimed)
        assertFalse(SleepTimerOption.OFF.isTimed)
        assertFalse(SleepTimerOption.END_OF_EPISODE.isTimed)
    }

    @Test
    fun `sheet labels read naturally`() {
        assertEquals("Off", SleepTimerOption.OFF.label())
        assertEquals("15 minutes", SleepTimerOption.MIN_15.label())
        assertEquals("30 minutes", SleepTimerOption.MIN_30.label())
        assertEquals("45 minutes", SleepTimerOption.MIN_45.label())
        assertEquals("60 minutes", SleepTimerOption.MIN_60.label())
        assertEquals("End of episode", SleepTimerOption.END_OF_EPISODE.label())
    }

    @Test
    fun `status label counts down whole minutes`() {
        // 30-minute timer, 6 minutes elapsed -> "Sleep 24m".
        val deadline = 1_000_000L + 30 * 60_000L
        assertEquals(
            "Sleep 24m",
            sleepStatusLabel(SleepTimerOption.MIN_30, deadline, 1_000_000L + 6 * 60_000L),
        )
    }

    @Test
    fun `status label clamps past-deadline to zero`() {
        val deadline = 1_000_000L
        assertEquals(
            "Sleep 0m",
            sleepStatusLabel(SleepTimerOption.MIN_15, deadline, deadline + 5_000L),
        )
    }

    @Test
    fun `status label covers episode-end and off`() {
        assertEquals(
            "Sleep: episode end",
            sleepStatusLabel(SleepTimerOption.END_OF_EPISODE, null, 0L),
        )
        assertNull(sleepStatusLabel(SleepTimerOption.OFF, null, 0L))
        assertNull(sleepStatusLabel(SleepTimerOption.MIN_30, null, 0L))
    }
}
