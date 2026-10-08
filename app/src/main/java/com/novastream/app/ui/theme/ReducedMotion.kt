package com.novastream.app.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user has asked the system to reduce motion.
 *
 * Android has no direct "prefers-reduced-motion" API; the closest cross-version signal is the
 * system animation scale. When the user disables animations (Accessibility → Remove animations, or
 * a dev-options animation scale of 0), looping/decorative motion should stop. The Home hero's
 * auto-advance loop is the worst offender — it runs forever with no opt-out — and the staggered
 * row entrance is a close second.
 *
 * Read once per composition; the value is stable enough that live observation isn't worth the
 * ContentObserver plumbing.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { systemReducedMotion(context) }
}

/** Pure read of the system animation scale, defaulting to animations-on when unavailable. */
internal fun systemReducedMotion(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        1f,
    ) == 0f
}.getOrDefault(false)
