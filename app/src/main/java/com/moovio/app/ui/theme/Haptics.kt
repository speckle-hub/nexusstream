package com.moovio.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The app's haptic vocabulary: three cues, used consistently.
 *
 * Vibration used to be written inline at each call site (`TextHandleMove` here, `LongPress` there,
 * nothing at all in the player's brightness/volume drags), so the same physical gesture could feel
 * different depending on which screen you were on. Naming the three intensities keeps them honest:
 *
 * - [select] — a light tick for a discrete choice: a chip, a tab, a scrub passing a second.
 * - [toggle] — the same weight, for state that flips (a heart, a favourite).
 * - [commit] — the heavier buzz that means "something is about to happen", for a long press that
 *   opens a destructive confirmation.
 *
 * [HapticFeedback] is a plain interface, so this stays testable and [Stable] holds across
 * recompositions — important because the player composes these handlers once per gesture.
 */
@Stable
class MoovioHaptics(private val feedback: HapticFeedback) {

    fun select() {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun toggle() {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun commit() {
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberMoovioHaptics(): MoovioHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { MoovioHaptics(feedback) }
}
