package com.moovio.desktop.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hovered
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moovio.desktop.domain.model.NavigationDestination
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.moovioColors

data class SidebarItem(
    val destination: NavigationDestination,
    val label: String,
    val icon: androidx.compose.material.ImageVector,
    val isAdult: Boolean = false,
)

@Composable
fun SidebarButton(
    item: SidebarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hovered by remember { mutableStateOf(false) }

    val contentColor = if (selected) moovioColors.accent else moovioColors.textSecondary
    val background = when {
        selected -> moovioColors.accent.copy(alpha = 0.10f)
        hovered -> Color(0x1BFFFFFF)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = AppSpacing.s)
            .background(background)
            .clickable { onClick() }
            .hovered { hovered = it },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .wrapContentSize(align = Alignment.Center),
                tint = contentColor,
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.subtitle1,
                color = contentColor,
                modifier = Modifier
                    .padding(horizontal = AppSpacing.m)
                    .weight(1f),
            )
        }
    }
}


