package com.moovio.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.ui.MainWindow

/**
 * Desktop entry point for Moovio on Windows.
 *
 * Runs a single Compose window — no Android, no Electron. Video is rendered through libmpv
 * (see [com.moovio.desktop.player]).
 */
fun main() = application {
    val container = AppContainer()

    Window(
        title = "Moovio",
        onCloseRequest = {
            container.close()
            exitApplication()
        },
        state = WindowState(
            initialWindowSize = WindowSize(width = 1366, height = 768),
        ),
    ) {
        MainWindow(container)
    }
}
