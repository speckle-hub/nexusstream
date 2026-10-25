package com.moovio.desktop.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moovio.desktop.domain.model.NavigationDestination
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.MoovioOutline
import com.moovio.desktop.ui.theme.MoovioSurface
import com.moovio.desktop.ui.theme.MoovioTextSecondary
import com.moovio.desktop.ui.theme.moovioColors

private val SIDEBAR_ITEMS = listOf(
    SidebarItem(NavigationDestination.Home, "Home", Icons.Home),
    SidebarItem(NavigationDestination.Movies, "Movies", Icons.Movie),
    SidebarItem(NavigationDestination.TV, "TV", Icons.Television),
    SidebarItem(NavigationDestination.Anime, "Anime", Icons.PlayCircleOutline),
    SidebarItem(NavigationDestination.Manga, "Manga", Icons.Book),
    SidebarItem(NavigationDestination.Library, "Library", Icons.Folder),
    SidebarItem(NavigationDestination.Settings, "Settings", Icons.Settings),
    SidebarItem(NavigationDestination.Nsfw, "NSFW", Icons.Lock, isAdult = true),
)

@Composable
fun Sidebar(
    selected: NavigationDestination,
    onItemClick: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(AppSpacing.sidebarWidth)
            .fillMaxHeight()
            .background(MoovioSurface),
    ) {
        // Brand mark
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = AppSpacing.l),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "M",
                style = MaterialTheme.typography.h1,
                color = moovioColors.accent,
            )
        }
        Spacer(modifier = Modifier.height(AppSpacing.xxl))

        SIDEBAR_ITEMS.forEach { item ->
            SidebarButton(
                item = item,
                selected = selected == item.destination,
                onClick = { onItemClick(item.destination) },
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.xxl).weight(1f))

        Text(
            text = "Moovio 0.1.0",
            style = MaterialTheme.typography.caption,
            color = MoovioTextSecondary,
            modifier = Modifier.padding(horizontal = AppSpacing.l),
        )
    }
}


