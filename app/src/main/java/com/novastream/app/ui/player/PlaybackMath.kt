package com.novastream.app.ui.player

/**
 * Small pure helpers behind the Phase 15 player features. Kept Android-free so the timing logic
 * that drives the subtitle-sync overlay and the Stats-for-Nerds readout can be unit-tested.
 */

/** Lower/upper bound for the subtitle sync offset (±10 s). */
const val SUBTITLE_SYNC_MAX_MS = 10_000

/** Clamp a raw subtitle sync value into the supported ±10 s range. */
fun clampSubtitleSyncMs(value: Int): Int = value.coerceIn(-SUBTITLE_SYNC_MAX_MS, SUBTITLE_SYNC_MAX_MS)

/** Human label for a subtitle sync offset: "Off", "+1.5 s" or "-2.0 s". */
fun formatSubtitleSync(ms: Int): String = when {
    ms == 0 -> "Off"
    ms > 0 -> "+%.1f s".format(ms / 1000.0)
    else -> "-%.1f s".format(-ms / 1000.0)
}

/**
 * Index of the last cue-set whose presentation time is at or before [targetMs], or `-1` when the
 * target precedes every recorded event.
 *
 * [times] must be non-decreasing (it is the recorded `onCues` timeline). The subtitle-sync overlay
 * uses this to pick which buffered cue set to show for `position - offset`: a positive offset shows
 * an earlier cue set (delayed captions), a negative one reaches toward buffered future cues.
 */
fun activeCueIndex(times: List<Long>, targetMs: Long): Int {
    var result = -1
    for (i in times.indices) {
        if (times[i] <= targetMs) result = i else break
    }
    return result
}

/** Compact bitrate readout for the Stats overlay: "N/A", "850 kbps" or "4.2 Mbps". */
fun formatBitrate(bps: Long): String = when {
    bps <= 0 -> "N/A"
    bps >= 1_000_000 -> "%.1f Mbps".format(bps / 1_000_000.0)
    else -> "${bps / 1000} kbps"
}

/** FPS readout, or blank when the container did not declare one. */
fun formatFps(frameRate: Float): String =
    if (frameRate > 0f) "%.0f fps".format(frameRate) else ""
