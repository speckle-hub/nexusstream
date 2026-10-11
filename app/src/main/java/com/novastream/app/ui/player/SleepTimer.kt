package com.novastream.app.ui.player

/**
 * Player sleep-timer options (Phase 29).
 *
 * Pure-JVM helper with no Android dependencies, so the mapping and label logic is unit-tested
 * (`SleepTimerTest`). The Activity owns the actual countdown job; this file only answers "how
 * long" and "what to show".
 */
enum class SleepTimerOption(val minutes: Int?) {
    OFF(null),
    MIN_15(15),
    MIN_30(30),
    MIN_45(45),
    MIN_60(60),

    /**
     * Pause when the current episode/video ends instead of after a fixed delay. Doubles as
     * "end of video" for movies (anything with no episode list).
     */
    END_OF_EPISODE(null);

    /** True for the fixed-delay options that arm a countdown job. */
    val isTimed: Boolean get() = minutes != null
}

/**
 * Fixed delay in milliseconds, or null for [SleepTimerOption.OFF] and
 * [SleepTimerOption.END_OF_EPISODE] (which are event-driven, not time-driven).
 */
fun SleepTimerOption.delayMs(): Long? = minutes?.let { it * 60_000L }

/** Sheet row label, e.g. "30 minutes" / "End of episode" / "Off". */
fun SleepTimerOption.label(): String = when (this) {
    SleepTimerOption.OFF -> "Off"
    SleepTimerOption.END_OF_EPISODE -> "End of episode"
    else -> "$minutes minutes"
}

/**
 * Short status label for an armed timer, e.g. "Sleep 24m" / "Sleep: episode end", or null when
 * [option] is [SleepTimerOption.OFF]. [deadlineElapsedMs] is the `SystemClock.elapsedRealtime()`
 * deadline for timed options (null when not armed); [nowElapsedMs] is the current value on the
 * same clock, so callers can pass fakes in tests.
 */
fun sleepStatusLabel(
    option: SleepTimerOption,
    deadlineElapsedMs: Long?,
    nowElapsedMs: Long,
): String? {
    if (option == SleepTimerOption.OFF) return null
    if (option == SleepTimerOption.END_OF_EPISODE) return "Sleep: episode end"
    if (deadlineElapsedMs == null) return null
    val remainingMin = ((deadlineElapsedMs - nowElapsedMs) / 60_000L).coerceAtLeast(0)
    return "Sleep ${remainingMin}m"
}
