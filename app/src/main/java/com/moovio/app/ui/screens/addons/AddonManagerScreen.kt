package com.moovio.app.ui.screens.addons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.moovio.app.data.local.AddonStore
import com.moovio.app.data.mihon.MihonExtensionSources
import com.moovio.app.data.model.AddonKind
import com.moovio.app.data.model.CloudStreamExt
import com.moovio.app.data.model.MangaExt
import com.moovio.app.data.model.RemoteRepo
import com.moovio.app.ui.components.EmptyState
import com.moovio.app.ui.components.MoovioSwitch
import com.moovio.app.ui.components.SurfaceCard
import com.moovio.app.ui.theme.LocalMoovioColors
import com.moovio.app.ui.vm.AddonViewModel
import com.moovio.app.ui.vm.collectAsStateSafe
import com.moovio.app.ui.vm.RepoBrowse
import com.moovio.app.ui.vm.moovioViewModel

/** Which slice of a browsed catalogue is listed. */
private enum class CatalogFilter { ALL, INSTALLED, UPDATES }

/** Display name of a provider section. */
private fun sectionLabel(kind: AddonKind): String = when (kind) {
    AddonKind.STREMIO -> "Stremio"
    AddonKind.CLOUDSTREAM -> "CloudStream"
    AddonKind.ANIYOMI -> "Aniyomi"
    AddonKind.KEIYOUSHI -> "Mihon"
}

/**
 * Add-on Manager: four independent sections (Stremio, CloudStream, Aniyomi, Mihon).
 *
 * Each provider section owns its repositories, its installed list and its browse results — they
 * used to share one catalogue and one installed list, which is why Aniyomi extensions showed up
 * under CloudStream.
 */
@Composable
fun AddonManagerScreen(nav: NavHostController) {
    val vm = moovioViewModel { AddonViewModel(it) }
    val moovio = LocalMoovioColors.current
    // rememberSaveable: rotation must not drop the user back to the Stremio tab.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val addons by vm.addons.collectAsStateSafe()
    val cloudStream by vm.cloudStreamInstalled.collectAsStateSafe()
    val manga by vm.mangaInstalled.collectAsStateSafe()
    val message by vm.message.collectAsStateSafe()

    val sections = listOf(AddonKind.STREMIO, AddonKind.CLOUDSTREAM, AddonKind.ANIYOMI, AddonKind.KEIYOUSHI)
    fun installedCount(kind: AddonKind): Int = when (kind) {
        AddonKind.STREMIO -> addons.size
        AddonKind.CLOUDSTREAM -> cloudStream.size
        else -> manga.count { it.section == kind }
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(4000)
            vm.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = moovio.textPrimary)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { vm.syncRepos() }) {
                    Icon(Icons.Filled.Sync, "Sync repositories", tint = moovio.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sync", color = moovio.accent)
                }
                TextButton(onClick = { vm.updateAll() }) {
                    Icon(Icons.Filled.Refresh, "Update Stremio add-ons", tint = moovio.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh", color = moovio.accent)
                }
            }
            ScrollableTabRow(
                selectedTabIndex = tab,
                containerColor = moovio.background,
                contentColor = moovio.accent,
                edgePadding = 12.dp,
            ) {
                sections.forEachIndexed { i, kind ->
                    Tab(selected = tab == i, onClick = { tab = i }) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                sectionLabel(kind),
                                color = if (tab == i) moovio.accent else moovio.textTertiary,
                                maxLines = 1,
                            )
                            val count = installedCount(kind)
                            if (count > 0) CountBadge(count, highlighted = tab == i)
                        }
                    }
                }
            }
            when (sections.getOrElse(tab) { AddonKind.STREMIO }) {
                AddonKind.STREMIO -> StremioTab(vm)
                AddonKind.CLOUDSTREAM -> ProviderTab(vm, AddonKind.CLOUDSTREAM)
                AddonKind.ANIYOMI -> ProviderTab(vm, AddonKind.ANIYOMI)
                AddonKind.KEIYOUSHI -> ProviderTab(vm, AddonKind.KEIYOUSHI)
            }
        }

        // Feedback used to be collected and cleared without ever being rendered, so a failed
        // repository fetch or install was invisible. It is now shown above the list.
        message?.let { text ->
            Surface(
                color = moovio.surfaceElevated,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
            ) {
                Text(
                    text,
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = moovio.textPrimary,
                )
            }
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
                Button(
                    onClick = { if (url.isNotBlank()) { vm.install(url); url = "" } },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (busy) "Installing…" else "Install add-on")
                }
            }
        }
        // One-tap (re)installs for the seeded metadata/subtitle add-ons, so a user who removes
        // one can get it back without hunting for the URL.
        item { SectionHeader("Recommended") }
        items(AddonStore.Defaults.recommendedStreamAddons) { (name, manifestUrl) ->
            val host = manifestHost(manifestUrl)
            val installed = addons.any { it.transportUrl.contains(host) }
            SurfaceCard(Modifier.fillMaxWidth()) {
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

        item { SectionHeader("Installed (${addons.size})") }
        if (addons.isEmpty()) item { EmptyState("No add-ons installed", "Paste a Stremio manifest URL above.") }
        items(addons, key = { it.id }) { addon ->
            SurfaceCard(Modifier.fillMaxWidth()) {
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
    val browse by vm.browse.collectAsStateSafe()
    val busy by vm.busy.collectAsStateSafe()
    val cloudStreamInstalled by vm.cloudStreamInstalled.collectAsStateSafe()
    val mangaInstalled by vm.mangaInstalled.collectAsStateSafe()
    val mihonLoaded by vm.mihonLoadedSources.collectAsStateWithLifecycle(emptyList())
    val showNsfw by vm.showNsfwExtensions.collectAsStateSafe()
    val mangaUpdates by vm.mangaUpdates.collectAsStateSafe()

    val isCloudStream = kind == AddonKind.CLOUDSTREAM
    val myRepos = repos.filter { it.kind == kind }
    // Each section shows only its own installed extensions — the reason Aniyomi extensions used to
    // appear under CloudStream (and every section listed every other section's installs).
    val installedCloudStream = if (isCloudStream) cloudStreamInstalled else emptyList()
    val installedManga = if (isCloudStream) emptyList() else mangaInstalled.filter { it.section == kind }
    val state = browse[kind] ?: RepoBrowse(kind = kind)

    var query by rememberSaveable(kind) { mutableStateOf("") }
    var filterIndex by rememberSaveable(kind) { mutableIntStateOf(0) }
    val filter = CatalogFilter.entries.getOrElse(filterIndex) { CatalogFilter.ALL }
    var addOpen by rememberSaveable(kind) { mutableStateOf(false) }

    val installedPkgs = installedManga.map { it.pkg }.toSet()
    val installedUrls = installedCloudStream.map { it.url }.toSet()
    fun isInstalled(entry: Any): Boolean = when (entry) {
        is CloudStreamExt -> entry.url in installedUrls
        is MangaExt -> entry.pkg in installedPkgs
        else -> false
    }
    fun hasUpdate(entry: Any): Boolean = entry is MangaExt && mangaUpdates.containsKey(entry.pkg)
    // `nsfw = 1` entries are hidden unless the user opted in here, so the manager stays SFW by
    // default and adult extensions can only be reached deliberately.
    fun visible(entry: Any): Boolean = when (entry) {
        is CloudStreamExt -> showNsfw || !entry.nsfw
        is MangaExt -> showNsfw || !entry.nsfw
        else -> true
    }

    val visibleItems = state.items.filter { visible(it) }
    val hiddenByNsfw = state.items.size - visibleItems.size
    val queryText = query.trim()
    val searched = visibleItems.filter { entry ->
        queryText.isEmpty() || when (entry) {
            is CloudStreamExt ->
                entry.name.contains(queryText, true) ||
                    entry.language?.contains(queryText, true) == true ||
                    entry.description?.contains(queryText, true) == true
            is MangaExt ->
                entry.name.contains(queryText, true) ||
                    entry.pkg.contains(queryText, true) ||
                    entry.lang?.contains(queryText, true) == true
            else -> false
        }
    }
    val installedMatches = searched.count { isInstalled(it) }
    val updateMatches = searched.count { hasUpdate(it) }
    val shown = when (filter) {
        CatalogFilter.ALL -> searched
        CatalogFilter.INSTALLED -> searched.filter { isInstalled(it) }
        CatalogFilter.UPDATES -> searched.filter { hasUpdate(it) }
    }
    val installedTotal = installedCloudStream.size + installedManga.size

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ---- Repositories: pills, not forms -------------------------------------
        item { SectionHeader("Repositories") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(myRepos, key = { "repo-${it.url}" }) { repo ->
                    FilterChip(
                        selected = state.repoUrl == repo.url,
                        enabled = !busy,
                        onClick = { vm.browseRepo(kind, repo) },
                        label = { Text(repo.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
                val recommended = AddonStore.Defaults.recommendedRepos
                    .filter { it.second.kind == kind && myRepos.none { r -> r.url == it.second.url } }
                items(recommended) { (label, repo) ->
                    FilterChip(
                        selected = false,
                        enabled = !busy,
                        onClick = { vm.addRepo(repo) },
                        label = { Text("+ $label", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        enabled = !busy,
                        onClick = { addOpen = true },
                        label = { Text("Add…") },
                    )
                }
            }
        }

        if (myRepos.any { it.nsfw }) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Show NSFW extensions",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = moovio.textSecondary,
                    )
                    MoovioSwitch(checked = showNsfw, onCheckedChange = { vm.setShowNsfwExtensions(it) })
                }
            }
        }

        // ---- Installed (this section only) --------------------------------------
        item { SectionHeader("Installed ($installedTotal)") }
        if (installedTotal == 0) {
            item {
                Text(
                    if (isCloudStream) "No CloudStream extensions installed — tap a repository above to browse."
                    else "No ${sectionLabel(kind)} extensions installed — tap a repository above to browse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = moovio.textTertiary,
                )
            }
        }
        items(installedCloudStream) { ext ->
            InstalledRow(ext.name, ext.version, ext.language) { vm.uninstallCloudStream(ext.url) }
        }
        items(installedManga) { ext ->
            val update = mangaUpdates[ext.pkg]
            val live = mihonLoaded.any { s -> MihonExtensionSources.sourceName(s) == ext.name }
            InstalledExtRow(
                name = ext.name,
                version = ext.version,
                detail = listOfNotNull(ext.lang, "${ext.sources.size} sources").joinToString(" · "),
                nsfw = ext.nsfw,
                enabled = !ext.disabled,
                live = live,
                updateAvailable = update != null,
                onToggle = { vm.setMangaExtEnabled(ext.pkg, it) },
                onUpdate = { update?.let { vm.installMangaExt(it) } },
                onRemove = { vm.uninstallMangaExt(ext.pkg) },
            )
        }
        if (!isCloudStream && installedManga.isNotEmpty() && !vm.mihonRuntimeAvailable) {
            item { RuntimeNotice() }
        }

        // ---- Browsed catalogue --------------------------------------------------
        item {
            SectionHeader(
                state.repoName?.let { "Browse · $it" } ?: "Browse",
            )
        }
        if (state.repoUrl == null) {
            item {
                Text(
                    if (myRepos.isEmpty()) "Add a repository to browse its extensions."
                    else "Tap a repository pill above to list its extensions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = moovio.textTertiary,
                )
            }
        } else {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search ${visibleItems.size} extensions") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = moovio.textTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter == CatalogFilter.ALL,
                            onClick = { filterIndex = 0 },
                            label = { Text("All ${searched.size}") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = filter == CatalogFilter.INSTALLED,
                            onClick = { filterIndex = 1 },
                            label = { Text("Installed $installedMatches") },
                        )
                    }
                    if (updateMatches > 0) {
                        item {
                            FilterChip(
                                selected = filter == CatalogFilter.UPDATES,
                                onClick = { filterIndex = 2 },
                                label = { Text("Updates $updateMatches") },
                            )
                        }
                    }
                }
            }
            if (state.error != null) {
                item {
                    Text(
                        "Couldn't load this repository — ${state.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = moovio.accent,
                    )
                }
            }
            if (hiddenByNsfw > 0) {
                item {
                    Text(
                        "$hiddenByNsfw adult extension${if (hiddenByNsfw == 1) "" else "s"} hidden — enable “Show NSFW extensions”",
                        style = MaterialTheme.typography.labelMedium,
                        color = moovio.textTertiary,
                    )
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        when {
                            busy -> "Loading…"
                            state.error != null -> "Nothing to show."
                            filter == CatalogFilter.INSTALLED -> "None of these extensions are installed."
                            filter == CatalogFilter.UPDATES -> "Everything installed is up to date."
                            queryText.isNotEmpty() -> "No extension matches “$queryText”."
                            else -> "This repository has no extensions."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = moovio.textTertiary,
                    )
                }
            }
            items(shown) { entry ->
                when (entry) {
                    is CloudStreamExt -> ExtRow(
                        name = entry.name,
                        version = entry.version,
                        lang = entry.language,
                        desc = entry.description,
                        nsfw = entry.nsfw,
                        installed = isInstalled(entry),
                        onInstall = { vm.installCloudStream(entry) },
                    )
                    is MangaExt -> ExtRow(
                        name = entry.name,
                        version = entry.version,
                        lang = entry.lang,
                        desc = "${entry.sources.size} sources",
                        nsfw = entry.nsfw,
                        installed = isInstalled(entry),
                        updateAvailable = hasUpdate(entry),
                        onInstall = { vm.installMangaExt(entry) },
                    )
                    else -> {}
                }
            }
        }
    }

    if (addOpen) {
        AddRepoDialog(
            kind = kind,
            onDismiss = { addOpen = false },
            onAdd = { url ->
                vm.addRepo(RemoteRepo(manifestHost(url), url, kind))
                addOpen = false
            },
        )
    }
}

/** The URL shape each index uses, shown as the dialog's hint. */
private fun repoHint(kind: AddonKind): String = when (kind) {
    AddonKind.CLOUDSTREAM -> "CloudStream repository manifest — …/builds/repo.json"
    AddonKind.ANIYOMI -> "Aniyomi extension index — …/repo/index.min.json"
    AddonKind.KEIYOUSHI -> "Mihon extension index — …/repo/index.pb (or index.min.json)"
    AddonKind.STREMIO -> "Stremio manifest — https://…/manifest.json"
}

@Composable
private fun AddRepoDialog(kind: AddonKind, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val moovio = LocalMoovioColors.current
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${sectionLabel(kind)} repository") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Repository URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(repoHint(kind), style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary)
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(url.trim()) }, enabled = url.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Explains why an installed Aniyomi/Mihon extension has no live sources on this build. */
@Composable
private fun RuntimeNotice() {
    val moovio = LocalMoovioColors.current
    SurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "Installed, not runnable yet",
                style = MaterialTheme.typography.titleSmall,
                color = moovio.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "This build stores and updates Aniyomi/Mihon extension APKs, but cannot execute " +
                    "their sources yet, so they contribute nothing to search or playback. The " +
                    "runtime needs a newer Android toolchain and is tracked in the roadmap.",
                style = MaterialTheme.typography.bodySmall,
                color = moovio.textSecondary,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = LocalMoovioColors.current.textSecondary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun CountBadge(count: Int, highlighted: Boolean) {
    val moovio = LocalMoovioColors.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background((if (highlighted) moovio.accent else moovio.textTertiary).copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) moovio.accent else moovio.textSecondary,
        )
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
    val shape = CircleShape
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
private fun ExtRow(
    name: String,
    version: String?,
    lang: String?,
    desc: String?,
    nsfw: Boolean = false,
    installed: Boolean = false,
    updateAvailable: Boolean = false,
    onInstall: () -> Unit,
) {
    val moovio = LocalMoovioColors.current
    SurfaceCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        color = moovio.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (nsfw) MetaChip("18+")
                    if (installed) MetaChip(if (updateAvailable) "Update" else "Installed")
                }
                Text(
                    listOfNotNull(version, lang, desc).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = moovio.textTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                updateAvailable -> TextButton(onClick = onInstall) { Text("Update", color = moovio.accent) }
                installed -> Icon(Icons.Filled.Check, "Installed", tint = moovio.accent, modifier = Modifier.padding(12.dp))
                else -> IconButton(onClick = onInstall) {
                    Icon(Icons.Filled.Download, "Install $name", tint = moovio.accent)
                }
            }
        }
    }
}

/** Installed Aniyomi/Mihon extension: enable switch, live indicator, update action, remove. */
@Composable
private fun InstalledExtRow(
    name: String,
    version: String?,
    detail: String?,
    nsfw: Boolean,
    enabled: Boolean,
    live: Boolean,
    updateAvailable: Boolean,
    onToggle: (Boolean) -> Unit,
    onUpdate: () -> Unit,
    onRemove: () -> Unit,
) {
    val moovio = LocalMoovioColors.current
    SurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMedium,
                            color = moovio.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (nsfw) MetaChip("18+")
                        if (live) MetaChip("Live")
                        if (updateAvailable) MetaChip("Update")
                    }
                    Text(
                        listOfNotNull(version, detail).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = moovio.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MoovioSwitch(checked = enabled, onCheckedChange = onToggle)
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (updateAvailable) {
                    TextButton(onClick = onUpdate) { Text("Update", color = moovio.accent) }
                }
                TextButton(onClick = onRemove) { Text("Remove", color = moovio.accent) }
            }
        }
    }
}

@Composable
private fun InstalledRow(name: String, version: String?, lang: String?, onRemove: () -> Unit) {
    val moovio = LocalMoovioColors.current
    SurfaceCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = moovio.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(version, lang).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = moovio.textTertiary,
                )
            }
            TextButton(onClick = onRemove) { Text("Remove", color = moovio.accent) }
        }
    }
}
