package com.moovio.app.ui.screens.addons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.moovio.app.data.model.AddonKind
import com.moovio.app.data.model.CloudStreamExt
import com.moovio.app.data.model.MangaExt
import com.moovio.app.ui.components.EmptyState
import com.moovio.app.ui.components.GlassSurface
import com.moovio.app.ui.components.MoovioSwitch
import com.moovio.app.ui.theme.LocalMoovioColors
import com.moovio.app.ui.vm.AddonViewModel
import com.moovio.app.ui.vm.collectAsStateSafe
import com.moovio.app.ui.vm.moovioViewModel

@Composable
fun AddonManagerScreen(nav: NavHostController) {
    val vm = moovioViewModel { AddonViewModel(it) }
    val moovio = LocalMoovioColors.current
    // rememberSaveable: rotation must not drop the user back to the Stremio tab.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("Stremio", "CloudStream", "Aniyomi", "Mihon")
    val message by vm.message.collectAsStateSafe()

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            vm.clearMessage()
        }
    }

    // navigationBarsPadding: pushed destination with no bottom tab bar.
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back", tint = moovio.textPrimary) }
            IconButton(onClick = { vm.syncRepos() }) { Icon(Icons.Filled.Sync, "Sync repositories", tint = moovio.accent) }
            IconButton(onClick = { vm.updateAll() }) { Icon(Icons.Filled.Refresh, "Update all", tint = moovio.accent) }
        }
        ScrollableTabRow(selectedTabIndex = tab, containerColor = moovio.background, contentColor = moovio.accent, edgePadding = 12.dp) {
            titles.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }) {
                    Text(t, modifier = Modifier.padding(14.dp), color = if (tab == i) moovio.accent else moovio.textTertiary)
                }
            }
        }
        when (tab) {
            0 -> StremioTab(vm)
            1 -> ProviderTab(vm, AddonKind.CLOUDSTREAM)
            2 -> ProviderTab(vm, AddonKind.ANIYOMI)
            else -> ProviderTab(vm, AddonKind.KEIYOUSHI)
        }
    }
}

@Composable
private fun StremioTab(vm: AddonViewModel) {
    val moovio = LocalMoovioColors.current
    var url by remember { mutableStateOf("") }
    val addons by vm.addons.collectAsStateSafe()
    val busy by vm.busy.collectAsStateSafe()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Add-on manifest URL") },
                    placeholder = { Text("https://…/manifest.json") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (url.isNotBlank()) { vm.install(url); url = "" } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Installing…" else "Install add-on")
                }
            }
        }
        // One-tap (re)installs for the seeded metadata/subtitle add-ons, so a user who removes
        // one can get it back without hunting for the URL.
        item { Text("Recommended", style = MaterialTheme.typography.titleMedium, color = moovio.textSecondary) }
        items(com.moovio.app.data.local.AddonStore.Defaults.recommendedStreamAddons) { (name, manifestUrl) ->
            val host = manifestHost(manifestUrl)
            val installed = addons.any { it.transportUrl.contains(host) }
            GlassSurface(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = moovio.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(manifestUrl, style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (installed) {
                        MetaChip("Installed")
                    } else {
                        IconButton(onClick = { vm.install(manifestUrl) }, enabled = !busy) {
                            Icon(Icons.Filled.Download, "Install $name", tint = moovio.accent)
                        }
                    }
                }
            }
        }

        item { Text("Installed (${addons.size})", style = MaterialTheme.typography.titleMedium, color = moovio.textSecondary) }
        if (addons.isEmpty()) item { EmptyState("No add-ons installed", "Paste a Stremio manifest URL above.") }
        items(addons, key = { it.id }) { addon ->
            GlassSurface(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(addon.name, style = MaterialTheme.typography.titleMedium, color = moovio.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetaChip(addon.kind.label)
                            MetaChip("${addon.catalogs.size} Catalogs")
                            if (addon.nsfw) MetaChip("NSFW")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    MoovioSwitch(checked = addon.enabled, onCheckedChange = { vm.setEnabled(addon.id, it) })
                }
            }
        }
    }
}

@Composable
private fun ProviderTab(vm: AddonViewModel, kind: AddonKind) {
    val moovio = LocalMoovioColors.current
    val repos by vm.repos.collectAsStateSafe()
    val catalog by vm.catalog.collectAsStateSafe()
    val busy by vm.busy.collectAsStateSafe()
    val csInstalled by vm.cloudStreamInstalled.collectAsStateSafe()
    val mangaInstalled by vm.mangaInstalled.collectAsStateSafe()
    val myRepos = repos.filter { it.kind == kind }
    var newRepoUrl by rememberSaveable(kind) { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // No repositories are preinstalled — the user adds their own by URL.
        item {
            Column {
                OutlinedTextField(
                    value = newRepoUrl,
                    onValueChange = { newRepoUrl = it },
                    label = { Text("Add repository URL") },
                    placeholder = { Text("https://…/repo.json") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val url = newRepoUrl.trim()
                        if (url.isNotEmpty()) {
                            vm.addRepo(com.moovio.app.data.model.RemoteRepo(manifestHost(url), url, kind))
                            newRepoUrl = ""
                        }
                    },
                    enabled = !busy && newRepoUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add repository") }
            }
        }
        // Well-known repositories offered as one-tap adds (never auto-installed).
        val recommended = com.moovio.app.data.local.AddonStore.Defaults.recommendedRepos
            .filter { it.second.kind == kind && myRepos.none { r -> r.url == it.second.url } }
        if (recommended.isNotEmpty()) {
            item { Text("Recommended", style = MaterialTheme.typography.titleMedium, color = moovio.textSecondary) }
            items(recommended) { (label, repo) ->
                GlassSurface(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.titleMedium, color = moovio.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(repo.url, style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { vm.addRepo(repo) }, enabled = !busy) {
                            Icon(Icons.Filled.Download, "Add $label", tint = moovio.accent)
                        }
                    }
                }
            }
        }

        item { Text("Repositories", style = MaterialTheme.typography.titleMedium, color = moovio.textSecondary) }
        if (myRepos.isEmpty()) item { EmptyState("No repositories added", "Paste a repository URL above to browse its extensions.") }
        items(myRepos) { repo ->
            GlassSurface(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(repo.name, style = MaterialTheme.typography.titleMedium, color = moovio.textPrimary)
                        Text(repo.url, style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(onClick = {
                        when (kind) {
                            AddonKind.CLOUDSTREAM -> vm.browseCloudStream(repo.url)
                            AddonKind.ANIYOMI -> vm.browseAniyomi(repo.url)
                            else -> vm.browseKeiyoushi(repo.url)
                        }
                    }, enabled = !busy) { Text(if (busy) "…" else "Browse") }
                    TextButton(onClick = { vm.removeRepo(repo.url) }) { Text("Remove", color = moovio.accent) }
                }
            }
        }

        if (catalog.isNotEmpty()) {
            item { Text("Available extensions (${catalog.size})", style = MaterialTheme.typography.titleMedium, color = moovio.textSecondary) }
            items(catalog) { any ->
                when (any) {
                    is CloudStreamExt -> ExtRow(any.name, any.version, any.language, any.description) { vm.installCloudStream(any) }
                    is MangaExt -> ExtRow(any.name, any.version, any.lang, "${any.sources.size} sources") { vm.installMangaExt(any) }
                    else -> {}
                }
            }
        }

        if (csInstalled.isNotEmpty() || mangaInstalled.isNotEmpty()) {
            item { Text("Installed", style = MaterialTheme.typography.titleMedium, color = moovio.textSecondary) }
            items(csInstalled) { ext ->
                InstalledRow(ext.name, ext.version, ext.language) { vm.uninstallCloudStream(ext.url) }
            }
            items(mangaInstalled) { ext ->
                InstalledRow(ext.name, ext.version, ext.lang) { vm.uninstallMangaExt(ext.pkg) }
            }
        }
    }
}

/** "example.com" from a manifest URL — used to tell whether an add-on is installed. */
private fun manifestHost(url: String): String = url
    .substringAfter("://", url)
    .substringBefore('/')
    .substringBefore(':')

/** A compact, clean metadata tag (replaces raw "·"-joined text dumps). */
@Composable
private fun MetaChip(text: String) {
    val moovio = LocalMoovioColors.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(moovio.surfaceElevated.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, moovio.outline.copy(alpha = 0.6f)), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = moovio.textSecondary, maxLines = 1)
    }
}

@Composable
private fun ExtRow(name: String, version: String?, lang: String?, desc: String?, onInstall: () -> Unit) {
    val moovio = LocalMoovioColors.current
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = moovio.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(version, lang, desc).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onInstall) { Icon(Icons.Filled.Download, "Install", tint = moovio.accent) }
        }
    }
}

@Composable
private fun InstalledRow(name: String, version: String?, lang: String?, onRemove: () -> Unit) {
    val moovio = LocalMoovioColors.current
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = moovio.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(version, lang).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary)
            }
            TextButton(onClick = onRemove) { Text("Remove", color = moovio.accent) }
        }
    }
}
