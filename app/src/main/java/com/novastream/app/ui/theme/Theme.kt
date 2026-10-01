package com.novastream.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Extra design tokens exposed to the whole tree. */
data class NovaColors(
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

val LocalNovaColors = staticCompositionLocalOf {
    NovaColors(
        background = NsBackground,
        surface = NsSurface,
        surfaceElevated = NsSurfaceElevated,
        glass = NsSurfaceGlass,
        outline = NsOutline,
        textPrimary = NsTextPrimary,
        textSecondary = NsTextSecondary,
        textTertiary = NsTextTertiary,
        accent = AccentViolet,
        isDark = true,
    )
}

@Composable
fun NovaStreamTheme(
    darkTheme: Boolean = true,
    accentKey: String? = "violet",
    content: @Composable () -> Unit,
) {
    val accent = accentFor(accentKey)
    val nova = if (darkTheme) {
        NovaColors(
            background = NsBackground,
            surface = NsSurface,
            surfaceElevated = NsSurfaceElevated,
            glass = NsSurfaceGlass,
            outline = NsOutline,
            textPrimary = NsTextPrimary,
            textSecondary = NsTextSecondary,
            textTertiary = NsTextTertiary,
            accent = accent,
            isDark = true,
        )
    } else {
        NovaColors(
            background = LBackground,
            surface = LSurface,
            surfaceElevated = LSurfaceElevated,
            glass = Color(0xE6FFFFFF),
            outline = Color(0xFFDDDDE6),
            textPrimary = LTextPrimary,
            textSecondary = LTextSecondary,
            textTertiary = Color(0xFF8888A0),
            accent = accent,
            isDark = false,
        )
    }

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = AccentCyan,
            background = nova.background,
            onBackground = nova.textPrimary,
            surface = nova.surface,
            onSurface = nova.textPrimary,
            surfaceVariant = nova.surfaceElevated,
            onSurfaceVariant = nova.textSecondary,
            outline = nova.outline,
            error = AccentPink,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = AccentCyan,
            background = nova.background,
            onBackground = nova.textPrimary,
            surface = nova.surface,
            onSurface = nova.textPrimary,
            surfaceVariant = nova.surfaceElevated,
            onSurfaceVariant = nova.textSecondary,
            outline = nova.outline,
            error = AccentPink,
        )
    }

    CompositionLocalProvider(LocalNovaColors provides nova) {
        MaterialTheme(colorScheme = scheme, typography = NovaTypography, content = content)
    }
}

val NovaTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.5.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
)
