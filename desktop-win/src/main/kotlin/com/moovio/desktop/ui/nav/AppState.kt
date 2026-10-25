package com.moovio.desktop.ui.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.domain.model.PlaybackRequest
import java.awt.Window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Screen {
    val route: String

    data object Home : Screen { override val route = "home" }
    data object Movies : Screen { override val route = "movies" }
    data object Series : Screen { override val route = "series" }
    data object Anime : Screen { override val route = "anime" }
    data object Manga : Screen { override val route = "manga" }
    data object Nsfw : Screen { override val route = "nsfw" }
    data object Library : Screen { override val route = "library" }
    data object Settings : Screen { override val route = "settings" }
    data class Detail(val media: MediaItem) : Screen {
        override val route = "detail:${media.key}"
    }
}

/** Survives navigation; owns the back stack, window chrome state, player and toasts. */
class AppState(val container: AppContainer) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set

    var searchOpen by mutableStateOf(false)
        private set

    var playRequest by mutableStateOf<PlaybackRequest?>(null)
        private set

    var playTarget by mutableStateOf<PlayTarget?>(null)
        private set

    var fullscreen by mutableStateOf(false)
        private set

    var window: Window? = null

    var toast by mutableStateOf<String?>(null)
        private set

    private val backStack = mutableStateListOf<Screen>()
    private var toastJob: Job? = null

    val canGoBack: Boolean get() = backStack.isNotEmpty()

    fun navigate(target: Screen) {
        if (target == screen) return
        backStack.add(screen)
        screen = target
    }

    /** Switching top-level tabs clears history so Back never ping-pongs between tabs. */
    fun navigateTab(target: Screen) {
        backStack.clear()
        screen = target
    }

    fun back(): Boolean {
        if (searchOpen) {
            searchOpen = false
            return true
        }
        if (playRequest != null || playTarget != null) {
            closePlayer()
            return true
        }
        if (backStack.isNotEmpty()) {
            screen = backStack.removeAt(backStack.lastIndex)
            return true
        }
        return false
    }

    fun openSearch() {
        searchOpen = true
    }

    fun closeSearch() {
        searchOpen = false
    }

    fun openDetail(media: MediaItem) {
        navigate(Screen.Detail(media))
    }

    fun requestPlay(target: PlayTarget) {
        playTarget = target
        searchOpen = false
    }

    fun startPlayback(request: PlaybackRequest) {
        playTarget = null
        playRequest = request
    }

    fun closePlayer() {
        playTarget = null
        playRequest = null
        fullscreen = false
    }

    fun updateFullscreen(enabled: Boolean) {
        fullscreen = enabled
    }

    fun showToast(message: String) {
        toast = message
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(3200)
            toast = null
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        toast = null
    }
}
