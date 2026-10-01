package com.novastream.app.ui.theme

import androidx.compose.ui.graphics.Color

// Core surfaces (dark-first premium palette)
val NsBackground = Color(0xFF0A0A0F)
val NsSurface = Color(0xFF12121A)
val NsSurfaceElevated = Color(0xFF1A1A25)
val NsSurfaceGlass = Color(0xCC1A1A25)
val NsOutline = Color(0xFF2A2A38)

// Accents
val AccentViolet = Color(0xFF7C5CFF)
val AccentVioletDim = Color(0xFF5B3FE0)
val AccentCyan = Color(0xFF35D6E0)
val AccentPink = Color(0xFFFF5C8A)
val AccentAmber = Color(0xFFFFB020)
val AccentEmerald = Color(0xFF35E0A1)

// Text
val NsTextPrimary = Color(0xFFF2F2F7)
val NsTextSecondary = Color(0xFFA0A0B0)
val NsTextTertiary = Color(0xFF6A6A7A)

// Light surfaces
val LBackground = Color(0xFFF6F6FB)
val LSurface = Color(0xFFFFFFFF)
val LSurfaceElevated = Color(0xFFEFEFF6)
val LTextPrimary = Color(0xFF12121A)
val LTextSecondary = Color(0xFF555566)

data class Accent(val key: String, val label: String, val color: Color)

val Accents = listOf(
    Accent("violet", "Violet", AccentViolet),
    Accent("cyan", "Cyan", AccentCyan),
    Accent("pink", "Pink", AccentPink),
    Accent("amber", "Amber", AccentAmber),
    Accent("emerald", "Emerald", AccentEmerald),
)

fun accentFor(key: String?): Color = Accents.firstOrNull { it.key == key }?.color ?: AccentViolet
