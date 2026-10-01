package com.novastream.app.ui.nav

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.ui.screens.addons.AddonManagerScreen
import com.novastream.app.ui.screens.collection.CollectionScreen
import com.novastream.app.ui.screens.detail.DetailScreen
import com.novastream.app.ui.screens.home.HomeScreen
import com.novastream.app.ui.screens.library.LibraryScreen
import com.novastream.app.ui.screens.manga.MangaScreen
import com.novastream.app.ui.screens.nsfw.NsfwScreen
import com.novastream.app.ui.screens.search.SearchScreen
import com.novastream.app.ui.screens.settings.SettingsScreen
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.LocalContainer
import com.novastream.app.ui.vm.collectAsStateSafe

object Routes {
    const val HOME = "home"
    const val MANGA = "manga"
    const val NSFW = "nsfw"
    const val SEARCH = "search"
    const val SEARCH_PATTERN = "search?q={q}"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val ADDONS = "addons"
    const val COLLECTION = "collection/{type}"
    const val DETAIL = "detail/{type}/{addonId}/{id}/{title}/{poster}"

    fun collection(type: MediaType): String = "collection/${type.id}"

    fun detail(item: MediaItem): String {
        fun e(s: String?) = Uri.encode(s ?: "-")
        return "detail/${item.type.id}/${e(item.addonId)}/${e(item.id)}/${e(item.title)}/${e(item.poster)}"
    }

    /** Navigate to global search pre-filled with a query (used by clickable genre tags). */
    fun searchFor(query: String): String = "search?q=${Uri.encode(query)}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

// One cohesive Material Filled set for every bottom tab.
private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.MANGA, "Manga", Icons.Filled.AutoStories),
    Tab(Routes.NSFW, "NSFW", Icons.Filled.VisibilityOff),
    Tab(Routes.SEARCH, "Search", Icons.Filled.Search),
    Tab(Routes.LIBRARY, "Library", Icons.Filled.VideoLibrary),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun NovaApp(navController: NavHostController = rememberNavController()) {
    val nova = LocalNovaColors.current
    val container = LocalContainer.current
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }
    val savedTab by container.settings.lastTab.collectAsStateSafe()

    // Restore the last active tab on cold boot (only while we're still on Home).
    LaunchedEffect(savedTab, currentRoute) {
        val target = tabs.getOrNull(savedTab)?.route
        if (target != null && target != Routes.HOME && currentRoute == Routes.HOME) {
            navController.navigate(target) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Persist the active tab whenever it changes.
    LaunchedEffect(currentRoute) {
        val idx = tabs.indexOfFirst { it.route == currentRoute }
        if (idx >= 0) container.settings.setLastTab(idx)
    }

    Scaffold(
        containerColor = nova.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(180)),
            ) {
                NavigationBar(containerColor = nova.surface.copy(alpha = 0.96f)) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            // A slightly smaller icon keeps the active indicator pill compact.
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = nova.accent,
                                selectedTextColor = nova.accent,
                                indicatorColor = nova.accent.copy(alpha = 0.15f),
                                unselectedIconColor = nova.textTertiary,
                                unselectedTextColor = nova.textTertiary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().background(nova.background).padding(bottom = if (showBar) padding.calculateBottomPadding() else 0.dp)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.HOME) { HomeScreen(navController) }
                composable(Routes.MANGA) { MangaScreen(navController) }
                composable(Routes.NSFW) { NsfwScreen(navController) }
                composable(
                    Routes.SEARCH_PATTERN,
                    arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" }),
                ) { entry ->
                    SearchScreen(navController, initialQuery = entry.arguments?.getString("q").orEmpty())
                }
                composable(Routes.LIBRARY) { LibraryScreen(navController) }
                composable(Routes.SETTINGS) { SettingsScreen(navController) }
                composable(Routes.ADDONS) { AddonManagerScreen(navController) }
                composable(
                    Routes.COLLECTION,
                    arguments = listOf(navArgument("type") { type = NavType.StringType }),
                ) { entry ->
                    val type = MediaType.fromId(entry.arguments?.getString("type")) ?: MediaType.MOVIE
                    CollectionScreen(navController, type)
                }
                composable(
                    Routes.DETAIL,
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("addonId") { type = NavType.StringType },
                        navArgument("id") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                        navArgument("poster") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val type = entry.arguments?.getString("type") ?: "movie"
                    val addonId = entry.arguments?.getString("addonId")?.takeIf { it != "-" }
                    val id = entry.arguments?.getString("id") ?: ""
                    val title = entry.arguments?.getString("title")?.takeIf { it != "-" } ?: ""
                    val poster = entry.arguments?.getString("poster")?.takeIf { it != "-" }
                    val item = MediaItem(
                        id = id,
                        type = MediaType.fromId(type) ?: MediaType.MOVIE,
                        title = title,
                        poster = poster,
                        addonId = addonId,
                    )
                    DetailScreen(navController, item)
                }
            }
        }
    }
}
