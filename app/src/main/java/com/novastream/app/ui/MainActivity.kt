package com.novastream.app.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.novastream.app.NovaApp
import com.novastream.app.ui.nav.NovaApp as NovaAppUi
import com.novastream.app.ui.theme.NovaStreamTheme
import com.novastream.app.ui.vm.LocalContainer

// Extends FragmentActivity (not just ComponentActivity) so BiometricPrompt can attach to it —
// androidx.biometric requires a FragmentActivity host for the NSFW unlock flow.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NovaApp).container
        setContent {
            val theme by container.settings.theme.collectAsState(initial = "dark")
            val accent by container.settings.accent.collectAsState(initial = "violet")
            val dark = when (theme) {
                "light" -> false
                "system" -> isSystemInDarkTheme()
                else -> true
            }
            NovaStreamTheme(darkTheme = dark, accentKey = accent) {
                val bg = com.novastream.app.ui.theme.LocalNovaColors.current.background
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                CompositionLocalProvider(LocalContainer provides container) {
                    NovaAppUi()
                }
            }
        }
    }
}
