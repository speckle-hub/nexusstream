package com.novastream.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared layout + motion tokens.
 *
 * Every screen used to hardcode its own `16.dp` gutter, `12.dp` gap and `4.dp` nudge, which is why
 * the poster grid, the rails and the detail rows never quite lined up. These are the single source
 * of truth — import them instead of typing literals.
 */
object AppSpacing {
    /** Left/right screen gutter. Used by every screen's outermost container. */
    val screen = 16.dp

    /** Gap between siblings inside a row/grid. */
    val item = 12.dp

    /** Vertical breathing room between major sections. */
    val section = 24.dp

    /** Smaller inset used inside cards and rows. */
    val inset = 12.dp

    /**
     * Poster width for every **horizontal media rail** (Home, Browse → Manga, NSFW).
     *
     * `MediaRow` used to be called with its 132 dp default on Manga/NSFW while Home passed 150 dp by
     * hand, so the same poster was a different size depending on which screen you were on — the
     * NSFW/Browse rails looked narrow next to the Home feed. Every rail must pass this token so a
     * rail card is identical everywhere (and no screen hardcodes a card width again).
     */
    val railCard = 150

    /**
     * Minimum width of a poster cell; drives `GridCells.Adaptive` so grids adapt to the screen.
     *
     * 108 dp looked right on paper but gave **two** columns on a 360 dp phone — the most common
     * logical width — because
     * `floor((360 - 2*16 + 12) / (108 + 12)) == 2`, which wasted the whole side of the screen and
     * forced extra scrolling. 88 dp keeps three columns on every phone from 320 dp up
     * (`floor((360 - 2*16 + 12) / (88 + 12)) == 3`, cards ≈ 101 dp wide) while still widening to
     * four-plus columns on tablets.
     */
    val posterMin = 88.dp
}

/**
 * One motion spec for the whole app.
 *
 * Durations used to be ad-hoc (`tween(220)`, `tween(900)`), so transitions felt unrelated to each
 * other. Everything now uses these tokens.
 */
object Motion {
    /** Press feedback, ripple, tiny state flips. */
    const val FAST = 150

    /** Screen-level transitions: nav bar, tab content, expand/collapse. */
    const val MEDIUM = 250

    /** Hero/carousel cross-fades, dialogs. */
    const val SLOW = 400

    /** Card press scale-down duration. */
    const val PRESS = 120

    /** How far a card shrinks while held. */
    const val PRESS_SCALE = 0.96f
}
