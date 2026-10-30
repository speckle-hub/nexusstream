package com.moovio.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Extra design tokens exposed to the whole tree.
 *
 * Annotated [Immutable] because every field is a value class over ULong; without it Compose cannot
 * prove stability and any composable that reads `LocalMoovioColors.current` is treated as unstable,
 * which defeats skipping on the heavy lazy grids.
 */
@Immutable
data class MoovioColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val outline: Color,
    /** Divider/border colour: the primary text colour at 10%, so every hairline matches its theme. */
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    /** Solid accent surface (primary button, selected chip) guaranteed legible against [onAction]. */
    val actionFill: Color,
    /** Label/icon colour for content sitting on [actionFill] — white or near-black ink. */
    val onAction: Color,
    val isDark: Boolean,
)

private val DarkDefaults = MoovioColors(
    background = MoovioBackground,
    surface = MoovioSurface,
    surfaceElevated = MoovioSurfaceElevated,
    outline = MoovioOutline,
    hairline = MoovioTextPrimary.copy(alpha = 0.10f),
    textPrimary = MoovioTextPrimary,
    textSecondary = MoovioTextSecondary,
    textTertiary = MoovioTextTertiary,
    accent = AccentViolet,
    actionFill = AccentVioletDim,
    onAction = Color.White,
    isDark = true,
)

val LocalMoovioColors = staticCompositionLocalOf { DarkDefaults }

/** Toned-down semantic negatives, so an error never introduces a second saturated hue. */
private val ErrorDark = Color(0xFFE5484D)
private val ErrorLight = Color(0xFFDC2626)

@Composable
fun MoovioTheme(
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
    // Accents are user-selectable and some are too bright for white text, so the fill/content pair
    // is derived rather than assumed (see Color.actionFill).
    val (actionFill, onAction) = accent.actionFill()

    // True AMOLED pitch black: every dark surface drops to pure #000 so the panel is actually off,
    // with only the raised step lifting off it so sheets and menus still read as separate layers.
    val bg = when {
        !darkTheme -> LBackground
        amoledBlack -> MoovioAmoledBackground
        else -> MoovioBackground
    }
    val surface = when {
        !darkTheme -> LSurface
        amoledBlack -> MoovioAmoledSurface
        else -> MoovioSurface
    }
    val elevated = when {
        !darkTheme -> LSurfaceElevated
        amoledBlack -> MoovioAmoledElevated
        else -> MoovioSurfaceElevated
    }
    val moovio = if (darkTheme) {
        MoovioColors(
            background = bg,
            surface = surface,
            surfaceElevated = elevated,
            outline = MoovioOutline,
            hairline = MoovioTextPrimary.copy(alpha = 0.10f),
            textPrimary = MoovioTextPrimary,
            textSecondary = MoovioTextSecondary,
            textTertiary = MoovioTextTertiary,
            accent = accent,
            actionFill = actionFill,
            onAction = onAction,
            isDark = true,
        )
    } else {
        MoovioColors(
            background = LBackground,
            surface = LSurface,
            surfaceElevated = LSurfaceElevated,
            outline = Color(0xFFDDDDE6),
            hairline = LTextPrimary.copy(alpha = 0.10f),
            textPrimary = LTextPrimary,
            textSecondary = LTextSecondary,
            textTertiary = Color(0xFF6E6E85),
            accent = accent,
            actionFill = actionFill,
            onAction = onAction,
            isDark = false,
        )
    }

    // M3 surface-container roles (§2d #17). Material3 components (sheets, menus, the default
    // dialog/segmented surfaces) select one of these by elevation; leaving them unset means they
    // fall back to the light baseline and clash with the app's own surfaces. They ladder out of the
    // app's own `surface`/`surfaceElevated`, so a stock M3 surface sits in the same tonal family.
    //
    // primary/onPrimary come from the derived action pair, so a stock M3 Button or Switch is legible
    // for every accent instead of assuming white-on-violet works.
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = actionFill,
            onPrimary = onAction,
            primaryContainer = actionFill.copy(alpha = 0.22f),
            onPrimaryContainer = accent,
            secondary = moovio.textSecondary,
            onSecondary = bg,
            secondaryContainer = elevated,
            onSecondaryContainer = moovio.textPrimary,
            background = bg,
            onBackground = moovio.textPrimary,
            surface = surface,
            onSurface = moovio.textPrimary,
            surfaceVariant = elevated,
            onSurfaceVariant = moovio.textSecondary,
            outline = moovio.outline,
            error = ErrorDark,
            onError = Color.White,
            surfaceContainerLowest = bg,
            surfaceContainerLow = surface,
            surfaceContainer = elevated,
            surfaceContainerHigh = lerp(elevated, Color.White, 0.05f),
            surfaceContainerHighest = lerp(elevated, Color.White, 0.09f),
        )
    } else {
        lightColorScheme(
            primary = actionFill,
            onPrimary = onAction,
            primaryContainer = actionFill.copy(alpha = 0.14f),
            onPrimaryContainer = actionFill,
            secondary = moovio.textSecondary,
            onSecondary = moovio.surface,
            secondaryContainer = moovio.surfaceElevated,
            onSecondaryContainer = moovio.textPrimary,
            background = moovio.background,
            onBackground = moovio.textPrimary,
            surface = moovio.surface,
            onSurface = moovio.textPrimary,
            surfaceVariant = moovio.surfaceElevated,
            onSurfaceVariant = moovio.textSecondary,
            outline = moovio.outline,
            error = ErrorLight,
            onError = Color.White,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = moovio.surface,
            surfaceContainer = moovio.surfaceElevated,
            surfaceContainerHigh = lerp(moovio.surfaceElevated, Color.Black, 0.04f),
            surfaceContainerHighest = lerp(moovio.surfaceElevated, Color.Black, 0.08f),
        )
    }

    CompositionLocalProvider(LocalMoovioColors provides moovio) {
        MaterialTheme(colorScheme = scheme, typography = MoovioTypography, content = content)
    }
}

/**
 * The app's type scale.
 *
 * Hierarchy comes from size and colour, so nothing here is heavier than [FontWeight.Medium] — the
 * previous scale set every display and headline role to Bold(700) with negative tracking, which is
 * why section titles shouted as loudly as film names and why 20sp text had its letters squeezed.
 * Negative tracking is now reserved for true display sizes (32sp+), where optical tightening is
 * what a type designer would actually do.
 *
 * Sizes are a few steps from one scale, not a continuum: 11 / 12 / 13 / 14 / 15 / 17 / 19 / 22 /
 * 26 / 32 / 36.
 */
val MoovioTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.4).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.3).sp,
    ),
    // Wordmark on Home.
    displaySmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    // Detail-page title and the hero banner title.
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    // Row headers ("Trending now").
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        // ~1.45x — long synopses were cramped without an explicit line height.
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    ),
    // Metadata lines and quiet counts.
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    // Micro-labels. No wide tracking: they are sentence case now, not uppercased eyebrow text.
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)
