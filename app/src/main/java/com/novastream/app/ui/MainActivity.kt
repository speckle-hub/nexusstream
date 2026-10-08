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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.novastream.app.BuildConfig
import com.novastream.app.NovaApp
import com.novastream.app.data.integrations.ReleaseInfo
import com.novastream.app.data.integrations.UpdateChecker
import com.novastream.app.data.integrations.downloadApk
import com.novastream.app.data.integrations.installApk
import com.novastream.app.ui.nav.NovaApp as NovaAppUi
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.NovaStreamTheme
import com.novastream.app.ui.vm.LocalContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                        // Phase 25 — non-blocking launch-time update check. Composed *inside* the
                        // app-lock gate so the dialog only appears once the app is unlocked.
                        StartupUpdateCheck()
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

/**
 * Launch-time update check (Phase 25).
 *
 * Runs [UpdateChecker.check] once, off the composition thread, so app startup is never blocked on
 * the network. When GitHub reports a release newer than this build it shows [UpdateAvailableDialog]
 * with the version tag and release notes, offering to download + install the attached APK.
 */
@Composable
private fun StartupUpdateCheck() {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val configured = runCatching { container.settings.updateRepo.first() }.getOrDefault("")
        // Blank repo falls back to UpdateChecker.DEFAULT_REPO, so this works with no setup.
        val info = UpdateChecker.check(configured).getOrNull() ?: return@LaunchedEffect
        if (info.isNewerThan(BuildConfig.VERSION_NAME)) release = info
    }

    val info = release ?: return
    UpdateAvailableDialog(
        info = info,
        downloading = downloading,
        failure = failure,
        onUpdate = {
            val url = info.apkUrl
            if (url == null) {
                // Release has no APK asset — open its GitHub page instead of failing silently.
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(info.pageUrl),
                        )
                    )
                }
                release = null
            } else {
                scope.launch {
                    downloading = true
                    failure = null
                    val file = downloadApk(context, url)
                    downloading = false
                    if (file != null) {
                        release = null
                        runCatching { installApk(context, file) }
                            .onFailure { failure = "Couldn't launch the installer" }
                    } else {
                        failure = "Download failed. Check your connection and try again."
                    }
                }
            }
        },
        onDismiss = { if (!downloading) release = null },
    )
}

/** Expressive Material 3 dialog announcing a newer release, with Update / Later actions. */
@Composable
private fun UpdateAvailableDialog(
    info: ReleaseInfo,
    downloading: Boolean,
    failure: String?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val nova = LocalNovaColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = nova.surfaceElevated,
        icon = { Icon(Icons.Filled.SystemUpdate, null, tint = nova.accent, modifier = Modifier.size(28.dp)) },
        title = {
            Column {
                Text(
                    "New Version Available",
                    style = MaterialTheme.typography.headlineSmall,
                    color = nova.textPrimary,
                )
                Text(
                    info.tag,
                    style = MaterialTheme.typography.titleMedium,
                    color = nova.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            val notes = info.notes.trim().ifBlank { info.name }
            Column(
                Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            ) {
                Text("What's new", style = MaterialTheme.typography.labelLarge, color = nova.textSecondary)
                Spacer(Modifier.height(6.dp))
                Text(notes, style = MaterialTheme.typography.bodyMedium, color = nova.textPrimary)
                failure?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                enabled = !downloading,
                shape = RoundedCornerShape(16.dp),
            ) {
                if (downloading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(if (info.apkUrl != null) "Update" else "Open release")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !downloading) { Text("Later") }
        },
    )
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
