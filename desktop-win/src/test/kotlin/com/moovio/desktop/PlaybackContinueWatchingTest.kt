package com.moovio.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.domain.model.StreamSource
import com.moovio.desktop.player.MpvLibrary
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.screens.home.HomeScreen
import com.moovio.desktop.ui.screens.player.PlayerScreen
import com.moovio.desktop.ui.theme.MoovioTheme
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The acceptance path: really play a network stream, persist progress, then show it on Home.
 * Skipped unless MOOVIO_MPV_TEST=1 (network + real playback) and a libmpv binary is present.
 */
class PlaybackContinueWatchingTest {

    private val clip = "https://media.w3.org/2010/05/sintel/trailer.mp4"

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `playing a stream records progress and Home lists it`() = runComposeUiTest {
        if (System.getenv("MOOVIO_MPV_TEST") != "1") return@runComposeUiTest
        if (MpvLibrary.locate() == null) return@runComposeUiTest

        val previousDataDir = System.getProperty("moovio.data.dir")
        val dataDir = Files.createTempDirectory("moovio-playback").toFile()
        System.setProperty("moovio.data.dir", dataDir.absolutePath)
        val container = AppContainer()
        try {
            val media = MediaItem(id = "tt1727587", type = MediaType.MOVIE, name = "Sintel Trailer")
            val app = AppState(container)
            app.requestPlay(
                PlayTarget(
                    media = media,
                    source = StreamSource(url = clip, addonId = "test.streams", addonName = "Test streams"),
                ),
            )

            var inPlayer by mutableStateOf(true)
            setContent {
                MoovioTheme(reduceMotion = true) {
                    if (inPlayer) {
                        PlayerScreen(app, container, onExit = {})
                    } else {
                        HomeScreen(app, container, onOpen = {})
                    }
                }
            }

            waitUntil(timeoutMillis = 90_000) {
                (container.library.progressFor(media.key)?.positionMs ?: 0) >= 3_000
            }
            val saved = container.library.progressFor(media.key)
            assertTrue(saved != null && saved.durationMs > 0, "playback progress was not recorded")
            assertTrue(saved.inProgress, "saved progress is not considered in progress")

            inPlayer = false
            waitForIdle()

            onNodeWithText("Continue Watching").assertExists()
            onNodeWithText("Sintel Trailer").assertExists()
        } finally {
            container.dispose()
            if (previousDataDir == null) System.clearProperty("moovio.data.dir")
            else System.setProperty("moovio.data.dir", previousDataDir)
            runCatching { dataDir.deleteRecursively() }
        }
    }
}
