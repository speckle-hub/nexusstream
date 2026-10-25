package com.moovio.desktop.domain.model

/**
 * Navigation targets of the desktop shell. The NSFW route is PIN-gated — see
 * [com.moovio.desktop.ui.screens.nsfw.NsfwLockScreen].
 */
sealed class NavigationDestination {
    object Home : NavigationDestination()
    object Movies : NavigationDestination()
    object TV : NavigationDestination()
    object Anime : NavigationDestination()
    object Manga : NavigationDestination()
    object Library : NavigationDestination()
    object Search : NavigationDestination()
    object Settings : NavigationDestination()

    data class Detail(val mediaId: String, val title: String) : NavigationDestination()
    object Nsfw : NavigationDestination()
    object Player : NavigationDestination()
}
