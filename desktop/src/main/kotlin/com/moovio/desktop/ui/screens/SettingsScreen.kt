package com.moovio.desktop.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Icons
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.AccentViolet
import com.moovio.desktop.ui.theme.MoovioTextPrimary
import com.moovio.desktop.ui.theme.MoovioTextSecondary
import com.moovio.desktop.ui.theme.moovioColors

@Composable
fun SettingsScreen(container: AppContainer) {
    val settings = container.settings

    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var unlockPin by remember { mutableStateOf("") }
    var manifestUrl by remember { mutableStateOf("") }
    var addonStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(AppSpacing.xxl),
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.Start,
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.h3,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = AppSpacing.xl),
        )

        section("NSFW") {
            if (!settings.isNsfwUnlocked()) {
                TextField(
                    value = unlockPin,
                    onValueChange = { unlockPin = it },
                    placeholder = { Text("Enter PIN") },
                    modifier = Modifier.width(240.dp),
                    visualTransformation = VisualTransformation.Password,
                )
                Button(
                    onClick = {
                        // TODO: verify PIN against settings.nsfwPin
                        addonStatus = "PIN verification not yet wired"
                    },
                    modifier = Modifier.padding(top = AppSpacing.m),
                ) {
                    Text("Unlock")
                }
                if (settings.nsfwPin == null) {
                    Text("No PIN set — create one below.", style = MaterialTheme.typography.body2, color = MoovioTextSecondary)
                }
            } else {
                Text("NSFW unlocked. Locks again in 5 minutes of inactivity.", style = MaterialTheme.typography.body1, color = MoovioTextSecondary)
                Button(onClick = { /* TODO: lock now */ }, modifier = Modifier.padding(top = AppSpacing.m)) {
                    Text("Lock now")
                }
            }
            TextField(value = newPin, onValueChange = { newPin = it }, placeholder = { Text("New PIN (4-6 digits)" }),
                modifier = Modifier.width(240.dp))
            TextField(value = confirmPin, onValueChange = { confirmPin = it }, placeholder = { Text("Confirm PIN") },
                modifier = Modifier.width(240.dp).padding(top = AppSpacing.s))
            Button(
                onClick = {
                    if (newPin == confirmPin && newPin.length in 4..6) {
                        settings.nsfwPin = newPin
                        addonStatus = "PIN set"
                    } else {
                        addonStatus = "PINs must match (4-6 digits)"
                    }
                },
                modifier = Modifier.padding(top = AppSpacing.m),
            ) {
                Text("Set PIN")
            }
            addonStatus?.let { status ->
                Text(text = status, style = MaterialTheme.typography.body2, color = moovioColors.accent,
                    modifier = Modifier.padding(top = AppSpacing.s))
            }
        }

        section("Playback") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.s),
                verticalAlignment = androidx.compose.foundation.layout.Alignment.CenterVertically,
            ) {
                Switch(checked = settings.hardwareDecode, onCheckedChange = { settings.hardwareDecode = it })
                Text("Hardware decode (libmpv)", style = MaterialTheme.typography.body1, color = MoovioTextPrimary,
                    modifier = Modifier.padding(start = AppSpacing.m).weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.s),
                verticalAlignment = androidx.compose.foundation.layout.Alignment.CenterVertically,
            ) {
                Switch(checked = settings.reducedMotion, onCheckedChange = { settings.reducedMotion = it })
                Text("Reduce motion", style = MaterialTheme.typography.body1, color = MoovioTextPrimary,
                    modifier = Modifier.padding(start = AppSpacing.m).weight(1f))
            }
        }

        section("Add-ons (Stremio)") {
            Text("Install an add-on by manifest URL (e.g. https://torrentio.strem.fun/manifest.json)",
                style = MaterialTheme.typography.body1, color = MoovioTextSecondary,
                modifier = Modifier.padding(bottom = AppSpacing.m))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = manifestUrl,
                    onValueChange = { manifestUrl = it },
                    placeholder = { Text("https://example.com/manifest.json") },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        addonStatus = "Installing…"
                        // TODO: container.stremioRepository.installAddon(manifestUrl)
                    },
                ) {
                    Text("Install")
                }
            }
        }

        section("About") {
            Text("Moovio Desktop 0.1.0", style = MaterialTheme.typography.body1, color = MoovioTextPrimary)
            Text("Compose Multiplatform + libmpv · Windows", style = MaterialTheme.typography.body2, color = MoovioTextSecondary)
        }
    }
}

@Composable
private fun section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.xxl)) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            color = moovioColors.accent,
            modifier = Modifier.padding(bottom = AppSpacing.m),
        )
        content()
    }
}


