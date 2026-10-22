package com.moovio.app.ui.player

/**
 * Phase 35 — player touch-controller state machine.
 *
 * The old overlay kept control visibility in a bare `Boolean` poked from four
 * competing `pointerInput` detectors (tap vs press raced for the same down
 * event, so taps intermittently failed to reveal/hide the controls). This file
 * owns the *decision* side of the controller as pure-JVM logic so it is
 * unit-tested (`Phase35PlayerControlsTest`); `PlayerScreen` owns the coroutine
 * timer and just asks this policy what to do.
 */
enum class PlayerControlsState { VISIBLE, HIDING, HIDDEN }

/** Deterministic outcome of a single-tap on the player surface. */
enum class PlayerTapAction { HIDE_NOW, REVEAL_AND_RESTART_TIMER, IGNORED_LOCKED }

/** Auto-hide countdown, shared by the policy and the composable timer. */
const val PLAYER_CONTROLS_HIDE_MS = 3000L

/**
 * True only when the overlay may hide itself: playing, ready (not buffering),
 * no sheet/picker open, not locked, not scrubbing and no blocking error.
 */
fun shouldAutoHideControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    sheetOpen: Boolean,
    locked: Boolean,
    scrubbing: Boolean,
    hasError: Boolean = false,
): Boolean =
    isPlaying && !isBuffering && !sheetOpen && !locked && !scrubbing && !hasError

/**
 * Deterministic single-tap: visible → hide now and cancel the timer; hidden →
 * reveal and start a fresh countdown; locked → ignore (only the lock toggle
 * may act).
 */
fun playerTapAction(state: PlayerControlsState, locked: Boolean): PlayerTapAction =
    when {
        locked -> PlayerTapAction.IGNORED_LOCKED
        state == PlayerControlsState.VISIBLE -> PlayerTapAction.HIDE_NOW
        else -> PlayerTapAction.REVEAL_AND_RESTART_TIMER
    }

/**
 * Forced visibility for states where auto-hide must not run: locked forces
 * hidden (only the lock toggle shows); paused / buffering / error / sheet /
 * scrubbing freeze on visible.
 */
fun forcedControlsState(
    locked: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    sheetOpen: Boolean,
    scrubbing: Boolean,
    hasError: Boolean = false,
): PlayerControlsState? =
    when {
        locked -> PlayerControlsState.HIDDEN
        sheetOpen || scrubbing || !isPlaying || isBuffering || hasError -> PlayerControlsState.VISIBLE
        else -> null
    }
