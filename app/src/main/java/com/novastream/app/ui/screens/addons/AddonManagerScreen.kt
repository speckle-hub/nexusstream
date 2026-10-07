package com.novastream.app.ui.screens.addons

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
import com.novastream.app.data.model.AddonKind
import com.novastream.app.data.model.CloudStreamExt
import com.novastream.app.data.model.MangaExt
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.AddonViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

@Composable
fun AddonManagerScreen(nav: NavHostController) {
    val vm = novaViewModel { AddonViewModel(it) }
    val nova = LocalNovaColors.current
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

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back", tint = nova.textPrimary) }
            IconButton(onClick = { vm.syncRepos() }) { Icon(Icons.Filled.Sync, "Sync repositories", tint = nova.accent) }
            IconButton(onClick = { vm.updateAll() }) { Icon(Icons.Filled.Refresh, "Update all", tint = nova.accent) }
        }
        ScrollableTabRow(selectedTabIndex = tab, containerColor = nova.background, contentColor = nova.accent, edgePadding = 12.dp) {
            titles.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }) {
                    Text(t, modifier = Modifier.padding(14.dp), color = if (tab == i) nova.accent else nova.textTertiary)
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
    val nova = LocalNovaColors.current
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
        // One-tap installs for the highest-quality stream providers. Torrentio and TPB+ are already
        // seeded on first launch, but they (and MediaFusion / OpenSubtitles) can be re-added here
        // if a user removes them, so no one has to hunt for a working source.
        item { Text("Recommended", style = MaterialTheme.typography.titleMedium, color = nova.textSecondary) }
        items(com.novastream.app.data.local.AddonStore.Defaults.recommendedStreamAddons) { (name, manifestUrl) ->
            val host = manifestHost(manifestUrl)
            val installed = addons.any { it.transportUrl.contains(host) }
            GlassSurface(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(manifestUrl, style = MaterialTheme.typography.labelMedium, color = nova.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (installed) {
                        MetaChip("Installed")
                    } else {
                        IconButton(onClick = { vm.install(manifestUrl) }, enabled = !busy) {
                            Icon(Icons.Filled.Download, "Install $name", tint = nova.accent)
                        }
                    }
                }
            }
        }

        item { Text("Installed (${addons.size})", style = MaterialTheme.typography.titleMedium, color = nova.textSecondary) }
        if (addons.isEmpty()) item { EmptyState("No add-ons installed", "Paste a Stremio manifest URL above.") }
        items(addons, key = { it.id }) { addon ->
            GlassSurface(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(addon.name, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetaChip(addon.kind.label)
                            MetaChip("${addon.catalogs.size} Catalogs")
                            if (addon.nsfw) MetaChip("NSFW")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = addon.enabled, onCheckedChange = { vm.setEnabled(addon.id, it) })
                }
            }
        }
    }
}

@Composable
private fun ProviderTab(vm: AddonViewModel, kind: AddonKind) {
    val nova = LocalNovaColors.current
    val repos by vm.repos.collectAsStateSafe()
    val catalog by vm.catalog.collectAsStateSafe()
    val busy by vm.busy.collectAsStateSafe()
    val csInstalled by vm.cloudStreamInstalled.collectAsStateSafe()
    val mangaInstalled by vm.mangaInstalled.collectAsStateSafe()
    val myRepos = repos.filter { it.kind == kind }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Repositories", style = MaterialTheme.typography.titleMedium, color = nova.textSecondary) }
        items(myRepos) { repo ->
            GlassSurface(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(repo.name, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary)
                        Text(repo.url, style = MaterialTheme.typography.labelMedium, color = nova.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(onClick = {
                        when (kind) {
                            AddonKind.CLOUDSTREAM -> vm.browseCloudStream(repo.url)
                            AddonKind.ANIYOMI -> vm.browseAniyomi(repo.url)
                            else -> vm.browseKeiyoushi(repo.url)
                        }
                    }, enabled = !busy) { Text(if (busy) "…" else "Browse") }
                }
            }
        }

        if (catalog.isNotEmpty()) {
            item { Text("Available extensions (${catalog.size})", style = MaterialTheme.typography.titleMedium, color = nova.textSecondary) }
            items(catalog) { any ->
                when (any) {
                    is CloudStreamExt -> ExtRow(any.name, any.version, any.language, any.description) { vm.installCloudStream(any) }
                    is MangaExt -> ExtRow(any.name, any.version, any.lang, "${any.sources.size} sources") { vm.installMangaExt(any) }
                    else -> {}
                }
            }
        }

        if (csInstalled.isNotEmpty() || mangaInstalled.isNotEmpty()) {
            item { Text("Installed", style = MaterialTheme.typography.titleMedium, color = nova.textSecondary) }
            items(csInstalled) { ext ->
                InstalledRow(ext.name, ext.version, ext.language) { vm.uninstallCloudStream(ext.url) }
            }
            items(mangaInstalled) { ext ->
                InstalledRow(ext.name, ext.version, ext.lang) { vm.uninstallMangaExt(ext.pkg) }
            }
        }
    }
}

/** "torrentio.strem.fun" from a manifest URL — used to tell whether an add-on is installed. */
private fun manifestHost(url: String): String = url
    .substringAfter("://", url)
    .substringBefore('/')
    .substringBefore(':')

/** A compact, clean metadata tag (replaces raw "·"-joined text dumps). */
@Composable
private fun MetaChip(text: String) {
    val nova = LocalNovaColors.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(nova.surfaceElevated.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, nova.outline.copy(alpha = 0.6f)), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = nova.textSecondary, maxLines = 1)
    }
}

@Composable
private fun ExtRow(name: String, version: String?, lang: String?, desc: String?, onInstall: () -> Unit) {
    val nova = LocalNovaColors.current
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(version, lang, desc).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = nova.textTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onInstall) { Icon(Icons.Filled.Download, "Install", tint = nova.accent) }
        }
    }
}

@Composable
private fun InstalledRow(name: String, version: String?, lang: String?, onRemove: () -> Unit) {
    val nova = LocalNovaColors.current
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(version, lang).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = nova.textTertiary)
            }
            TextButton(onClick = onRemove) { Text("Remove", color = nova.accent) }
        }
    }
}
