package com.moovio.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.WatchProgress
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.screens.home.HomeScreen
import com.moovio.desktop.ui.theme.MoovioTheme
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class HomeContinueWatchingUiTest {

    private val previousDataDir = System.getProperty("moovio.data.dir")
    private lateinit var dataDir: File

    @BeforeTest
    fun useTemporaryDataDir() {
        dataDir = Files.createTempDirectory("moovio-home").toFile()
        System.setProperty("moovio.data.dir", dataDir.absolutePath)
    }

    @AfterTest
    fun restoreDataDir() {
        if (previousDataDir == null) System.clearProperty("moovio.data.dir")
        else System.setProperty("moovio.data.dir", previousDataDir)
        runCatching { dataDir.deleteRecursively() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `home shows a title saved by the player`() = runComposeUiTest {
        val container = AppContainer()
        val episode = MediaItem(id = "tt1196946", type = MediaType.SERIES, name = "The Vampire Diaries")
        container.library.recordProgress(
            WatchProgress(
                mediaKey = episode.key,
                mediaId = episode.id,
                type = episode.type,
                name = episode.name,
                season = 1,
                episode = 1,
                episodeTitle = "Pilot",
                positionMs = 3_461,
                durationMs = 2_698_133,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        val app = AppState(container)

        setContent {
            MoovioTheme(reduceMotion = true) { HomeScreen(app, container, onOpen = {}) }
        }

        onNodeWithText("Continue Watching").assertExists()
        onNodeWithText("The Vampire Diaries").assertExists()

        container.dispose()
    }
}
