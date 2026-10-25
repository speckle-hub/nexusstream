package com.moovio.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focus.FocusRequester
import androidx.compose.foundation.focus.focusRequester
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Icons
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.NavigationDestination
import com.moovio.desktop.ui.nav.NavController
import com.moovio.desktop.player.MpvSurface
import com.moovio.desktop.player.PlayerController
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.AccentViolet
import com.moovio.desktop.ui.theme.MoovioSurface
import com.moovio.desktop.ui.theme.MoovioTextPrimary
import com.moovio.desktop.ui.theme.MoovioTextSecondary
import com.moovio.desktop.ui.theme.moovioColors
import kotlinx.coroutines.delay

/**
 * Player: libmpv surface + control overlay with keyboard shortcuts
 * (Space play/pause, arrows seek, F fullscreen, Esc exit fullscreen).
 */
@Composable
fun PlayerScreen(
    item: MediaItem,
    player: com.moovio.desktop.player.MpvPlayer,
    controller: PlayerController,
    nav: NavController,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var autoHideJob: kotlinx.coroutines.Job? = null
    val scope = rememberCoroutineScope()

    fun scheduleAutoHide() {
        autoHideJob?.cancel()
        if (controller.isPlaying.value) {
            autoHideJob = scope.launch {
                delay(3000)
                controlsVisible = false
            }
        }
    }

    // Keyboard shortcuts
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyPress) return@onKeyEvent false
                when (event.key) {
                    Key.Space -> { controller.togglePlayback(); scheduleAutoHide(); true }
                    Key.ArrowLeft -> { controller.seekRelative(-10_000); scheduleAutoHide(); true }
                    Key.ArrowRight -> { controller.seekRelative(10_000); scheduleAutoHide(); true }
                    Key.ArrowUp -> { controller.setVolume((controller.volume.value + 5f).coerceAtMost(100f)); true }
                    Key.ArrowDown -> { controller.setVolume((controller.volume.value - 5f).coerceAtLeast(0f)); true }
                    Key.F -> { controller.setFullscreen(!controller.isFullscreen.value); true }
                    Key.Escape -> {
                        if (controller.isFullscreen.value) controller.setFullscreen(false)
                        true
                    }
                    else -> false
                }
            },
    ) {
        MpvSurface(
            player = player,
            modifier = Modifier.fillMaxSize(),
        )

        // Controls overlay — fades out automatically during playback.
        androidx.compose.animation.AnimatedVisibility(
            visible = controlsVisible,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.l),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { nav.back() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MoovioTextPrimary)
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.h4,
                        color = MoovioTextPrimary,
                        modifier = Modifier.padding(start = AppSpacing.m).weight(1f),
                    )
                    IconButton(onClick = { controller.setFullscreen(!controller.isFullscreen.value) }) {
                        Icon(
                            imageVector = if (controller.isFullscreen.value) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = MoovioTextPrimary,
                        )
                    }
                }

                // Progress
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatMs(controller.positionMs.value),
                        style = MaterialTheme.typography.body2,
                        color = MoovioTextSecondary,
                        modifier = Modifier.width(40.dp),
                    )
                    Slider(
                        value = controller.positionMs.value.toFloat(),
                        onValueChange = { controller.seek(it.toLong()) },
                        valueRange = 0f..(controller.durationMs.value.coerceAtLeast(1).toFloat()),
                        modifier = Modifier.weight(1f).padding(horizontal = AppSpacing.m),
                    )
                    Text(
                        text = formatMs(controller.durationMs.value),
                        style = MaterialTheme.typography.body2,
                        color = MoovioTextSecondary,
                        modifier = Modifier.width(40.dp),
                    )
                }

                // Bottom controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { controller.seekRelative(-10_000) }) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = MoovioTextPrimary)
                    }
                    IconButton(
                        onClick = {
                            if (controller.isPlaying.value) controller.pause() else controller.play()
                            scheduleAutoHide()
                        },
                    ) {
                        Icon(
                            imageVector = if (controller.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (controller.isPlaying.value) "Pause" else "Play",
                            tint = MoovioTextPrimary,
                        )
                    }
                    IconButton(onClick = { controller.seekRelative(10_000) }) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = MoovioTextPrimary)
                    }
                    TextButton(onClick = { controller.toggleSheet() }) {
                        Text(text = "Tracks", style = MaterialTheme.typography.subtitle1, color = moovioColors.accent)
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${String.format("%02d", seconds)}"
}


