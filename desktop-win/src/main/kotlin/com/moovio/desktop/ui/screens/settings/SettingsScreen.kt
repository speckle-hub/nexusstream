package com.moovio.desktop.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moovio.desktop.data.local.AppPaths
import com.moovio.desktop.data.repo.RecommendedAddon
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.Addon
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.domain.model.StreamSource
import com.moovio.desktop.player.MpvLibrary
import com.moovio.desktop.security.PinHasher
import com.moovio.desktop.ui.components.GhostButton
import com.moovio.desktop.ui.components.PrimaryButton
import com.moovio.desktop.ui.components.SectionHeader
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.theme.AccentColor
import com.moovio.desktop.ui.theme.Accents
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.glass
import kotlinx.coroutines.launch

private const val TEST_CLIP =
    "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4"

@Composable
fun SettingsScreen(app: AppState, container: AppContainer) {
    val settings by container.settings.state.collectAsState()
    val addons by container.addonStore.state.collectAsState()
    val scope = rememberCoroutineScope()

    var manifestUrl by remember { mutableStateOf("") }
    var installing by remember { mutableStateOf(false) }
    var tmdbKey by remember(settings.tmdbApiKey) { mutableStateOf(settings.tmdbApiKey) }
    var mpvPath by remember(settings.mpvLibPath) { mutableStateOf(settings.mpvLibPath) }
    var showPinDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 48.dp, end = 48.dp, top = 34.dp, bottom = 56.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = Palette.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            "Moovio · desktop ${System.getProperty("moovio.version") ?: "1.0.0"}",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.textTertiary,
        )
        Spacer(Modifier.height(28.dp))

        SettingsSection("Appearance", "Accent and motion") {
            Text("Accent", style = MaterialTheme.typography.titleSmall, color = Palette.textSecondary)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Accents.all.forEach { preset ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(preset.primary)
                            .clickable { container.settings.update { it.copy(accent = preset.id) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (settings.accent == preset.id) {
                            Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Color.White)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            SettingToggle(
                title = "Reduce motion",
                subtitle = "Disables the hero auto-slide and card entrance animation.",
                checked = settings.reduceMotion,
            ) { value -> container.settings.update { it.copy(reduceMotion = value) } }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection("Playback", "How titles start") {
            SettingToggle(
                title = "Auto-play the best stream",
                subtitle = "Picks the highest-ranked playable source when you press Play.",
                checked = settings.autoPlayBest,
            ) { value -> container.settings.update { it.copy(autoPlayBest = value) } }
            SettingToggle(
                title = "Resume where you left off",
                subtitle = "Continue from your saved position automatically.",
                checked = settings.autoResume,
            ) { value -> container.settings.update { it.copy(autoResume = value) } }
            SettingToggle(
                title = "Hardware decoding",
                subtitle = "Ask libmpv to use D3D11 hardware decode when available.",
                checked = settings.hardwareDecoding,
            ) { value -> container.settings.update { it.copy(hardwareDecoding = value) } }

            Spacer(Modifier.height(14.dp))
            Text("Preferred quality", style = MaterialTheme.typography.titleSmall, color = Palette.textSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("2160p", "1080p", "720p", "auto").forEach { quality ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (settings.preferredQuality == quality) AccentColor.copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.05f),
                            )
                            .clickable { container.settings.update { it.copy(preferredQuality = quality) } }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            quality,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (settings.preferredQuality == quality) AccentColor else Palette.textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection("Add-ons", "Stremio protocol · one failing add-on never breaks the app") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DarkTextField(
                    value = manifestUrl,
                    onValueChange = { manifestUrl = it },
                    placeholder = "https://example.com/manifest.json",
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = if (installing) "Installing…" else "Install",
                    enabled = !installing && manifestUrl.isNotBlank(),
                    onClick = {
                        installing = true
                        scope.launch {
                            runCatching { container.addons.install(manifestUrl) }
                                .onSuccess {
                                    manifestUrl = ""
                                    app.showToast("Installed ${it.name}")
                                }
                                .onFailure { app.showToast(it.message ?: "Could not install add-on") }
                            installing = false
                        }
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Moovio ships legal metadata only. Stream providers are installed by you, at your own discretion.",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.textTertiary,
            )
            Spacer(Modifier.height(18.dp))

            addons.addons.forEach { addon ->
                AddonRow(
                    addon = addon,
                    onEnabled = { container.addons.setEnabled(addon.id, it) },
                    onAdult = { container.addons.setAdult(addon.id, it) },
                    onRemove = {
                        container.addons.remove(addon.id)
                        app.showToast("Removed ${addon.name}")
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "RECOMMENDED",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.textTertiary,
            )
            Spacer(Modifier.height(10.dp))
            container.addons.recommendedAddons().forEach { recommended ->
                RecommendedRow(
                    recommended = recommended,
                    installed = addons.addons.any {
                        it.manifestUrl == recommended.manifestUrl || it.name == recommended.name
                    },
                    onInstall = {
                        installing = true
                        scope.launch {
                            runCatching { container.addons.install(recommended.manifestUrl) }
                                .onSuccess { app.showToast("Installed ${it.name}") }
                                .onFailure { app.showToast(it.message ?: "Could not install add-on") }
                            installing = false
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection("Privacy", "The NSFW area is separate and PIN-gated") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (settings.hasNsfwPin) "PIN is set" else "No PIN yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = Palette.textPrimary,
                    )
                    Text(
                        "The NSFW section re-locks every launch and never appears in search, home or the library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.textTertiary,
                    )
                }
                if (container.isNsfwUnlocked) {
                    GhostButton("Lock now", Icons.Rounded.Lock, onClick = {
                        container.lockNsfw()
                        app.showToast("NSFW area locked")
                    })
                    Spacer(Modifier.width(10.dp))
                }
                PrimaryButton(
                    text = if (settings.hasNsfwPin) "Change PIN" else "Set PIN",
                    onClick = { showPinDialog = true },
                )
            }
            if (settings.hasNsfwPin) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = {
                    container.settings.update { it.copy(nsfwPinHash = "", nsfwPinSalt = "") }
                    container.lockNsfw()
                    app.showToast("NSFW PIN removed")
                }) {
                    Text("Remove PIN", color = Palette.textTertiary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection("System", "Player, storage and metadata keys") {
            val mpvFound = remember(settings.mpvLibPath) { MpvLibrary.locate(settings.mpvLibPath) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (mpvFound != null) Palette.success else Palette.warning),
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (mpvFound != null) "libmpv ready" else "libmpv not found",
                        style = MaterialTheme.typography.titleSmall,
                        color = Palette.textPrimary,
                    )
                    Text(
                        mpvFound ?: "Run ./gradlew fetchLibmpv, or point to libmpv-2.dll below.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.textTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                GhostButton("Test player", Icons.Rounded.PlayArrow, onClick = {
                    app.requestPlay(
                        PlayTarget(
                            media = MediaItem(
                                id = "moovio-test",
                                type = MediaType.MOVIE,
                                name = "Big Buck Bunny — test clip",
                                description = "Public-domain Blender short used to verify libmpv playback.",
                            ),
                            source = StreamSource(
                                url = TEST_CLIP,
                                addonId = "moovio.test",
                                addonName = "Moovio test",
                                quality = "720p",
                            ),
                        ),
                    )
                })
            }
            Spacer(Modifier.height(14.dp))
            DarkTextField(
                value = mpvPath,
                onValueChange = {
                    mpvPath = it
                    container.settings.update { copy -> copy.copy(mpvLibPath = it) }
                },
                placeholder = "Custom libmpv-2.dll path (optional)",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            DarkTextField(
                value = tmdbKey,
                onValueChange = {
                    tmdbKey = it
                    container.settings.update { copy -> copy.copy(tmdbApiKey = it.trim()) }
                },
                placeholder = "TMDB API key (optional — better artwork)",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Local data", style = MaterialTheme.typography.titleSmall, color = Palette.textPrimary)
                    Text(
                        AppPaths.dataDir.absolutePath,
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.textTertiary,
                    )
                }
                GhostButton("Open folder", Icons.Rounded.FolderOpen, onClick = {
                    runCatching { java.awt.Desktop.getDesktop().open(AppPaths.dataDir) }
                        .onFailure { app.showToast("Could not open folder") }
                })
            }
            Spacer(Modifier.height(12.dp))
            GhostButton("Reload metadata", Icons.Rounded.Refresh, onClick = {
                app.showToast("Metadata caches refresh on the next screen visit")
            })
        }

        Spacer(Modifier.height(22.dp))
        Text(
            "Moovio is a player and organizer. It ships no stream providers; metadata comes from " +
                "Cinemeta and AniList, and playback capability comes only from add-ons you install. " +
                "Respect copyright in your region.",
            style = MaterialTheme.typography.labelSmall,
            color = Palette.textTertiary.copy(alpha = 0.8f),
        )
    }

    if (showPinDialog) {
        PinSetupDialog(
            hasExistingPin = settings.hasNsfwPin,
            onDismiss = { showPinDialog = false },
            onSave = { pin ->
                val salt = PinHasher.newSalt()
                val hash = PinHasher.hash(pin, salt)
                container.settings.update { it.copy(nsfwPinHash = hash, nsfwPinSalt = salt) }
                container.unlockNsfw()
                showPinDialog = false
                app.showToast("NSFW PIN saved")
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(18.dp), alpha = 0.05f)
            .padding(22.dp),
    ) {
        SectionHeader(title, subtitle)
        Spacer(Modifier.height(18.dp))
        content()
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.textTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentColor,
                uncheckedThumbColor = Palette.textSecondary,
                uncheckedTrackColor = Palette.surfaceHighest,
                uncheckedBorderColor = Palette.outline,
            ),
        )
    }
}

@Composable
fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, style = MaterialTheme.typography.bodySmall, color = Palette.textTertiary)
        },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions.Default,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(11.dp),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentColor.copy(alpha = 0.7f),
            unfocusedBorderColor = Palette.outline,
            focusedContainerColor = Palette.surfaceHigh.copy(alpha = 0.6f),
            unfocusedContainerColor = Palette.surfaceHigh.copy(alpha = 0.35f),
            cursorColor = AccentColor,
            focusedTextColor = Palette.textPrimary,
            unfocusedTextColor = Palette.textPrimary,
        ),
    )
}

@Composable
private fun AddonRow(
    addon: Addon,
    onEnabled: (Boolean) -> Unit,
    onAdult: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .glass(RoundedCornerShape(12.dp), alpha = 0.035f)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(addon.name, style = MaterialTheme.typography.titleSmall, color = Palette.textPrimary)
                if (addon.builtIn) {
                    Text(
                        "BUILT-IN",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentColor,
                    )
                }
                if (addon.adult) {
                    Text("NSFW-only", style = MaterialTheme.typography.labelSmall, color = Palette.warning)
                }
            }
            Text(
                buildString {
                    append(addon.resources.joinToString(", ").ifBlank { "manifest" })
                    if (addon.version.isNotBlank()) append("  ·  v").append(addon.version)
                },
                style = MaterialTheme.typography.labelSmall,
                color = Palette.textTertiary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Enabled", style = MaterialTheme.typography.labelSmall, color = Palette.textTertiary)
                Switch(
                    checked = addon.enabled,
                    onCheckedChange = onEnabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AccentColor,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = Palette.surfaceHighest,
                        uncheckedBorderColor = Palette.outline,
                    ),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Private area only", style = MaterialTheme.typography.labelSmall, color = Palette.textTertiary)
                Switch(
                    checked = addon.adult,
                    onCheckedChange = onAdult,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Palette.warning,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = Palette.surfaceHighest,
                        uncheckedBorderColor = Palette.outline,
                    ),
                )
            }
            if (!addon.builtIn) {
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.danger,
                    modifier = Modifier.clickable(onClick = onRemove),
                )
            }
        }
    }
}

@Composable
private fun RecommendedRow(
    recommended: RecommendedAddon,
    installed: Boolean,
    onInstall: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(recommended.name, style = MaterialTheme.typography.titleSmall, color = Palette.textPrimary)
            Text(recommended.description, style = MaterialTheme.typography.bodySmall, color = Palette.textTertiary)
        }
        Text(
            recommended.kind,
            style = MaterialTheme.typography.labelSmall,
            color = Palette.textTertiary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        if (installed) {
            Text("Installed", style = MaterialTheme.typography.labelSmall, color = Palette.success)
        } else {
            GhostButton("Add", onClick = onInstall)
        }
    }
}

@Composable
private fun PinSetupDialog(
    hasExistingPin: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = PinHasher.isValidPin(pin) && pin == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasExistingPin) "Change NSFW PIN" else "Set an NSFW PIN", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "4–8 digits. The PIN is salted and hashed — it is never stored in plain text.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.textSecondary,
                )
                DarkTextField(
                    value = pin,
                    onValueChange = { newValue -> pin = newValue.filter(Char::isDigit).take(8) },
                    placeholder = "New PIN",
                    password = true,
                )
                DarkTextField(
                    value = confirm,
                    onValueChange = { newValue -> confirm = newValue.filter(Char::isDigit).take(8) },
                    placeholder = "Confirm PIN",
                    password = true,
                )
                if (pin.isNotEmpty() && !PinHasher.isValidPin(pin)) {
                    Text("Use 4–8 digits.", style = MaterialTheme.typography.labelSmall, color = Palette.warning)
                } else if (confirm.isNotEmpty() && pin != confirm) {
                    Text("PINs do not match.", style = MaterialTheme.typography.labelSmall, color = Palette.danger)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(pin) }, enabled = valid) {
                Text("Save PIN", color = if (valid) AccentColor else Palette.textTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.textSecondary) }
        },
        containerColor = Palette.surfaceHigh,
    )
}
