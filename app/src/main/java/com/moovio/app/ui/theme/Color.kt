package com.moovio.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// Core surfaces. One neutral ramp, slightly blue-shifted so it reads as a cinema at night
// rather than a flat grey — but never so tinted that a surface reads as a colour on its own.
val MoovioBackground = Color(0xFF0A0A0F)
val MoovioSurface = Color(0xFF12121A)
val MoovioSurfaceElevated = Color(0xFF1A1A25)
val MoovioOutline = Color(0xFF2A2A38)

// True AMOLED (pitch black) surfaces, used when Settings → Theme → True AMOLED is on.
// Cards stay #000 too so a poster grid has no visible rectangles around the artwork, and only the
// raised step lifts to #0B0B0B so sheets/menus are still distinguishable from the page.
val MoovioAmoledBackground = Color(0xFF000000)
val MoovioAmoledSurface = Color(0xFF000000)
val MoovioAmoledElevated = Color(0xFF0B0B0B)

// Accents. The violet is the real brand colour (it is the launcher icon's fill), the rest are
// user-selectable alternatives from Settings → Appearance.
val AccentViolet = Color(0xFF7C5CFF)
val AccentVioletDim = Color(0xFF5B3FE0)
val AccentCyan = Color(0xFF35D6E0)
val AccentPink = Color(0xFFFF5C8A)
val AccentAmber = Color(0xFFFFB020)
val AccentEmerald = Color(0xFF35E0A1)

// Phase 11 named presets.
val AccentNeonPurple = Color(0xFFB45CFF)
val AccentCrimson = Color(0xFFFF3355)
val AccentSunset = Color(0xFFFF7A33)

// Text
val MoovioTextPrimary = Color(0xFFF2F2F7)
val MoovioTextSecondary = Color(0xFFA0A0B0)
// Was #6A6A7A — only ~3.5:1 on MoovioBackground, below the 4.5:1 WCAG AA floor for body text. This is
// used for metadata lines under every poster, episode rows and library subtitles, so it had to be
// legible rather than merely quiet.
val MoovioTextTertiary = Color(0xFF8A8A9C)

// Light surfaces
val LBackground = Color(0xFFF6F6FB)
val LSurface = Color(0xFFFFFFFF)
val LSurfaceElevated = Color(0xFFEFEFF6)
val LTextPrimary = Color(0xFF12121A)
val LTextSecondary = Color(0xFF555566)

/** Near-black used as "ink" on bright accent fills, so a fill never needs pure black. */
val ActionInk = Color(0xFF0B0B0D)

/** WCAG AA floor for normal-size text. */
private const val MIN_TEXT_CONTRAST = 4.5f

/** Contrast ratio between two opaque colours, per WCAG 2.1 relative luminance. */
fun Color.contrastWith(other: Color): Float {
    val a = luminance()
    val b = other.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

/**
 * The (fill, content) pair used for solid accent surfaces — primary buttons, the selected chip,
 * the checked switch track.
 *
 * Every accent in [Accents] is user-selectable, and several of them are too bright for white label
 * text (amber reaches only ~1.8:1 against white). Rather than hardcoding a second "dim" variant per
 * accent, this picks the legible pairing at runtime: white text when the accent is dark enough,
 * near-black ink when it is bright, and otherwise darkens the accent step by step until white text
 * clears 4.5:1 — so sRGB saturation shifts but the hue family never changes.
 */
fun Color.actionFill(): Pair<Color, Color> {
    if (contrastWith(Color.White) >= MIN_TEXT_CONTRAST) return this to Color.White
    if (contrastWith(ActionInk) >= MIN_TEXT_CONTRAST) return this to ActionInk
    var fill = this
    var guard = 0
    while (fill.contrastWith(Color.White) < MIN_TEXT_CONTRAST && guard < 24) {
        fill = lerp(fill, Color.Black, 0.06f)
        guard++
    }
    return fill to Color.White
}

data class Accent(val key: String, val label: String, val color: Color)

val Accents = listOf(
    // Phase 11 preset themes, listed first so they read as the headline choices.
    Accent("neon_purple", "Neon Purple", AccentNeonPurple),
    Accent("emerald", "Emerald", AccentEmerald),
    Accent("crimson", "Crimson", AccentCrimson),
    Accent("sunset", "Sunset Orange", AccentSunset),
    // Original palette.
    Accent("violet", "Violet", AccentViolet),
    Accent("cyan", "Cyan", AccentCyan),
    Accent("pink", "Pink", AccentPink),
    Accent("amber", "Amber", AccentAmber),
)

fun accentFor(key: String?): Color = Accents.firstOrNull { it.key == key }?.color ?: AccentViolet
