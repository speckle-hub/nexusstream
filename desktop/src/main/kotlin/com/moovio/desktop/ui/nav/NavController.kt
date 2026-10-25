package com.moovio.desktop.ui.nav

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.moovio.desktop.domain.model.NavigationDestination

/**
 * Simple state holder for shell navigation. Kept outside Compose state so it survives
 * recompositions; screens observe it via [current] (a StateFlow-compatible mutable state).
 */
class NavController {
    private val _current = mutableStateOf(NavigationDestination.Home as NavigationDestination)
    val current: MutableState<NavigationDestination> = _current

    fun navigate(to: NavigationDestination) {
        _current.value = to
    }

    fun openDetail(mediaId: String, title: String) {
        navigate(NavigationDestination.Detail(mediaId, title))
    }

    fun back() {
        when (val current = _current.value) {
            is NavigationDestination.Detail -> navigate(NavigationDestination.Home)
            NavigationDestination.Nsfw -> navigate(NavigationDestination.Home)
            else -> navigate(NavigationDestination.Home)
        }
    }
}
