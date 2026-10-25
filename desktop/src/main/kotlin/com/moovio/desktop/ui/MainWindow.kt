package com.moovio.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.NavigationDestination
import com.moovio.desktop.player.LibMpvPlayer
import com.moovio.desktop.player.MpvPlayer
import com.moovio.desktop.player.PlayerController
import com.moovio.desktop.ui.nav.AppBar
import com.moovio.desktop.ui.nav.AppBarHeight
import com.moovio.desktop.ui.nav.NavController
import com.moovio.desktop.ui.nav.Sidebar
import com.moovio.desktop.ui.screens.DetailScreen
import com.moovio.desktop.ui.screens.HomeScreen
import com.moovio.desktop.ui.screens.LibraryScreen
import com.moovio.desktop.ui.screens.NsfwLockScreen
import com.moovio.desktop.ui.screens.PlayerScreen
import com.moovio.desktop.ui.screens.SearchScreen
import com.moovio.desktop.ui.screens.SettingsScreen
import com.moovio.desktop.ui.theme.AppDuration
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.MoovioBackground
import com.moovio.desktop.ui.theme.MoovioOutline

@Composable
fun MainWindow(container: AppContainer) {
    val nav = remember { NavController() }
    val player: MpvPlayer = LibMpvPlayer()
    val controller = remember { PlayerController(player) }
    var nsfwGateOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MoovioBackground)) {
        AppBar(
            onMenuClick = {},
            onSearch = { nav.navigate(NavigationDestination.Search) },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MoovioBackground),
        ) {
            Sidebar(
                selected = nav.current.value,
                onItemClick = { destination ->
                    when (destination) {
                        NavigationDestination.Nsfw -> {
                            if (container.settings.isNsfwUnlocked()) {
                                nsfwGateOpen = true
                                nav.navigate(destination)
                            }
                        }
                        else -> {
                            nsfwGateOpen = false
                            nav.navigate(destination)
                        }
                    }
                },
            )
            Divider(modifier = Modifier.fillMaxHeight())
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = nav.current.value,
                    modifier = Modifier.fillMaxSize(),
                    label = { it.toString() },
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(durationMillis = AppDuration.normal)) +
                        fadeOut(animationSpec = tween(durationMillis = AppDuration.normal)))
                    },
                ) { destination ->
                    when (destination) {
                        NavigationDestination.Home -> HomeScreen(container, nav)
                        NavigationDestination.Movies, NavigationDestination.TV,
                        NavigationDestination.Anime, NavigationDestination.Manga -> {
                            val label = when (destination) {
                                NavigationDestination.Movies -> "Movies"
                                NavigationDestination.TV -> "TV"
                                NavigationDestination.Anime -> "Anime"
                                NavigationDestination.Manga -> "Manga"
                                else -> "Catalog"
                            }
                            CatalogPlaceholder(label, container, nav)
                        }
                        NavigationDestination.Search -> SearchScreen(container, nav)
                        NavigationDestination.Library -> LibraryScreen(container, nav)
                        NavigationDestination.Settings -> SettingsScreen(container)
                        NavigationDestination.Nsfw -> if (nsfwGateOpen) {
                            NsfwPlaceholder()
                        } else {
                            NsfwLockScreen(container) {
                                nsfwGateOpen = true
                                nav.navigate(NavigationDestination.Nsfw)
                            }
                        }
                        is NavigationDestination.Detail -> DetailScreen(it.mediaId, it.title, container, nav)
                        NavigationDestination.Player -> PlayerScreen(
                            item = MediaItem("playing", MediaType.MOVIE, "Now playing"),
                            player = player,
                            controller = controller,
                            nav = nav,
                        )
                    }
                }
            }
        }
    }
}

/** Temporary placeholder for type tabs until per-type catalogs are fully wired. */
@Composable
private fun CatalogPlaceholder(
    label: String,
    container: AppContainer,
    nav: NavController,
) {
    Column(
        modifier = ColumnScope.fillMaxSize().padding(AppSpacing.xxl),
    ) {
        Text(text = label, style = MaterialTheme.typography.h3,
            color = MaterialTheme.colorScheme.onSurface)
        Text(text = "Catalog rows for $label load from installed Stremio add-ons.",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.moovioColors.textSecondary)
    }
}

@Composable
private fun NsfwPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(AppSpacing.xxl),
        horizontalAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(text = "NSFW", style = MaterialTheme.typography.h3, color = MaterialTheme.colorScheme.onSurface)
        Text(text = "PIN-gated section — hidden from home and search.",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.moovioColors.textSecondary)
    }
}


