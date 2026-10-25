package com.moovio.desktop.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.ui.theme.AccentColor
import com.moovio.desktop.ui.theme.AccentSecondary
import com.moovio.desktop.ui.theme.Palette

@Composable
fun Sidebar(app: AppState, container: AppContainer) {
    val addons by container.addonStore.state.collectAsState()
    val enabled = addons.addons.count { it.enabled }

    Column(
        Modifier
            .width(232.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A11), Color(0xFF07070B)),
                ),
            )
            .padding(vertical = 22.dp),
    ) {
        Brand()
        Spacer(Modifier.height(30.dp))

        val main = listOf(
            NavEntry(Screen.Home, "Home", Icons.Rounded.Home),
            NavEntry(Screen.Movies, "Movies", Icons.Rounded.Movie),
            NavEntry(Screen.Series, "TV Shows", Icons.Rounded.Tv),
            NavEntry(Screen.Anime, "Anime", Icons.Rounded.Animation),
            NavEntry(Screen.Manga, "Manga", Icons.Rounded.AutoStories),
        )
        main.forEach { entry ->
            NavItem(
                entry = entry,
                active = app.screen.route == entry.screen.route,
                onClick = { app.navigateTab(entry.screen) },
            )
        }

        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.06f)),
        )
        Spacer(Modifier.height(16.dp))

        NavItem(
            NavEntry(Screen.Library, "Library", Icons.Rounded.VideoLibrary),
            active = app.screen.route == Screen.Library.route,
            onClick = { app.navigateTab(Screen.Library) },
        )
        NavItem(
            NavEntry(Screen.Nsfw, "NSFW", Icons.Rounded.Lock),
            active = app.screen.route == Screen.Nsfw.route,
            onClick = { app.navigateTab(Screen.Nsfw) },
            accent = Palette.warning,
        )

        Spacer(Modifier.weight(1f))

        Column(Modifier.padding(horizontal = 12.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.035f))
                    .padding(14.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Extension, null, Modifier.size(15.dp), tint = AccentSecondary)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "$enabled add-on${if (enabled == 1) "" else "s"} active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.textSecondary,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Stremio protocol ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.textTertiary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            NavItem(
                NavEntry(Screen.Settings, "Settings & Add-ons", Icons.Rounded.Settings),
                active = app.screen.route == Screen.Settings.route,
                onClick = { app.navigateTab(Screen.Settings) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Moovio Desktop · v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.textTertiary.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun Brand() {
    Row(
        Modifier.padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AccentColor, AccentColor.copy(alpha = 0.8f), AccentSecondary.copy(alpha = 0.8f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                null,
                Modifier.size(21.dp),
                tint = Color.White,
            )
        }
        Spacer(Modifier.width(11.dp))
        Column {
            Text(
                "MOOVIO",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
                color = Palette.textPrimary,
            )
            Text(
                "DESKTOP",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.6.sp,
                color = Palette.textTertiary,
                fontSize = 8.sp,
            )
        }
    }
}

private data class NavEntry(val screen: Screen, val label: String, val icon: ImageVector)

@Composable
private fun NavItem(
    entry: NavEntry,
    active: Boolean,
    onClick: () -> Unit,
    accent: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background by animateColorAsState(
        targetValue = when {
            active -> AccentColor.copy(alpha = 0.14f)
            hovered -> Color.White.copy(alpha = 0.05f)
            else -> Color.Transparent
        },
        animationSpec = tween(170),
        label = "navBackground",
    )
    val indicator by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(200),
        label = "navIndicator",
    )
    val tint = when {
        accent != null -> accent
        active -> AccentColor
        hovered -> Palette.textPrimary
        else -> Palette.textSecondary
    }

    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .hoverable(interaction)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height((16 * indicator).dp)
                .clip(RoundedCornerShape(50))
                .background(AccentColor),
        )
        Spacer(Modifier.width(12.dp))
        Icon(entry.icon, null, Modifier.size(18.dp), tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(
            entry.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) Palette.textPrimary else tint,
            modifier = Modifier.weight(1f),
        )
        if (entry.screen == Screen.Nsfw) {
            Text(
                "PIN",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.textTertiary,
                fontSize = 8.sp,
            )
        }
    }
}
