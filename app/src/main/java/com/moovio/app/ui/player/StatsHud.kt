package com.moovio.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.common.util.UnstableApi
import com.moovio.app.ui.theme.AppRadius
import com.moovio.app.ui.theme.LocalMoovioColors
import com.moovio.app.ui.theme.tnum
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** 40 samples at the 250 ms tick = the last 10 seconds of stream history. */
private const val HISTORY_SAMPLES = 40

/** Vertical scale of the sparkline: a full-height peak means this much buffer ahead. */
private const val BUFFER_SCALE_MS = 24_000L

/**
 * One reading of the stream. Immutable so a tick allocates one object and nothing else.
 *
 * [history] is normalised 0..1 (buffer ahead ÷ [BUFFER_SCALE_MS]) rather than milliseconds, because
 * the sparkline draws it directly and the numbers are shown separately.
 */
@Immutable
private data class StreamReading(
    val bufferAheadMs: Long,
    val bufferedPct: Int,
    val bitrateBps: Long,
    val bandwidthBps: Long,
    val width: Int,
    val height: Int,
    val fps: Float,
    val videoCodec: String?,
    val audioCodec: String?,
    val channels: Int,
    val dropped: Long,
    val droppedPerSecond: Float,
    val state: Int,
    val history: List<Float>,
)

/**
 * "Stats for nerds", drawn instead of listed.
 *
 * The old panel was eight label/value rows of text that grew every time a metric mattered, and every
 * one of its numbers was hoisted into the player screen's own state — so a 250 ms tick recomposed the
 * whole control overlay (seek bar, buttons, subtitle layer) just to refresh a buffer percentage.
 *
 * This version owns its own ticker and its own state: nothing outside this composable recomposes when
 * a reading lands, and the parent now needs to hold no stats state at all (the dropped-frame
 * analytics listener and the four hoisted counters are gone). The buffer trend is drawn as a
 * sparkline because "is the stream keeping up" is a *shape* question — a number can't show a buffer
 * that is sawing up and down.
 *
 * Deliberately quiet: hairline grid, one accent line, tabular figures in the app's own `labelSmall`.
 * The only colour is the accent, and only on the line that carries meaning.
 */
@UnstableApi
@Composable
fun StatsHud(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    val moovio = LocalMoovioColors.current
    // The bandwidth estimate only exists on the analytics listener. It is subscribed here, inside the
    // HUD, so the value it drives can never recompose anything outside this composable.
    var bandwidthBps by remember { mutableLongStateOf(0L) }
    DisposableEffect(player) {
        val listener = object : AnalyticsListener {
            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long,
            ) {
                bandwidthBps = bitrateEstimate
            }
        }
        player.addAnalyticsListener(listener)
        onDispose { player.removeAnalyticsListener(listener) }
    }
    val initial = remember { reading(player, emptyList(), 0f, 0L) }
    var stats by remember { mutableStateOf(initial) }

    // The ticker lives here, not in the screen: a recomposition caused by a stats update can then
    // only ever touch this subtree.
    LaunchedEffect(player) {
        val history = ArrayDeque<Float>(HISTORY_SAMPLES)
        var lastDropped = -1L
        var rate = 0f
        while (isActive) {
            val ahead = player.totalBufferedDuration.coerceAtLeast(0L)
            history.addLast((ahead.toFloat() / BUFFER_SCALE_MS).coerceIn(0f, 1f))
            while (history.size > HISTORY_SAMPLES) history.removeFirst()

            val counters = player.videoDecoderCounters
            val dropped = counters?.droppedBufferCount?.toLong() ?: 0L
            if (lastDropped >= 0) {
                // Sampling at a fixed 250 ms, so the delta is frames per quarter second.
                rate = (dropped - lastDropped).coerceAtLeast(0L) * 4f
            }
            lastDropped = dropped

            stats = reading(player, history.toList(), rate, bandwidthBps)
            delay(250)
        }
    }

    val shape = RoundedCornerShape(AppRadius.panel.dp)
    Column(
        modifier = modifier
            .width(232.dp)
            .background(moovio.surface.copy(alpha = 0.92f), shape)
            .border(1.dp, moovio.hairline, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Stream",
                style = MaterialTheme.typography.labelSmall,
                color = moovio.textSecondary,
            )
            Box(Modifier.weight(1f))
            Text(
                text = stateLabel(stats.state, stats.droppedPerSecond, stats.history),
                style = MaterialTheme.typography.labelSmall,
                color = moovio.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Buffer ahead over the last 10 s. One accent line, hairline grid, no animation: the shape
        // is the information, so it updates by redrawing, not by tweening.
        Canvas(Modifier.fillMaxWidth().height(52.dp)) {
            val midY = size.height / 2f
            drawLine(moovio.hairline, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1f)

            val points = stats.history
            if (points.size > 1) {
                val stepX = size.width / (HISTORY_SAMPLES - 1).toFloat()
                val path = Path()
                points.forEachIndexed { index, value ->
                    val x = size.width - (points.size - 1 - index) * stepX
                    val y = size.height - value * size.height
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = moovio.accent,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        Text(
            text = "Buffer ahead ${seconds(stats.bufferAheadMs)} · ${stats.bufferedPct}%",
            style = MaterialTheme.typography.labelSmall.tnum,
            color = moovio.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${bitrate(stats.bitrateBps)} · ${bitrate(stats.bandwidthBps)} down",
            style = MaterialTheme.typography.labelSmall.tnum,
            color = moovio.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${stats.width}×${stats.height} · ${"%.0f".format(stats.fps)} fps · dropped ${stats.dropped}",
            style = MaterialTheme.typography.labelSmall.tnum,
            color = moovio.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listOfNotNull(stats.videoCodec, stats.audioCodec).joinToString(" · ").ifBlank { "Codec pending" },
            style = MaterialTheme.typography.labelSmall,
            color = moovio.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Snapshot of the player. Best-effort: every field degrades to a dash-friendly zero. */
@UnstableApi
private fun reading(
    player: ExoPlayer,
    history: List<Float>,
    droppedPerSecond: Float,
    bandwidthBps: Long,
): StreamReading {
    val video: Format? = runCatching { player.videoFormat }.getOrNull()
    val audio: Format? = runCatching { player.audioFormat }.getOrNull()
    val size = runCatching { player.videoSize }.getOrNull()
    val counters = runCatching { player.videoDecoderCounters }.getOrNull()
    return StreamReading(
        bufferAheadMs = player.totalBufferedDuration.coerceAtLeast(0L),
        bufferedPct = player.bufferedPercentage.coerceIn(0, 100),
        bitrateBps = video?.bitrate?.toLong()?.takeIf { it > 0 } ?: 0L,
        bandwidthBps = bandwidthBps,
        width = video?.width?.takeIf { it > 0 } ?: (size?.width ?: 0),
        height = video?.height?.takeIf { it > 0 } ?: (size?.height ?: 0),
        fps = video?.frameRate?.takeIf { it > 0f } ?: 0f,
        videoCodec = codecLabel(video),
        audioCodec = audio?.let { format ->
            val mime = format.sampleMimeType?.substringAfterLast('/')?.uppercase().orEmpty()
            val channels = format.channelCount.takeIf { it > 0 }?.let { " ${it}ch" }.orEmpty()
            (mime + channels).ifBlank { null }
        },
        channels = audio?.channelCount ?: 0,
        dropped = counters?.droppedBufferCount?.toLong() ?: 0L,
        droppedPerSecond = droppedPerSecond,
        state = player.playbackState,
        history = history,
    )
}

/** Short, readable RFC 6381 codec string: `avc1.640028` reads worse than `h264 · High`. */
private fun codecLabel(format: Format?): String? {
    val mime = format?.sampleMimeType ?: return format?.codecs?.takeIf { it.isNotBlank() }
    val family = when {
        mime.contains("avc") -> "H.264"
        mime.contains("hevc") -> "H.265"
        mime.contains("av01") -> "AV1"
        mime.contains("vp9") -> "VP9"
        mime.contains("vp8") -> "VP8"
        else -> mime.substringAfterLast('/').uppercase()
    }
    val profile = format.codecs?.takeIf { it.isNotBlank() }
    return if (profile != null) "$family · $profile" else family
}

/**
 * One word for "is the stream keeping up".
 *
 * Frames are the honest signal: a healthy buffer with a dropping frame counter still means the decoder
 * cannot keep pace, which is exactly what someone opening this panel is trying to find out.
 */
private fun stateLabel(state: Int, droppedPerSecond: Float, history: List<Float>): String = when {
    state == Player.STATE_BUFFERING -> "Buffering"
    state == Player.STATE_ENDED -> "Ended"
    droppedPerSecond > 1.5f -> "Dropping frames"
    history.size >= 8 && history.last() + 0.08f < history[history.size - 8] -> "Draining"
    history.size >= 8 && history.last() > history[history.size - 8] + 0.08f -> "Filling"
    else -> "Stable"
}

private fun seconds(ms: Long): String = "%.1f s".format(ms / 1000.0)

private fun bitrate(bps: Long): String = when {
    bps <= 0L -> "—"
    bps >= 1_000_000L -> "%.1f Mbps".format(bps / 1_000_000.0)
    else -> "%d kbps".format(bps / 1000L)
}
