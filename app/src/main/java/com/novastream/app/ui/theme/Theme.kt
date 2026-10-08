package com.novastream.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
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
    dynamicColor: Boolean = false,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Material You (API 31+). Falls back to the static palette below on older devices.
    val dynamicScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }.getOrNull()
    } else null
    // With Material You on, the system's primary replaces the manual accent.
    val accent = dynamicScheme?.primary ?: accentFor(accentKey)
    val secondary = dynamicScheme?.secondary ?: AccentCyan

    // True AMOLED pitch black: swap every dark surface for true #000 where it matters most
    // (page background + cards that sit directly on it).
    val bg = when {
        !darkTheme -> LBackground
        amoledBlack -> Color(0xFF000000)
        else -> NsBackground
    }
    val surface = when {
        !darkTheme -> LSurface
        amoledBlack -> Color(0xFF000000)
        else -> NsSurface
    }
    val elevated = when {
        !darkTheme -> LSurfaceElevated
        amoledBlack -> Color(0xFF0B0B0B)
        else -> NsSurfaceElevated
    }
    val nova = if (darkTheme) {
        NovaColors(
            background = bg,
            surface = surface,
            surfaceElevated = elevated,
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
            textTertiary = Color(0xFF6E6E85),
            accent = accent,
            isDark = false,
        )
    }

    // M3 surface-container roles (§2d #17). Material3 components (sheets, menus, the default
    // dialog/segmented surfaces) select one of these by elevation; leaving them unset means they
    // fall back to the light baseline and clash with the app's own surfaces. They now ladder out of
    // the app's own `surface`/`surfaceElevated`, so a stock M3 surface sits in the same tonal family.
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = secondary,
            background = bg,
            onBackground = nova.textPrimary,
            surface = surface,
            onSurface = nova.textPrimary,
            surfaceVariant = elevated,
            onSurfaceVariant = nova.textSecondary,
            outline = nova.outline,
            error = AccentPink,
            surfaceContainerLowest = bg,
            surfaceContainerLow = surface,
            surfaceContainer = elevated,
            surfaceContainerHigh = lerp(elevated, Color.White, 0.05f),
            surfaceContainerHighest = lerp(elevated, Color.White, 0.09f),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = secondary,
            background = nova.background,
            onBackground = nova.textPrimary,
            surface = nova.surface,
            onSurface = nova.textPrimary,
            surfaceVariant = nova.surfaceElevated,
            onSurfaceVariant = nova.textSecondary,
            outline = nova.outline,
            error = AccentPink,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = nova.surface,
            surfaceContainer = nova.surfaceElevated,
            surfaceContainerHigh = lerp(nova.surfaceElevated, Color.Black, 0.04f),
            surfaceContainerHighest = lerp(nova.surfaceElevated, Color.Black, 0.08f),
        )
    }

    CompositionLocalProvider(LocalNovaColors provides nova) {
        MaterialTheme(colorScheme = scheme, typography = NovaTypography, content = content)
    }
}

/**
 * The app's type scale.
 *
 * This used to define only 8 of M3's roles and set no tracking or line height, so screens had to
 * bolt on `fontWeight = FontWeight.Bold` and `MaterialTheme.typography.labelSmall` silently fell
 * back to M3 defaults — the classic "this looks like a sample app" tell. Now every role is defined,
 * tracking is tuned per size (tight as type grows, wide on uppercase micro-labels) and body copy
 * gets an explicit line height so paragraphs breathe.
 */
val NovaTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        // ~1.45x — long synopses were cramped without an explicit line height.
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.15.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        // Uppercase micro-labels ("FEATURED") need positive tracking to read as designed.
        letterSpacing = 0.8.sp,
    ),
)
