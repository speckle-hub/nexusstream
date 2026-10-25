package com.moovio.desktop.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextAlign
import androidx.compose.ui.unit.dp
import com.moovio.desktop.ui.theme.MoovioTheme

/**
 * Video surface. When libmpv is available this hosts the mpv render surface; until the native
 * libs are confirmed resolvable on this host it renders a premium placeholder so the shell and
 * navigation are fully runnable out of the box.
 */
@Composable
fun MpvSurface(
    player: MpvPlayer,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    val error = errorMessage or player.error.value
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoovioTheme.colors.surface)
            .border(BorderStroke(1.dp, MoovioTheme.colors.outline)),
        contentAlignment = Alignment.Center,
    ) {
        if (error != null) {
            Text(
                text = "Playback unavailable\n$error",
                color = MoovioTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Text(
                text = "libmpv video surface\nposition: ${player.positionMs.value} ms",
                color = MoovioTheme.colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

