package com.novastream.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.novastream.app.NovaApp
import com.novastream.app.ui.nav.NovaApp as NovaAppUi
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.NovaStreamTheme
import com.novastream.app.ui.vm.LocalContainer

// Extends FragmentActivity (not just ComponentActivity) so BiometricPrompt can attach to it —
// androidx.biometric requires a FragmentActivity host for the NSFW unlock flow.
class MainActivity : FragmentActivity() {

    /**
     * Android 13+ gates notifications behind a runtime permission. The manifest declares it, but
     * without this request the app's foreground-service download notification is silently dropped
     * from the shade. Registered before `onCreate` (as required) and requested once per launch when
     * it isn't already granted; denial simply means no notification (downloads still run).
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationPermission()
        enableEdgeToEdge()
        val container = (application as NovaApp).container
        setContent {
            val theme by container.settings.theme.collectAsStateWithLifecycle("dark")
            val accent by container.settings.accent.collectAsStateWithLifecycle("violet")
            val dynamicColor by container.settings.dynamicColor.collectAsStateWithLifecycle(false)
            val amoledBlack by container.settings.amoledBlack.collectAsStateWithLifecycle(false)
            val flagSecure by container.settings.flagSecure.collectAsStateWithLifecycle(false)
            val appLock by container.settings.appLock.collectAsStateWithLifecycle(false)
            // Screen-protection: block screenshots / recents thumbnails when enabled.
            LaunchedEffect(flagSecure) {
                if (flagSecure) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            val dark = when (theme) {
                "light" -> false
                "system" -> isSystemInDarkTheme()
                else -> true
            }
            NovaStreamTheme(
                darkTheme = dark,
                accentKey = accent,
                dynamicColor = dynamicColor,
                amoledBlack = amoledBlack,
            ) {
                val bg = com.novastream.app.ui.theme.LocalNovaColors.current.background
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                CompositionLocalProvider(LocalContainer provides container) {
                    // App-lock gate: blocks all content behind biometric/device-credential unlock.
                    AppLockGate(activity = this@MainActivity, enabled = appLock) {
                        NovaAppUi()
                    }
                }
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            runCatching { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }
}

/**
 * Startup app-lock gate (Phase 12). When enabled, nothing renders until the user unlocks with
 * their biometric or device credential. If the device has no secure lock configured it fails open
 * (rather than permanently blocking the app).
 */
@Composable
private fun AppLockGate(
    activity: FragmentActivity,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val nova = LocalNovaColors.current
    // rememberSaveable (not remember): a rotation or any Activity recreation must not drop the
    // unlocked state and re-trigger the biometric prompt.
    var unlocked by rememberSaveable { mutableStateOf(!enabled) }
    LaunchedEffect(enabled) {
        if (!enabled) unlocked = true
        else if (!unlocked) promptUnlock(activity) { unlocked = true }
    }
    if (enabled && !unlocked) {
        Box(Modifier.fillMaxSize().background(nova.background), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Icon(Icons.Filled.Lock, null, tint = nova.accent, modifier = Modifier.size(48.dp))
                Text("NexusStream is locked", style = MaterialTheme.typography.titleLarge, color = nova.textPrimary)
                Text("Unlock to continue", style = MaterialTheme.typography.bodyMedium, color = nova.textSecondary)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { promptUnlock(activity) { unlocked = true } }) { Text("Unlock") }
            }
        }
    } else {
        content()
    }
}

/** Shows a BiometricPrompt with a device-credential fallback; fails open when unusable. */
private fun promptUnlock(activity: FragmentActivity, onSuccess: () -> Unit) {
    val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val can = BiometricManager.from(activity).canAuthenticate(authenticators)
    if (can != BiometricManager.BIOMETRIC_SUCCESS) {
        onSuccess()
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock NexusStream")
        .setSubtitle("Confirm it's you")
        .setAllowedAuthenticators(authenticators)
        .build()
    runCatching { prompt.authenticate(info) }
}
