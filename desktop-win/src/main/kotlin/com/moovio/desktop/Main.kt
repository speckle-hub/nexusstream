package com.moovio.desktop

import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.moovio.desktop.data.local.AppPaths
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.player.MpvLibrary
import com.moovio.desktop.ui.MoovioApp
import com.moovio.desktop.ui.WindowStyler
import java.awt.Dimension
import kotlinx.coroutines.delay

fun main() {
    AppLog.installCrashHandler()
    val container = AppContainer()
    AppLog.info(
        "Starting Moovio Desktop ${System.getProperty("moovio.version") ?: "dev"}; " +
            "data=${AppPaths.dataDir.absolutePath}; " +
            "libmpv=${MpvLibrary.locate(container.settings.current.mpvLibPath) ?: "NOT FOUND"}",
    )
    // The coroutine scope is cancelled as the app exits, so pending JSON writes can be lost.
    // Flush watch progress synchronously on the way out instead.
    Runtime.getRuntime().addShutdownHook(
        Thread {
            runCatching { container.library.flush() }
            runCatching { container.dispose() }
        },
    )

    application {
        val windowState = rememberWindowState(
            width = 1440.dp,
            height = 900.dp,
            position = WindowPosition(Alignment.Center),
        )

        Window(
            onCloseRequest = {
                runCatching { container.library.flush() }
                container.dispose()
                exitApplication()
            },
            title = "Moovio",
            state = windowState,
            icon = painterResource("moovio.png"),
        ) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                window.minimumSize = Dimension(1120, 700)
                delay(350)
                runCatching { WindowStyler.apply(window) }
            }
            MoovioApp(container, windowState)
        }
    }
}
