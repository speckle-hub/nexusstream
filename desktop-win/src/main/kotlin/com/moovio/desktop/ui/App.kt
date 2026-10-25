package com.moovio.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.ui.components.EmptyState
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.nav.Screen
import com.moovio.desktop.ui.nav.Sidebar
import com.moovio.desktop.ui.screens.browse.CatalogScreen
import com.moovio.desktop.ui.screens.detail.DetailScreen
import com.moovio.desktop.ui.screens.home.HomeScreen
import com.moovio.desktop.ui.screens.library.LibraryScreen
import com.moovio.desktop.ui.screens.manga.MangaScreen
import com.moovio.desktop.ui.screens.nsfw.NsfwScreen
import com.moovio.desktop.ui.screens.player.PlayerScreen
import com.moovio.desktop.ui.screens.search.SearchOverlay
import com.moovio.desktop.ui.screens.settings.SettingsScreen
import com.moovio.desktop.ui.theme.MoovioTheme
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.glass
import com.moovio.desktop.domain.model.MediaType
import kotlinx.coroutines.delay

@Composable
fun MoovioApp(container: AppContainer, windowState: WindowState) {
    val app = remember(container) { AppState(container) }
    val settings by container.settings.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    MoovioTheme(accentId = settings.accent, reduceMotion = settings.reduceMotion) {
        LaunchedEffect(app.fullscreen) {
            windowState.placement = if (app.fullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
        }
        LaunchedEffect(Unit) {
            delay(250)
            runCatching { focusRequester.requestFocus() }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Palette.background)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.isCtrlPressed && event.key == Key.K -> {
                            if (app.searchOpen) app.closeSearch() else app.openSearch()
                            true
                        }
                        app.playTarget != null -> false
                        event.key == Key.Escape -> app.back()
                        event.isCtrlPressed && event.key == Key.One -> {
                            app.navigateTab(Screen.Home); true
                        }
                        event.isCtrlPressed && event.key == Key.Two -> {
                            app.navigateTab(Screen.Movies); true
                        }
                        event.isCtrlPressed && event.key == Key.Three -> {
                            app.navigateTab(Screen.Series); true
                        }
                        event.isCtrlPressed && event.key == Key.Four -> {
                            app.navigateTab(Screen.Anime); true
                        }
                        event.isCtrlPressed && event.key == Key.Five -> {
                            app.navigateTab(Screen.Manga); true
                        }
                        event.isCtrlPressed && event.key == Key.Six -> {
                            app.navigateTab(Screen.Library); true
                        }
                        else -> false
                    }
                }
                .focusRequester(focusRequester)
                .focusable(),
        ) {
            Row(Modifier.fillMaxSize()) {
                Sidebar(app, container)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Palette.background),
                ) {
                    AnimatedContent(
                        targetState = app.screen,
                        transitionSpec = {
                            fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 28 } togetherWith
                                fadeOut(tween(140))
                        },
                        label = "screen",
                        modifier = Modifier.fillMaxSize(),
                    ) { screen ->
                        when (screen) {
                            Screen.Home -> HomeScreen(app, container, onOpen = { app.openDetail(it) })

                            Screen.Movies -> CatalogScreen(
                                app,
                                container,
                                MediaType.MOVIE,
                                "Movies",
                                "Blockbusters, classics and everything in between",
                            )

                            Screen.Series -> CatalogScreen(
                                app,
                                container,
                                MediaType.SERIES,
                                "TV Shows",
                                "Prestige drama, comedy and everything episodic",
                            )

                            Screen.Anime -> CatalogScreen(
                                app,
                                container,
                                MediaType.ANIME,
                                "Anime",
                                "Seasonal picks and all-time favourites, powered by AniList",
                            )

                            Screen.Manga -> MangaScreen(app)

                            Screen.Nsfw -> NsfwScreen(app, container, onOpen = { app.openDetail(it) })

                            Screen.Library -> LibraryScreen(app, container, onOpen = { app.openDetail(it) })

                            Screen.Settings -> SettingsScreen(app, container)

                            is Screen.Detail -> DetailScreen(
                                app = app,
                                container = container,
                                item = screen.media,
                                onBack = { app.back() },
                            )
                        }
                    }
                }
            }

            // Global search
            AnimatedVisibility(
                visible = app.searchOpen,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(140)),
            ) {
                SearchOverlay(
                    app = app,
                    container = container,
                    onOpen = { media: MediaItem -> app.openDetail(media) },
                )
            }

            // Player owns the entire window (including the sidebar area).
            if (app.playTarget != null) {
                PlayerScreen(
                    app = app,
                    container = container,
                    onExit = { app.updateFullscreen(false) },
                )
            }

            // Toast
            AnimatedVisibility(
                visible = app.toast != null,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
            ) {
                Box(
                    Modifier
                        .glass(RoundedCornerShape(50), alpha = 0.10f)
                        .background(Palette.surfaceHigh.copy(alpha = 0.92f), RoundedCornerShape(50))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(
                        app.toast.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
