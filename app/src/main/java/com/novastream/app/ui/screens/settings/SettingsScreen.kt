package com.novastream.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.screens.nsfw.hashPin
import com.novastream.app.ui.theme.Accents
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.SettingsViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

@Composable
fun SettingsScreen(nav: NavHostController) {
    val vm = novaViewModel { SettingsViewModel(it) }
    val nova = LocalNovaColors.current
    var showPinDialog by remember { mutableStateOf(false) }

    val theme by vm.theme.collectAsStateSafe()
    val accent by vm.accent.collectAsStateSafe()
    val external by vm.playerExternal.collectAsStateSafe()
    val quality by vm.preferredQuality.collectAsStateSafe()
    val autoplay by vm.autoplay.collectAsStateSafe()
    val subs by vm.subsEnabled.collectAsStateSafe()
    val nsfwLock by vm.nsfwLock.collectAsStateSafe()
    val biometric by vm.nsfwBiometric.collectAsStateSafe()
    val autoNsfw by vm.autoNsfw.collectAsStateSafe()
    val cacheMeta by vm.cacheMeta.collectAsStateSafe()
    val cacheSize by vm.cacheSize.collectAsStateSafe()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 48.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.displaySmall, color = nova.textPrimary, fontWeight = FontWeight.Bold)
        }

        item { SettingsGroup("Add-ons & Extensions") {
            SettingsRow("Add-on Manager", "Install Stremio add-ons, CloudStream, Aniyomi, Mihon") { nav.navigate(Routes.ADDONS) }
        } }

        item { SettingsGroup("Appearance") {
            SettingsRow("Theme", theme.replaceFirstChar { it.uppercase() }) {
                vm.setTheme(if (theme == "dark") "light" else "dark")
            }
            Spacer(Modifier.height(10.dp))
            Text("Accent", style = MaterialTheme.typography.labelLarge, color = nova.textSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Accents.forEach { a ->
                    val selected = accent == a.key
                    // An outer ring makes the active accent unmistakable.
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(
                                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) a.color else nova.outline.copy(alpha = 0.6f)),
                                CircleShape,
                            )
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(a.color)
                            .clickable { vm.setAccent(a.key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Filled.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        } }

        item { SettingsGroup("Player") {
            SwitchRow("Use external player", "Open streams in a third-party player", external) { vm.setPlayerExternal(it) }
            SettingsRow("Preferred quality", quality) {
                val opts = listOf("Auto", "4K", "1080p", "720p", "480p")
                vm.setPreferredQuality(opts[(opts.indexOf(quality).coerceAtLeast(0) + 1) % opts.size])
            }
            SwitchRow("Autoplay next episode", null, autoplay) { vm.setAutoplay(it) }
            SwitchRow("Subtitles enabled", "Auto-load matching subtitles", subs) { vm.setSubs(it) }
        } }

        item { SettingsGroup("NSFW") {
            SwitchRow("NSFW lock", "Require PIN/biometrics to open NSFW", nsfwLock) { vm.setNsfwLock(it) }
            SettingsRow("Set / change PIN", null) { showPinDialog = true }
            SwitchRow("Biometric unlock", "Use fingerprint / face", biometric) { vm.setNsfwBiometric(it) }
            SwitchRow("Auto-pull NSFW from add-ons", "Populate Real 18+ from installed NSFW add-ons", autoNsfw) { vm.setAutoNsfw(it) }
        } }

        item { SettingsGroup("Storage") {
            SwitchRow("Cache metadata", "Speed up repeat browsing", cacheMeta) { vm.setCacheMeta(it) }
            SettingsRow("Clear cache", "${cacheSize / 1024 / 1024} MB") { vm.clearCache() }
        } }

        item { SettingsGroup("About") {
            SettingsRow("NexusStream", "v1.0.0 · Stremio + CloudStream + Aniyomi + Mihon") {}
        } }
    }

    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onSave = { vm.setNsfwPin(hashPin(it)); showPinDialog = false },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val nova = LocalNovaColors.current
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = nova.accent, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String?, onClick: () -> Unit) {
    val nova = LocalNovaColors.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = nova.textTertiary) }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = nova.textTertiary)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val nova = LocalNovaColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = nova.textTertiary) }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set NSFW PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8) },
                label = { Text("PIN (4–8 digits)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        },
        confirmButton = { TextButton(onClick = { if (pin.length >= 4) onSave(pin) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
