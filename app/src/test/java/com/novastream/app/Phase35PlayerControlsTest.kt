package com.novastream.app

import com.novastream.app.ui.player.PLAYER_CONTROLS_HIDE_MS
import com.novastream.app.ui.player.PlayerControlsState
import com.novastream.app.ui.player.PlayerTapAction
import com.novastream.app.ui.player.forcedControlsState
import com.novastream.app.ui.player.playerTapAction
import com.novastream.app.ui.player.shouldAutoHideControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 35 — unified touch-controller policy (pure JVM, no player needed). */
class Phase35PlayerControlsTest {

    @Test
    fun `hide countdown is three seconds`() {
        assertEquals(3000L, PLAYER_CONTROLS_HIDE_MS)
    }

    @Test
    fun `auto-hide runs only while playing ready unlocked and idle`() {
        assertTrue(shouldAutoHideControls(true, false, false, false, false))
        assertFalse(shouldAutoHideControls(false, false, false, false, false))
        assertFalse(shouldAutoHideControls(true, true, false, false, false))
        assertFalse(shouldAutoHideControls(true, false, true, false, false))
        assertFalse(shouldAutoHideControls(true, false, false, true, false))
        assertFalse(shouldAutoHideControls(true, false, false, false, true))
        assertFalse(shouldAutoHideControls(true, false, false, false, false, hasError = true))
    }

    @Test
    fun `single tap hides when visible and reveals when hidden`() {
        assertEquals(
            PlayerTapAction.HIDE_NOW,
            playerTapAction(PlayerControlsState.VISIBLE, locked = false),
        )
        assertEquals(
            PlayerTapAction.REVEAL_AND_RESTART_TIMER,
            playerTapAction(PlayerControlsState.HIDDEN, locked = false),
        )
        assertEquals(
            PlayerTapAction.REVEAL_AND_RESTART_TIMER,
            playerTapAction(PlayerControlsState.HIDING, locked = false),
        )
    }

    @Test
    fun `single tap is ignored while locked`() {
        assertEquals(
            PlayerTapAction.IGNORED_LOCKED,
            playerTapAction(PlayerControlsState.VISIBLE, locked = true),
        )
        assertEquals(
            PlayerTapAction.IGNORED_LOCKED,
            playerTapAction(PlayerControlsState.HIDDEN, locked = true),
        )
    }

    @Test
    fun `locked forces hidden`() {
        assertEquals(
            PlayerControlsState.HIDDEN,
            forcedControlsState(true, true, false, false, false),
        )
    }

    @Test
    fun `pause buffering error sheet and scrubbing freeze visible`() {
        assertEquals(
            PlayerControlsState.VISIBLE,
            forcedControlsState(false, false, false, false, false),
        )
        assertEquals(
            PlayerControlsState.VISIBLE,
            forcedControlsState(false, true, true, false, false),
        )
        assertEquals(
            PlayerControlsState.VISIBLE,
            forcedControlsState(false, true, false, true, false),
        )
        assertEquals(
            PlayerControlsState.VISIBLE,
            forcedControlsState(false, true, false, false, true),
        )
        assertEquals(
            PlayerControlsState.VISIBLE,
            forcedControlsState(false, true, false, false, false, hasError = true),
        )
    }

    @Test
    fun `active playback has no forced state so the timer owns it`() {
        assertNull(forcedControlsState(false, true, false, false, false))
    }
}
