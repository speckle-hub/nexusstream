package com.moovio.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.TextStyle
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

    /** Nudge inside a control: icon to label, badge padding, chip inner inset. */
    val tight = 8.dp

    /** Gap between siblings inside a row/grid. */
    val item = 12.dp

    /** Vertical breathing room between major sections. */
    val section = 24.dp

    /** Smaller inset used inside cards and rows. */
    val inset = 12.dp

    /** Page-level breathing room: the gap under the last row, above a bottom sheet's content. */
    val page = 32.dp

    /**
     * Poster width for every **horizontal media rail** (Home, Browse → Manga/Anime, NSFW).
     *
     * Every rail passes this token so a rail card is the same size on every screen. It was 150 dp,
     * but that only fit **two** cards across a 360 dp phone (`(360 − 32) / 150 ≈ 2.2`) while the
     * Search/grid posters sat at ~101 dp three-across — so the Browse → Anime / NSFW rails read as
     * "2 rows" next to Search's 3. 100 dp matches the 3-across Search grid density on phones
     * (`3·100 + 2·12 ≤ 360 − 32`) while the rails still widen on larger screens via the padding.
     */
    val railCard = 100

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
/**
 * Radius by element size, so a control is not automatically a pill.
 *
 * Nested radius math: inner = outer − padding, never below 4dp. A 16dp card with 8dp padding holds
 * 8dp-radius children.
 */
object AppRadius {
    /** Tags, resolution badges, keycap-like micro chips. */
    const val chip = 6

    /** Buttons, inputs, rows, selected chips. */
    const val control = 10

    /** Poster artwork, cards. */
    const val card = 12

    /** Panels, sheets, the hero's inner surfaces. */
    const val panel = 16
}

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

    /**
     * The one spring the whole app settles on.
     *
     * Every animated position used to be either a rigid `tween` (which stops dead) or a
     * `DampingRatioLowBouncy` spring (which overshoots and wobbles back). Neither is how a physical
     * control behaves: a page, an indicator or a morph decelerates into place and stops. That is
     * exactly `DampingRatioNoBouncy` with low stiffness, so motion reads as weight rather than
     * bounce. Generic so the same curve drives `Dp` offsets, `Float` scroll targets and the `Rect`
     * of a shared-element bounds transform.
     */
    fun <T> settle(stiffness: Float = Spring.StiffnessLow): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness)
}

/**
 * Tabular figures for numeric readouts.
 *
 * Proportionally-spaced digits are *narrower* for a `1` than a `8`, so a counter, byte total or ETA
 * visibly re-flows each time a digit changes — the text jitters and its neighbours shift. Tabular
 * figures give every digit one advance width, so the number grows/moves without the layout dancing.
 *
 * No-op for word text (only digits are affected), so it is safe to apply to mixed labels.
 */
val TextStyle.tnum: TextStyle get() = copy(fontFeatureSettings = "tnum")
