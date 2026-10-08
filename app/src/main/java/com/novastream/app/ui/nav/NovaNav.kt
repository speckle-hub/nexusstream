package com.novastream.app.ui.nav

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
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
import com.novastream.app.ui.screens.browse.BrowseScreen
import com.novastream.app.ui.screens.settings.HomeOrderScreen
import com.novastream.app.ui.screens.sources.SourcesScreen
import com.novastream.app.ui.screens.collection.CollectionScreen
import com.novastream.app.ui.screens.detail.DetailScreen
import com.novastream.app.ui.screens.home.HomeScreen
import com.novastream.app.ui.screens.library.LibraryScreen
import com.novastream.app.ui.screens.manga.MangaScreen
import com.novastream.app.ui.screens.nsfw.NsfwHubScreen
import com.novastream.app.ui.screens.search.SearchScreen
import com.novastream.app.ui.screens.settings.SettingsScreen
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.Motion
import com.novastream.app.ui.vm.LocalContainer

object Routes {
    const val HOME = "home"
    const val MANGA = "manga"
    const val NSFW = "nsfw"
    const val BROWSE = "browse"
    const val SEARCH = "search"
    const val SEARCH_PATTERN = "search?q={q}"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val ADDONS = "addons"
    const val SOURCES = "sources"
    const val HOME_ORDER = "home_order"
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

// Five tabs, one cohesive Material Filled icon set.
//
// Manga used to be a permanent tab of its own, making the bar six items wide — crowded enough to
// truncate labels. It is now one "Browse" destination with a segmented control (see BrowseScreen,
// which also carries Anime in place of the old NSFW segment). `Routes.MANGA` remains registered so
// that screen is still reachable by route; `Routes.NSFW` is now the isolated, route-only NSFW Hub.
// The persisted tab index is versioned (SettingsStore.CURRENT_TAB_LAYOUT_VERSION) so old six-tab
// indices are discarded rather than silently resolving to a different destination.
private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.SEARCH, "Search", Icons.Filled.Search),
    Tab(Routes.BROWSE, "Browse", Icons.Filled.AutoStories),
    Tab(Routes.LIBRARY, "Library", Icons.Filled.VideoLibrary),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

/**
 * Route pattern -> comparable base, ignoring any query/argument tail.
 *
 * The Search destination is registered as `search?q={q}` (it carries an optional pre-fill query),
 * while its tab is `search`. Comparing the raw `destination.route` therefore never matched, which
 * hid the bottom bar on the Search screen and stopped the tab from highlighting or persisting.
 * Comparing the part before `?` makes the two equivalent while leaving argument-bearing routes
 * (`collection/{type}`, `detail/…`) untouched.
 */
private fun String.routeBase(): String = substringBefore('?')

/** The bottom-bar tab a destination belongs to, or null when it isn't a tab destination. */
private fun tabForRoute(route: String?): Tab? =
    route?.let { r -> tabs.firstOrNull { it.route.routeBase() == r.routeBase() } }

/**
 * Tab-switch motion.
 *
 * Tabs used to swap instantly, which made the app feel like a set of disconnected pages. They now
 * cross-fade with a small horizontal drift, so the switch reads as one surface changing rather than
 * a cut. The direction follows tab order (moving right slides in from the right) so the gesture
 * feels spatial.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter(direction: Int): EnterTransition =
    fadeIn(animationSpec = tween(Motion.MEDIUM)) +
        slideInHorizontally(animationSpec = tween(Motion.MEDIUM)) { width -> direction * width / 12 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit(direction: Int): ExitTransition =
    fadeOut(animationSpec = tween(Motion.FAST)) +
        slideOutHorizontally(animationSpec = tween(Motion.FAST)) { width -> -direction * width / 12 }

/** Push-style motion for the full-screen detail/collection destinations. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailEnter(): EnterTransition =
    fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM)) { it / 8 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailExit(): ExitTransition =
    fadeOut(tween(Motion.FAST)) + slideOutVertically(tween(Motion.FAST)) { it / 8 }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NovaApp(navController: NavHostController = rememberNavController()) {
    val nova = LocalNovaColors.current
    val container = LocalContainer.current
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = tabForRoute(currentRoute) != null

    // Guards tab persistence until the one-shot cold-boot restore has finished, so restoring a
    // saved tab can never be undone by the initial Home route being persisted first.
    var restored by remember { mutableStateOf(false) }

    // Restore the last active tab **exactly once**, on cold boot. It used to key off
    // `currentRoute == HOME`, which meant tapping Home from Settings re-triggered it and bounced
    // the user straight back to the saved tab — the "Home glitches" bug.
    LaunchedEffect(Unit) {
        val current = navController.currentBackStackEntry?.destination?.route
        if (current == null || current == Routes.HOME) {
            val target = tabs.getOrNull(container.settings.lastTabSnapshot())?.route
            if (target != null && target != Routes.HOME) {
                navController.navigate(target) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        // Stamp the layout the persisted index now belongs to, so a future tab-list change is
        // detected the same way. Done after the read so a stale index is ignored exactly once.
        container.settings.stampTabLayoutVersion()
        restored = true
    }

    // Persist the active tab whenever it changes (after the restore above has settled).
    LaunchedEffect(currentRoute, restored) {
        if (!restored) return@LaunchedEffect
        val idx = tabs.indexOfFirst { it.route.routeBase() == currentRoute?.routeBase() }
        if (idx >= 0) container.settings.setLastTab(idx)
    }

    // Bottom padding every tab screen adds so its last row clears the floating bar. The bar is
    // permanently visible — it no longer hides itself on scroll.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarBottomPadding = if (showBar) FloatingNavBarHeight + 16.dp + navInset + 12.dp else 0.dp

    Box(Modifier.fillMaxSize().background(nova.background)) {
        CompositionLocalProvider(
            LocalFloatingNavBottomPadding provides navBarBottomPadding,
        ) {
            // Wraps the whole NavHost so a poster tapped on any screen can morph into the detail
            // header. Shared elements are matched by key across the two simultaneously-composed
            // destinations during a navigation transition.
            SharedTransitionLayout {
                // Captured from the layout's receiver: Compose 1.7.0 has no
                // `LocalSharedTransitionScope`, so it's captured once here and published per
                // destination below.
                val sharedTransitionScope = this
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // ---- Tabs: cross-fade + directional drift ---------------------------
                    composable(
                        Routes.HOME,
                        enterTransition = { tabEnter(1) },
                        exitTransition = { tabExit(1) },
                        popEnterTransition = { tabEnter(-1) },
                        popExitTransition = { tabExit(-1) },
                    ) { with(this) { ProvideSharedScope(sharedTransitionScope, this) { HomeScreen(navController) } } }
                    composable(
                        Routes.MANGA,
                        enterTransition = { tabEnter(1) },
                        exitTransition = { tabExit(1) },
                        popEnterTransition = { tabEnter(-1) },
                        popExitTransition = { tabExit(-1) },
                    ) { with(this) { ProvideSharedScope(sharedTransitionScope, this) { MangaScreen(navController) } } }
                    // Route-only: not in `tabs`, so the bottom bar stays hidden and the hub can
                    // never be reached by tabbing. Opened from Settings → Content Restrictions.
                    composable(
                        Routes.NSFW,
                        enterTransition = { detailEnter() },
                        exitTransition = { detailExit() },
                        popEnterTransition = { detailEnter() },
                        popExitTransition = { detailExit() },
                    ) { with(this) { ProvideSharedScope(sharedTransitionScope, this) { NsfwHubScreen(navController) } } }
                    composable(
                        Routes.BROWSE,
                        enterTransition = { tabEnter(1) },
                        exitTransition = { tabExit(1) },
                        popEnterTransition = { tabEnter(-1) },
                        popExitTransition = { tabExit(-1) },
                    ) { with(this) { ProvideSharedScope(sharedTransitionScope, this) { BrowseScreen(navController) } } }
                    composable(
                        Routes.SEARCH_PATTERN,
                        arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" }),
                        enterTransition = { tabEnter(1) },
                        exitTransition = { tabExit(1) },
                        popEnterTransition = { tabEnter(-1) },
                        popExitTransition = { tabExit(-1) },
                    ) { entry ->
                        with(this) { ProvideSharedScope(sharedTransitionScope, this) { SearchScreen(navController, initialQuery = entry.arguments?.getString("q").orEmpty()) } }
                    }
                    composable(
                        Routes.LIBRARY,
                        enterTransition = { tabEnter(1) },
                        exitTransition = { tabExit(1) },
                        popEnterTransition = { tabEnter(-1) },
                        popExitTransition = { tabExit(-1) },
                    ) { with(this) { ProvideSharedScope(sharedTransitionScope, this) { LibraryScreen(navController) } } }
                    composable(
                        Routes.SETTINGS,
                        enterTransition = { tabEnter(1) },
                        exitTransition = { tabExit(1) },
                        popEnterTransition = { tabEnter(-1) },
                        popExitTransition = { tabExit(-1) },
                    ) { with(this) { ProvideSharedScope(sharedTransitionScope, this) { SettingsScreen(navController) } } }

                    // ---- Pushed destinations: vertical push ----------------------------
                    composable(
                        Routes.ADDONS,
                        enterTransition = { detailEnter() },
                        exitTransition = { detailExit() },
                        popEnterTransition = { detailEnter() },
                        popExitTransition = { detailExit() },
                    ) { with(this) { ProvideSharedScope(sharedTransitionScope, this) { AddonManagerScreen(navController) } } }
                    composable(
                        Routes.SOURCES,
                        enterTransition = { detailEnter() },
                        exitTransition = { detailExit() },
                        popEnterTransition = { detailEnter() },
                        popExitTransition = { detailExit() },
                    ) { with(this) { ProvideSharedScope(sharedTransitionScope, this) { SourcesScreen(navController) } } }
                    composable(
                        Routes.HOME_ORDER,
                        enterTransition = { detailEnter() },
                        exitTransition = { detailExit() },
                        popEnterTransition = { detailEnter() },
                        popExitTransition = { detailExit() },
                    ) { with(this) { ProvideSharedScope(sharedTransitionScope, this) { HomeOrderScreen(navController) } } }
                    composable(
                        Routes.COLLECTION,
                        arguments = listOf(navArgument("type") { type = NavType.StringType }),
                        enterTransition = { detailEnter() },
                        exitTransition = { detailExit() },
                        popEnterTransition = { detailEnter() },
                        popExitTransition = { detailExit() },
                    ) { entry ->
                        val type = MediaType.fromId(entry.arguments?.getString("type")) ?: MediaType.MOVIE
                        with(this) { ProvideSharedScope(sharedTransitionScope, this) { CollectionScreen(navController, type) } }
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
                        enterTransition = { detailEnter() },
                        exitTransition = { detailExit() },
                        popEnterTransition = { detailEnter() },
                        popExitTransition = { detailExit() },
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
                        with(this) { ProvideSharedScope(sharedTransitionScope, this) { DetailScreen(navController, item) } }
                    }
                }
            }
        }

        // Floating pill navigation bar, overlaid at the bottom so lists scroll behind it. It stays
        // visible at all times (scroll-driven hiding was removed — see FloatingPillNavBar.kt).
        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(Motion.MEDIUM)) { it } + fadeIn(tween(Motion.SLOW)),
            exit = slideOutVertically(tween(Motion.MEDIUM)) { it } + fadeOut(tween(Motion.FAST)),
        ) {
            FloatingPillNavBar(
                items = tabs.map { FloatingNavItem(it.label, it.icon) },
                selectedIndex = tabs.indexOfFirst { it.route.routeBase() == currentRoute?.routeBase() }
                    .coerceAtLeast(0),
                onSelect = { index ->
                    val tab = tabs[index]
                    if (tab.route.routeBase() != currentRoute?.routeBase()) {
                        navController.navigate(tab.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}
