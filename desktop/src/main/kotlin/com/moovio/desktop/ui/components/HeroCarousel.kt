package com.moovio.desktop.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Icons
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.ui.theme.AppDuration
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.AccentViolet
import com.moovio.desktop.ui.theme.MoovioSurface
import com.moovio.desktop.ui.theme.MoovioTextPrimary
import com.moovio.desktop.ui.theme.MoovioTextSecondary
import com.moovio.desktop.ui.theme.moovioColors
import kotlinx.coroutines.delay

/** Full-bleed hero carousel: auto-slides every 5s, with manual prev/next and dot indicators. */
@Composable
fun HeroCarousel(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(0) }
    var autoSlidePaused by remember { mutableStateOf(false) }

    // Auto-slide every 5 seconds while the user isn't interacting with the carousel.
    LaunchedEffect(selectedIndex, autoSlidePaused) {
        if (autoSlidePaused) return@LaunchedEffect
        while (true) {
            delay(5000)
            if (!autoSlidePaused) {
                selectedIndex = (selectedIndex + 1) % items.size
            }
        }
    }

    val offset by animateFloatAsState(
        targetValue = -selectedIndex * 1f,
        animationSpec = tween(durationMillis = AppDuration.normal),
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val backdrop = items[selectedIndex].backdrop
        if (backdrop.isNotBlank()) {
            RemoteImage(
                url = backdrop,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Hero backdrop",
                shape = CornerShape(0.dp),
                fallbackColor = MoovioSurface,
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    color = MoovioSurface.copy(alpha = 0.55f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.xxl),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start,
        ) {
            val current = items[selectedIndex]
            Text(
                text = current.title,
                style = MaterialTheme.typography.h1,
                color = MoovioTextPrimary,
                modifier = Modifier.padding(bottom = AppSpacing.s),
            )
            Text(
                text = current.subtitle(),
                style = MaterialTheme.typography.body1,
                color = MoovioTextSecondary,
                modifier = Modifier.padding(bottom = AppSpacing.l),
            )

            Row(
                modifier = Modifier.padding(bottom = AppSpacing.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { selectedIndex = (selectedIndex - 1 + items.size) % items.size },
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateBefore,
                        contentDescription = "Previous",
                        tint = MoovioTextPrimary,
                    )
                }
                IconButton(
                    onClick = { selectedIndex = (selectedIndex + 1) % items.size },
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateAfter,
                        contentDescription = "Next",
                        tint = MoovioTextPrimary,
                    )
                }
            }

            Row(
                modifier = Modifier.padding(bottom = AppSpacing.m),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                items.indices.forEach { index ->
                    val isActive = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isActive) moovioColors.accent
                                else MoovioTextSecondary.copy(alpha = 0.3f),
                                CircleShape,
                            )
                            .clickable { selectedIndex = index },
                    )
                }
            }
        }
    }
}


