package com.novastream.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.BuildConfig
import com.novastream.app.data.integrations.ReleaseInfo
import com.novastream.app.data.integrations.UpdateChecker
import com.novastream.app.data.integrations.downloadApk
import com.novastream.app.data.integrations.installApk
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.nav.LocalFloatingNavBottomPadding
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.vm.LocalContainer
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
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    // Phase 12 dialog state.
    var showHeaders by remember { mutableStateOf(false) }
    var tokenProvider by remember { mutableStateOf<String?>(null) }
    var showRepo by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }

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
    val downloadStats by vm.downloadStats.collectAsStateSafe()
    val dynamicColor by vm.dynamicColor.collectAsStateSafe()
    val amoledBlack by vm.amoledBlack.collectAsStateSafe()
    val subtitleScale by vm.subtitleScale.collectAsStateSafe()
    val subtitleColor by vm.subtitleColor.collectAsStateSafe()
    val subtitleBg by vm.subtitleBg.collectAsStateSafe()
    val subtitleOffset by vm.subtitleOffset.collectAsStateSafe()
    val playerSpeed by vm.playerSpeed.collectAsStateSafe()
    val doubleTapMs by vm.doubleTapMs.collectAsStateSafe()
    val audioBoost by vm.audioBoost.collectAsStateSafe()
    val audioNormalize by vm.audioNormalize.collectAsStateSafe()
    val mangaInvert by vm.mangaInvert.collectAsStateSafe()
    val mangaGrayscale by vm.mangaGrayscale.collectAsStateSafe()
    val mangaWarm by vm.mangaWarm.collectAsStateSafe()
    val mangaCrop by vm.mangaCrop.collectAsStateSafe()
    val mangaDoublePage by vm.mangaDoublePage.collectAsStateSafe()
    val mangaZoomLock by vm.mangaZoomLock.collectAsStateSafe()
    val mangaVolumeKeys by vm.mangaVolumeKeys.collectAsStateSafe()
    val incognito by vm.incognito.collectAsStateSafe()
    val flagSecure by vm.flagSecure.collectAsStateSafe()
    val appLock by vm.appLock.collectAsStateSafe()
    val customUserAgent by vm.customUserAgent.collectAsStateSafe()
    val customReferer by vm.customReferer.collectAsStateSafe()
    val customCookie by vm.customCookie.collectAsStateSafe()
    val maxParallel by vm.maxParallel.collectAsStateSafe()
    val autoDownloadNext by vm.autoDownloadNext.collectAsStateSafe()
    val autoDownloadWifiOnly by vm.autoDownloadWifiOnly.collectAsStateSafe()
    val autoDeleteWatched by vm.autoDeleteWatched.collectAsStateSafe()
    val storageUri by vm.storageUri.collectAsStateSafe()
    val updateRepo by vm.updateRepo.collectAsStateSafe()
    val notifyNewContent by vm.notifyNewContent.collectAsStateSafe()
    val scrobbleTrakt by vm.scrobbleTrakt.collectAsStateSafe()
    val scrobbleAniList by vm.scrobbleAniList.collectAsStateSafe()
    val scrobbleMal by vm.scrobbleMal.collectAsStateSafe()
    val scrobbleKitsu by vm.scrobbleKitsu.collectAsStateSafe()
    val scrobbleSimkl by vm.scrobbleSimkl.collectAsStateSafe()
    val tokenTrakt by vm.tokenTrakt.collectAsStateSafe()
    val tokenAniList by vm.tokenAniList.collectAsStateSafe()
    val tokenMal by vm.tokenMal.collectAsStateSafe()
    val tokenKitsu by vm.tokenKitsu.collectAsStateSafe()
    val tokenSimkl by vm.tokenSimkl.collectAsStateSafe()

    // SAF launchers for backup/restore + the storage folder picker.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            message = container.backupManager.exportTo(uri)
                .fold({ "Exported $it items" }, { "Export failed: ${it.message}" })
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            message = container.backupManager.importFrom(uri)
                .fold({ "Restored $it items" }, { "Import failed: ${it.message}" })
        }
    }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        uri?.let { vm.setStorageUri(it.toString()) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 24.dp + LocalFloatingNavBottomPadding.current,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.displaySmall, color = nova.textPrimary)
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
            // Scrollable: the picker used to clip the last swatches on a narrow phone, leaving a
            // cramped blank strip with no way to reach the remaining accents.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
            Spacer(Modifier.height(12.dp))
            SwitchRow("Material You dynamic colors", "Use the system wallpaper palette (Android 12+)", dynamicColor) { vm.setDynamicColor(it) }
            SwitchRow("True AMOLED black", "Replace dark surfaces with pitch #000000", amoledBlack) { vm.setAmoledBlack(it) }
        } }

        item { SettingsGroup("Player") {
            SwitchRow("Use external player", "Open streams in a third-party player", external) { vm.setPlayerExternal(it) }
            SettingsRow("Preferred quality", quality) {
                val opts = listOf("Auto", "4K", "1080p", "720p", "480p")
                vm.setPreferredQuality(opts[(opts.indexOf(quality).coerceAtLeast(0) + 1) % opts.size])
            }
            SettingsRow("Double-tap seek", "${doubleTapMs / 1000} s") {
                val opts = listOf(5_000, 10_000, 15_000, 30_000)
                vm.setDoubleTapMs(opts[(opts.indexOf(doubleTapMs).coerceAtLeast(0) + 1) % opts.size])
            }
            SwitchRow("Autoplay next episode", null, autoplay) { vm.setAutoplay(it) }
            SwitchRow("Subtitles enabled", "Auto-load matching subtitles", subs) { vm.setSubs(it) }
            Spacer(Modifier.height(10.dp))
            Text("Playback speed", style = MaterialTheme.typography.labelLarge, color = nova.textSecondary)
            Slider(
                value = playerSpeed,
                onValueChange = { vm.setPlayerSpeed(it) },
                valueRange = 0.5f..2f,
                steps = 5,
            )
            Text("${playerSpeed}x", style = MaterialTheme.typography.labelMedium, color = nova.textTertiary)
            SwitchRow("Audio boost", "Louder dialogue via system loudness enhancer", audioBoost) { vm.setAudioBoost(it) }
            SwitchRow("Normalize volume", "Even out quiet/loud scenes", audioNormalize) { vm.setAudioNormalize(it) }
        } }

        item { SettingsGroup("Subtitles") {
            Text("Text size", style = MaterialTheme.typography.labelLarge, color = nova.textSecondary)
            Slider(value = subtitleScale, onValueChange = { vm.setSubtitleScale(it) }, valueRange = 0.6f..2f)
            Text("${(subtitleScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = nova.textTertiary)
            Spacer(Modifier.height(8.dp))
            Text("Text color", style = MaterialTheme.typography.labelLarge, color = nova.textSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("white", "yellow", "cyan", "green").forEach { key ->
                    val selected = subtitleColor == key
                    val c = when (key) {
                        "yellow" -> androidx.compose.ui.graphics.Color(0xFFFFEB3B)
                        "cyan" -> androidx.compose.ui.graphics.Color(0xFF35D6E0)
                        "green" -> androidx.compose.ui.graphics.Color(0xFF7CFF7C)
                        else -> androidx.compose.ui.graphics.Color.White
                    }
                    // 48dp touch target with the visual dot (34dp) centred inside it.
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { vm.setSubtitleColor(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) nova.accent else nova.outline), CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .background(c),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Background opacity", style = MaterialTheme.typography.labelLarge, color = nova.textSecondary)
            Slider(value = subtitleBg, onValueChange = { vm.setSubtitleBg(it) }, valueRange = 0f..1f)
            Text("Vertical offset", style = MaterialTheme.typography.labelLarge, color = nova.textSecondary)
            Slider(value = subtitleOffset, onValueChange = { vm.setSubtitleOffset(it) }, valueRange = 0f..0.3f)
        } }

        item { SettingsGroup("Manga Reader") {
            SwitchRow("Night mode (invert)", "Invert page colors for dark reading", mangaInvert) { vm.setMangaInvert(it) }
            SwitchRow("Grayscale", "Render pages in black and white", mangaGrayscale) { vm.setMangaGrayscale(it) }
            SwitchRow("Warm / sepia filter", "Soften paper white and blue light", mangaWarm) { vm.setMangaWarm(it) }
            SwitchRow("Auto-crop borders", "Trim uniform white margins", mangaCrop) { vm.setMangaCrop(it) }
            SwitchRow("Double-page spread", "Two pages side-by-side in landscape", mangaDoublePage) { vm.setMangaDoublePage(it) }
            SwitchRow("Lock zoom across pages", "Keep the same zoom level when turning pages", mangaZoomLock) { vm.setMangaZoomLock(it) }
            SwitchRow("Volume keys turn pages", "Use volume up/down to navigate", mangaVolumeKeys) { vm.setMangaVolumeKeys(it) }
        } }

        item { SettingsGroup("NSFW") {
            SwitchRow("NSFW lock", "Require PIN/biometrics to open NSFW", nsfwLock) { vm.setNsfwLock(it) }
            SettingsRow("Set / change PIN", null) { showPinDialog = true }
            SwitchRow("Biometric unlock", "Use fingerprint / face", biometric) { vm.setNsfwBiometric(it) }
            SwitchRow("Auto-pull NSFW from add-ons", "Populate Real 18+ from installed NSFW add-ons", autoNsfw) { vm.setAutoNsfw(it) }
        } }

        item { SettingsGroup("Storage") {
            SwitchRow("Cache metadata", "Speed up repeat browsing", cacheMeta) { vm.setCacheMeta(it) }
            SettingsRow("Clear metadata cache", "${cacheSize / 1024 / 1024} MB") { vm.clearCache() }
            SettingsRow(
                "Completed downloads",
                formatBytes(downloadStats.completedBytes),
            ) {}
            SettingsRow(
                "Temporary / partial files",
                "${formatBytes(downloadStats.temporaryBytes + downloadStats.orphanBytes)} \u00b7 ${formatBytes(downloadStats.orphanBytes)} orphaned",
            ) {}
            SettingsRow(
                "Clear download cache & temporary files",
                "${formatBytes(downloadStats.reclaimableBytes)} reclaimable",
            ) {
                vm.clearDownloadCache()
                message = "Cleared temporary download files"
            }
        } }

        item { SettingsGroup("Security") {
            SwitchRow("App lock", "Require biometric / device credential on launch", appLock) { vm.setAppLock(it) }
            SwitchRow("Screen protection", "Block screenshots and recents previews", flagSecure) { vm.setFlagSecure(it) }
            SwitchRow("Incognito mode", "Pause watch and read history logging", incognito) { vm.setIncognito(it) }
        } }

        item { SettingsGroup("Sources & Network") {
            // launchSingleTop: a double-tap must not stack two copies of the same pushed screen.
            SettingsRow("Sources dashboard", "View, test and enable/disable extensions") {
                nav.navigate(Routes.SOURCES) { launchSingleTop = true }
            }
            SettingsRow("Home row order", "Reorder the home feed sections") {
                nav.navigate(Routes.HOME_ORDER) { launchSingleTop = true }
            }
            SettingsRow(
                "Custom HTTP headers",
                when {
                    customUserAgent.isBlank() && customReferer.isBlank() && customCookie.isBlank() -> "Not set"
                    else -> listOfNotNull(
                        customUserAgent.takeIf { it.isNotBlank() }?.let { "UA" },
                        customReferer.takeIf { it.isNotBlank() }?.let { "Referer" },
                        customCookie.takeIf { it.isNotBlank() }?.let { "Cookie" },
                    ).joinToString(" · ")
                },
            ) { showHeaders = true }
        } }

        item { SettingsGroup("Downloads") {
            SwitchRow("Auto-download next episode", "Pre-fetch ahead after an episode begins", autoDownloadNext) { vm.setAutoDownloadNext(it) }
            SwitchRow("Wi-Fi only", "Only auto-download on unmetered networks", autoDownloadWifiOnly) { vm.setAutoDownloadWifiOnly(it) }
            SwitchRow("Auto-delete watched downloads", "Remove the file once playback passes 90%", autoDeleteWatched) { vm.setAutoDeleteWatched(it) }
            SettingsRow("Max parallel downloads", "$maxParallel") { vm.setMaxParallel((maxParallel % 6) + 1) }
            // The folder is an export destination: Media3's cache can only live in app-private
            // storage, so completed downloads are copied here once they finish.
            SettingsRow("Copy downloads to folder", if (storageUri.isBlank()) "Off \u00b7 app storage only" else "On \u00b7 copied on completion") { storageLauncher.launch(null) }
            SwitchRow("New-content notifications", "Notify when new episodes/chapters appear", notifyNewContent) { vm.setNotifyNewContent(it) }
        } }

        item { SettingsGroup("Scrobbling & Sync") {
            ScrobbleRow("Trakt", scrobbleTrakt, vm::setScrobbleTrakt, tokenTrakt) { tokenProvider = "trakt" }
            ScrobbleRow("AniList", scrobbleAniList, vm::setScrobbleAniList, tokenAniList) { tokenProvider = "anilist" }
            ScrobbleRow("MyAnimeList", scrobbleMal, vm::setScrobbleMal, tokenMal) { tokenProvider = "mal" }
            ScrobbleRow("Kitsu", scrobbleKitsu, vm::setScrobbleKitsu, tokenKitsu) { tokenProvider = "kitsu" }
            ScrobbleRow("SIMKL", scrobbleSimkl, vm::setScrobbleSimkl, tokenSimkl) { tokenProvider = "simkl" }
        } }

        item { SettingsGroup("Backup & Updates") {
            SettingsRow("Export backup", "Save favourites, history and settings to .json") {
                exportLauncher.launch("novastream-backup.json")
            }
            SettingsRow("Import backup", "Restore from a .json backup") { importLauncher.launch(arrayOf("application/json")) }
            SettingsRow("Update repository", updateRepo.ifBlank { "Set owner/repo" }) { showRepo = true }
            SettingsRow("Check for updates", "Look for a newer GitHub release") {
                if (updateRepo.isBlank()) { message = "Set an update repository first"; return@SettingsRow }
                scope.launch {
                    message = "Checking…"
                    val info = UpdateChecker.check(updateRepo).getOrNull()
                    message = when {
                        info == null -> "Update check failed"
                        info.isNewerThan(BuildConfig.VERSION_NAME) -> "New release ${info.tag} available"
                        else -> "You're up to date (v${BuildConfig.VERSION_NAME})"
                    }
                    if (info != null && info.isNewerThan(BuildConfig.VERSION_NAME)) {
                        pendingUpdate = info
                    }
                }
            }
        } }

        item { SettingsGroup("About") {
            SettingsRow("NexusStream", "v${BuildConfig.VERSION_NAME} · Stremio + CloudStream + Aniyomi + Mihon") {}
        } }
    }

    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onSave = { vm.setNsfwPin(hashPin(it)); showPinDialog = false },
        )
    }

    if (showHeaders) {
        HeadersDialog(
            userAgent = customUserAgent,
            referer = customReferer,
            cookie = customCookie,
            onDismiss = { showHeaders = false },
            onSave = { ua, ref, ck -> vm.setCustomHeaders(ua, ref, ck); showHeaders = false },
        )
    }

    tokenProvider?.let { provider ->
        TokenDialog(
            provider = provider,
            current = when (provider) {
                "trakt" -> tokenTrakt
                "anilist" -> tokenAniList
                "mal" -> tokenMal
                "kitsu" -> tokenKitsu
                else -> tokenSimkl
            },
            onDismiss = { tokenProvider = null },
            onSave = { vm.setToken(provider, it); tokenProvider = null },
        )
    }

    if (showRepo) {
        TokenDialog(
            provider = "repository",
            current = updateRepo,
            onDismiss = { showRepo = false },
            onSave = { vm.setUpdateRepo(it); showRepo = false },
        )
    }

    message?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("NexusStream") },
            text = { Text(msg) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { message = null }) { Text("OK") } },
        )
    }

    pendingUpdate?.let { info ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingUpdate = null },
            title = { Text("Update ${info.tag}") },
            text = { Text(info.notes.take(600).ifBlank { info.name }) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val url = info.apkUrl
                    pendingUpdate = null
                    if (url != null) {
                        scope.launch {
                            message = "Downloading update…"
                            val file = downloadApk(context, url)
                            if (file != null) installApk(context, file)
                            message = if (file != null) "Starting installer…" else "Download failed"
                        }
                    } else {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(info.pageUrl),
                                )
                            )
                        }
                    }
                }) { Text(if (info.apkUrl != null) "Install" else "Open page") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingUpdate = null }) { Text("Later") } },
        )
    }
}

/** One scrobble provider row: enable switch, token status and a token editor button. */
@Composable
private fun ScrobbleRow(
    name: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    token: String,
    onEditToken: () -> Unit,
) {
    val nova = LocalNovaColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary)
            Text(
                if (token.isBlank()) "No token — tap to add" else "Token configured",
                style = MaterialTheme.typography.bodyMedium,
                color = nova.textTertiary,
                modifier = Modifier.clickable { onEditToken() },
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun HeadersDialog(
    userAgent: String,
    referer: String,
    cookie: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var ua by remember { mutableStateOf(userAgent) }
    var ref by remember { mutableStateOf(referer) }
    var ck by remember { mutableStateOf(cookie) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom HTTP headers") },
        text = {
            Column {
                OutlinedTextField(value = ua, onValueChange = { ua = it }, label = { Text("User-Agent") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text("Referer") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = ck, onValueChange = { ck = it }, label = { Text("Cookie") }, singleLine = true)
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onSave(ua.trim(), ref.trim(), ck.trim()) }) { Text("Save") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TokenDialog(
    provider: String,
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (provider == "repository") "Update repository" else "$provider token") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(if (provider == "repository") "owner/repo" else "Access token") },
                singleLine = true,
            )
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onSave(value.trim()) }) { Text("Save") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
    else -> "$bytes B"
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
