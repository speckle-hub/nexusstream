package com.moovio.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.security.PinHasher
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.nav.Screen
import com.moovio.desktop.ui.screens.nsfw.NsfwScreen
import com.moovio.desktop.ui.screens.settings.SettingsScreen
import com.moovio.desktop.ui.theme.MoovioTheme
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class NsfwAndAddonUiTest {

    private val previousDataDir = System.getProperty("moovio.data.dir")
    private lateinit var dataDir: File

    @BeforeTest
    fun useTemporaryDataDir() {
        dataDir = Files.createTempDirectory("moovio-ui").toFile()
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
    fun `a correct pin replaces the gate with the private area`() = runComposeUiTest {
        val container = AppContainer()
        val salt = PinHasher.newSalt()
        container.settings.update { it.copy(nsfwPinSalt = salt, nsfwPinHash = PinHasher.hash("4821", salt)) }
        val app = AppState(container)
        app.navigateTab(Screen.Nsfw)

        setContent {
            MoovioTheme(reduceMotion = true) { NsfwScreen(app, container, onOpen = {}) }
        }

        onNodeWithText("Private area").assertExists()
        onNode(hasSetTextAction()).performTextInput("4821")
        onNodeWithText("Unlock").performClick()

        onNodeWithText("PRIVATE AREA").assertExists()
        onNodeWithText("Unlock").assertDoesNotExist()

        container.dispose()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `settings mark only explicitly private add-ons as nsfw-only`() = runComposeUiTest {
        val container = AppContainer()
        val app = AppState(container)
        app.navigateTab(Screen.Settings)

        setContent {
            MoovioTheme(reduceMotion = true) { SettingsScreen(app, container) }
        }

        onNodeWithText("Cinemeta").assertExists()
        onAllNodesWithText("NSFW-only").assertCountEquals(0)

        container.addons.setAdult(CinemetaClient.CINEMETA_ID, true)
        waitForIdle()

        onAllNodesWithText("NSFW-only").assertCountEquals(1)

        container.dispose()
    }
}
