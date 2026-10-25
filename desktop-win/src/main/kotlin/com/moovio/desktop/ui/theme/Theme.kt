package com.moovio.desktop.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AccentPreset(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
)

object Accents {
    val violet = AccentPreset("violet", "Violet", Color(0xFF8B5CF6), Color(0xFF22D3EE))
    val cyan = AccentPreset("cyan", "Cyan", Color(0xFF06B6D4), Color(0xFF818CF8))
    val rose = AccentPreset("rose", "Rose", Color(0xFFF43F5E), Color(0xFFFB923C))
    val amber = AccentPreset("amber", "Amber", Color(0xFFF59E0B), Color(0xFFEF4444))
    val emerald = AccentPreset("emerald", "Emerald", Color(0xFF10B981), Color(0xFF84CC16))

    val all = listOf(violet, cyan, rose, amber, emerald)

    fun byId(id: String?): AccentPreset = all.firstOrNull { it.id == id } ?: violet
}

object Palette {
    val background = Color(0xFF07070B)
    val backgroundElevated = Color(0xFF0B0B12)
    val surface = Color(0xFF101018)
    val surfaceHigh = Color(0xFF16161F)
    val surfaceHighest = Color(0xFF1D1D29)
    val outline = Color(0xFF2A2A3A)
    val textPrimary = Color(0xFFF5F5FA)
    val textSecondary = Color(0xFFA0A0B8)
    val textTertiary = Color(0xFF6E6E85)
    val danger = Color(0xFFEF4444)
    val success = Color(0xFF22C55E)
    val warning = Color(0xFFF59E0B)
}

val LocalAccent = staticCompositionLocalOf { Accents.violet }
val LocalReducedMotion = staticCompositionLocalOf { false }

val MoovioTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
    ),
)

@Composable
fun MoovioTheme(
    accentId: String = "violet",
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val accent = Accents.byId(accentId)
    val scheme = darkColorScheme(
        primary = accent.primary,
        onPrimary = Color.White,
        primaryContainer = accent.primary.copy(alpha = 0.22f),
        onPrimaryContainer = accent.primary,
        secondary = accent.secondary,
        onSecondary = Color(0xFF07101A),
        background = Palette.background,
        onBackground = Palette.textPrimary,
        surface = Palette.surface,
        onSurface = Palette.textPrimary,
        surfaceVariant = Palette.surfaceHigh,
        onSurfaceVariant = Palette.textSecondary,
        surfaceContainer = Palette.surface,
        surfaceContainerHigh = Palette.surfaceHigh,
        surfaceContainerHighest = Palette.surfaceHighest,
        outline = Palette.outline,
        outlineVariant = Palette.outline.copy(alpha = 0.6f),
        error = Palette.danger,
    )
    CompositionLocalProvider(
        LocalAccent provides accent,
        LocalReducedMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MoovioTypography,
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}

val AccentColor: Color
    @Composable get() = LocalAccent.current.primary

val AccentSecondary: Color
    @Composable get() = LocalAccent.current.secondary

val reduceMotion: Boolean
    @Composable get() = LocalReducedMotion.current

/** Frosted-glass surface: translucent gradient, hairline top highlight and border. */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(16.dp),
    tint: Color = Color.White,
    alpha: Float = 0.05f,
    borderAlpha: Float = 0.09f,
): Modifier = this
    .background(
        Brush.verticalGradient(
            listOf(
                tint.copy(alpha = alpha * 1.7f),
                tint.copy(alpha = alpha * 0.65f),
            ),
        ),
        shape,
    )
    .border(1.dp, tint.copy(alpha = borderAlpha), shape)

/** Subtle accent glow used behind posters, hero art and active nav items. */
fun accentGlow(accent: Color, radius: Float = 0.85f): Brush = Brush.radialGradient(
    colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent),
    center = Offset.Unspecified,
    radius = radius,
)

fun horizontalScrim(): Brush = Brush.horizontalGradient(
    listOf(
        Palette.background,
        Palette.background.copy(alpha = 0.92f),
        Palette.background.copy(alpha = 0.2f),
        Color.Transparent,
    ),
)

fun bottomScrim(): Brush = Brush.verticalGradient(
    listOf(
        Color.Transparent,
        Palette.background.copy(alpha = 0.55f),
        Palette.background,
    ),
)
