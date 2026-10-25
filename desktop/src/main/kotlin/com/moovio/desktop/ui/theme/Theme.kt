package com.moovio.desktop.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Extra design tokens exposed to the whole tree. */
data class MoovioColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val glass: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val isDark: Boolean,
)

val LocalMoovioColors: ProvidableCompositionLocal<MoovioColors> = staticCompositionLocalOf {
    MoovioColors(
        background = MoovioBackground,
        surface = MoovioSurface,
        surfaceElevated = MoovioSurfaceElevated,
        glass = MoovioSurfaceGlass,
        outline = MoovioOutline,
        textPrimary = MoovioTextPrimary,
        textSecondary = MoovioTextSecondary,
        textTertiary = MoovioTextTertiary,
        accent = AccentViolet,
        isDark = true,
    )
}

val MoovioTypography = Typography(
    h1 = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    h2 = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    h3 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    h4 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    subtitle1 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    body1 = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    body2 = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    button = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    caption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun MoovioTheme(
    content: @Composable () -> Unit,
) {
    val colors = MoovioColors(
        background = MoovioBackground,
        surface = MoovioSurface,
        surfaceElevated = MoovioSurfaceElevated,
        glass = MoovioSurfaceGlass,
        outline = MoovioOutline,
        textPrimary = MoovioTextPrimary,
        textSecondary = MoovioTextSecondary,
        textTertiary = MoovioTextTertiary,
        accent = AccentViolet,
        isDark = true,
    )
    val scheme = darkColors(
        primary = colors.accent,
        primaryVariant = AccentVioletDim,
        secondary = AccentCyan,
        background = colors.background,
        surface = colors.surface,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = colors.textPrimary,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceElevated,
    )

    CompositionLocalProvider(LocalMoovioColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MoovioTypography,
            content = content,
        )
    }
}

/** Convenience accessor so screens don't reach into the composition local directly. */
val Composable.moovioColors: MoovioColors
    get() = LocalMoovioColors.current
