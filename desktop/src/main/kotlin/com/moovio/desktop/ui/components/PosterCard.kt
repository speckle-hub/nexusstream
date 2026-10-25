package com.moovio.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.MoovioSurface
import com.moovio.desktop.ui.theme.MoovioSurfaceElevated
import com.moovio.desktop.ui.theme.MoovioTextPrimary
import com.moovio.desktop.ui.theme.MoovioTextSecondary
import com.moovio.desktop.ui.theme.moovioColors

@Composable
fun PosterCard(
    item: MediaItem,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val imageSource = item.poster.takeIf { it.isNotBlank() }
    var hovered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val cardColor = when {
        selected -> moovioColors.accent.copy(alpha = 0.15f)
        hovered -> MoovioSurfaceElevated
        else -> MoovioSurface
    }

    Box(
        modifier = modifier
            .width(140.dp)
            .height(200.dp)
            .clickable(
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .hovered { hovered = it }
            .background(cardColor)
            .border(
                if (selected) 2.dp else 0.dp,
                moovioColors.accent,
                RoundedCornerShape(AppSpacing.cardRadius),
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (imageSource != null) {
            RemoteImage(
                url = imageSource,
                modifier = Modifier.fillMaxSize(),
                contentDescription = item.title,
                shape = RoundedCornerShape(AppSpacing.cardRadius),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xCC0A0B0F)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.subtitle1,
                color = MoovioTextPrimary,
                maxLines = 2,
                overflow = androidx.compose.foundation.layout.Overflow.Ellipsis,
            )
        }
    }
}


