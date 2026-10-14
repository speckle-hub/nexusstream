package com.novastream.app.ui.screens.nsfw

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.novastream.app.data.adult.AdultSources
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.WatchEntry
import com.novastream.app.ui.components.ConfirmRemoveDialog
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.components.LoadingRow
import com.novastream.app.ui.components.MediaRow
import com.novastream.app.ui.components.NovaSwitch
import com.novastream.app.ui.components.PosterCard
import com.novastream.app.ui.components.PosterQuickActionsSheet
import com.novastream.app.ui.components.SearchField
import com.novastream.app.ui.components.SectionHeader
import com.novastream.app.ui.components.TagChip
import com.novastream.app.ui.nav.LocalFloatingNavBottomPadding
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.LocalContainer
import com.novastream.app.ui.vm.NsfwViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel
import kotlinx.coroutines.launch

/**
 * The isolated NSFW Hub — every adult section (NSFW Anime, NSFW Manga, Real 18+) lives here.
 *
 * It is deliberately **not** a bottom-bar tab: it is reachable only by route, which the Settings
 * screen opens from "Content Restrictions → Open NSFW Hub" (and which can require the PIN/biometric
 * lock configured there). Because nothing else mounts it, the adult grid and rails can never appear
 * in the public Home / Browse / Library surfaces, and the general Search screen only consults NSFW
 * add-ons if the user explicitly opts in with the toggle below.
 *
 * Phase 28: the hub opens on a **Private** tab — the isolated adult Continue Watching / Continue
 * Reading rails plus the private management tools (clear adult history, clear bookmarks, the
 * search opt-in) — with the three browse sections behind their own tabs.
 *
 * This file is the former `NsfwScreen`, moved off the Browse segmented control (which now shows
 * Manga and Anime) and given a back affordance plus the global-search opt-in.
 */
@Composable
fun NsfwHubScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val lock by container.settings.nsfwLock.collectAsStateSafe()
    val pin by container.settings.nsfwPin.collectAsStateSafe()
    val biometric by container.settings.nsfwBiometric.collectAsStateSafe()
    val unlocked by container.settings.nsfwUnlocked.collectAsStateSafe()
    val locked = lock && !unlocked

    // navigationBarsPadding: route-only hub with no bottom tab bar (LocalFloatingNavBottomPadding
    // is 0 here), so the grid must clear the system gesture/nav bar on its own.
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        NsfwHubHeader(
            nav = nav,
            showLock = lock && !locked,
            onLock = { scope.launch { container.settings.setNsfwUnlocked(false) } },
        )
        if (locked) {
            NsfwLockScreen(
                storedPinHash = pin,
                biometricEnabled = biometric,
                onUnlocked = { scope.launch { container.settings.setNsfwUnlocked(true) } },
                onSetPin = { raw ->
                    scope.launch {
                        container.settings.setNsfwPin(hashPin(raw))
                        container.settings.setNsfwUnlocked(true)
                    }
                },
            )
            return@Column
        }
        NsfwContent(nav)
    }
}

/** Title row with a Back affordance, since the hub is a pushed destination (no bottom bar). */
@Composable
private fun NsfwHubHeader(
    nav: NavHostController,
    showLock: Boolean = false,
    onLock: () -> Unit = {},
) {
    val nova = LocalNovaColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = AppSpacing.screen)
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { nav.popBackStack() }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Back",
                tint = nova.textPrimary,
            )
        }
        Text(
            "NSFW Hub",
            style = MaterialTheme.typography.headlineSmall,
            color = nova.textPrimary,
            modifier = Modifier.weight(1f),
        )
        // Private pill: the hub's visual signature — dark surface, purple accent text.
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(nova.accent.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, null, tint = nova.accent, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Private",
                    style = MaterialTheme.typography.labelSmall,
                    color = nova.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (showLock) {
            IconButton(onClick = onLock) {
                Icon(Icons.Filled.Lock, "Lock now", tint = nova.textSecondary)
            }
        }
    }
}

@Composable
private fun NsfwContent(nav: NavHostController) {
    val vm = novaViewModel { NsfwViewModel(it) }
    val nova = LocalNovaColors.current
    // Saved, not just remembered: opening a detail page disposes this destination, and coming
    // back used to reset the sub-tab to 0 (NSFW Anime) — losing the Real 18+ position.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // One saved query per sub-tab, hoisted out of NsfwSection on purpose: a conditional branch
    // is disposed when you switch sub-tabs, but this composable stays alive for the whole
    // destination, which is where saved state is actually persisted and restored. Switching
    // NSFW Anime ⇄ Real 18+ therefore keeps each section's term exactly as typed.
    var animeQuery by rememberSaveable { mutableStateOf("") }
    var mangaQuery by rememberSaveable { mutableStateOf("") }
    var realQuery by rememberSaveable { mutableStateOf("") }
    // Phase 36: Real 18+ source filter ("All" + one chip per built-in source).
    var realSource by rememberSaveable { mutableStateOf<String?>(null) }
    val realSourceOptions = remember { AdultSources.all.map { it.id to it.name } }
    // Index 0 is the Private dashboard (isolated history + tools); the browse sections shift by one.
    val titles = listOf("Private", "NSFW Anime", "NSFW Manga", "Real 18+")

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tab,
            containerColor = nova.background,
            contentColor = nova.accent,
            edgePadding = 12.dp,
        ) {
            titles.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }) {
                    Text(t, modifier = Modifier.padding(14.dp), color = if (tab == i) nova.accent else nova.textTertiary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            0 -> NsfwPrivateTab(nav = nav, vm = vm, onBrowseTab = { tab = it })
            1 -> NsfwSection(
                nav = nav,
                rows = vm.animeRows.collectAsStateSafe().value,
                results = vm.animeResults.collectAsStateSafe().value,
                loading = vm.animeLoading.collectAsStateSafe().value,
                searching = vm.animeSearching.collectAsStateSafe().value,
                query = animeQuery,
                onQueryChange = { animeQuery = it },
                onSearch = { vm.searchAnime(it) },
                searchedFor = vm.animeSearchedFor,
                emptyHint = "Install NSFW anime add-ons or search AniList/Jikan.",
            )
            2 -> NsfwSection(
                nav = nav,
                rows = vm.mangaRows.collectAsStateSafe().value,
                results = vm.mangaResults.collectAsStateSafe().value,
                loading = vm.mangaLoading.collectAsStateSafe().value,
                searching = vm.mangaSearching.collectAsStateSafe().value,
                query = mangaQuery,
                onQueryChange = { mangaQuery = it },
                onSearch = { vm.searchManga(it) },
                searchedFor = vm.mangaSearchedFor,
                emptyHint = "Search MangaDex's adult catalogue.",
            )
            else -> NsfwSection(
                nav = nav,
                rows = vm.realRows.collectAsStateSafe().value,
                results = vm.realResults.collectAsStateSafe().value,
                loading = vm.realLoading.collectAsStateSafe().value,
                searching = vm.realSearching.collectAsStateSafe().value,
                query = realQuery,
                onQueryChange = { realQuery = it },
                onSearch = { vm.searchReal(it) },
                searchedFor = vm.realSearchedFor,
                emptyHint = "Built-in sources unavailable. Install NSFW Stremio add-ons as a fallback.",
                errors = vm.realErrors.collectAsStateSafe().value,
                searchErrors = vm.realSearchErrors.collectAsStateSafe().value,
                sourceOptions = realSourceOptions,
                selectedSource = realSource,
                onSelectSource = { realSource = it },
            )
        }
    }
}

/**
 * The Private dashboard — the only surface where adult playback history and adult bookmarks
 * are visible (Phase 28 isolation). Public Home/Library flows strip every adult entry, so this
 * tab is the single place adult Continue Watching / Continue Reading live, next to the tools
 * that manage them.
 */
@Composable
private fun NsfwPrivateTab(
    nav: NavHostController,
    vm: NsfwViewModel,
    onBrowseTab: (Int) -> Unit,
) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val watching by vm.nsfwContinueWatching.collectAsStateSafe()
    val reading by vm.nsfwContinueReading.collectAsStateSafe()
    val bookmarks by vm.nsfwFavorites.collectAsStateSafe()
    val globalSearch by container.settings.nsfwGlobalSearch.collectAsStateSafe()
    val autoNsfw by container.settings.autoNsfwFromAddons.collectAsStateSafe()

    var confirmClearHistory by rememberSaveable { mutableStateOf(false) }
    var confirmClearBookmarks by rememberSaveable { mutableStateOf(false) }
    var pendingWatchRemoval by remember { mutableStateOf<WatchEntry?>(null) }
    var pendingBookmarkRemoval by remember { mutableStateOf<MediaItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().clipToBounds(),
        contentPadding = PaddingValues(bottom = 24.dp + LocalFloatingNavBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            NsfwStatusCard(
                historyCount = watching.size + reading.size,
                onLock = { scope.launch { container.settings.setNsfwUnlocked(false) } },
            )
        }

        if (watching.isNotEmpty()) {
            item {
                NsfwHistoryRail(
                    title = "Continue Watching",
                    reading = false,
                    entries = watching,
                    onOpen = { nav.navigate(Routes.detail(it.item)) },
                    onRemove = { pendingWatchRemoval = it },
                    onClearAll = { confirmClearHistory = true },
                )
            }
        }
        if (reading.isNotEmpty()) {
            item {
                NsfwHistoryRail(
                    title = "Continue Reading",
                    reading = true,
                    entries = reading,
                    onOpen = { nav.navigate(Routes.detail(it.item)) },
                    onRemove = { pendingWatchRemoval = it },
                    onClearAll = { confirmClearHistory = true },
                )
            }
        }
        if (bookmarks.isNotEmpty()) {
            item {
                NsfwBookmarksRail(
                    items = bookmarks,
                    onOpen = { nav.navigate(Routes.detail(it)) },
                    onRemove = { pendingBookmarkRemoval = it },
                    onClearAll = { confirmClearBookmarks = true },
                )
            }
        }
        if (watching.isEmpty() && reading.isEmpty() && bookmarks.isEmpty()) {
            item {
                EmptyState(
                    "Nothing private yet",
                    "Adult titles you watch or save appear here — never on Home or in the public Library.",
                    icon = Icons.Filled.History,
                )
            }
        }

        item {
            NsfwBrowseShortcuts(onOpen = onBrowseTab)
        }

        item {
            NsfwManageCard(
                globalSearch = globalSearch,
                onGlobalSearch = { scope.launch { container.settings.setNsfwGlobalSearch(it) } },
                autoNsfw = autoNsfw,
                onAutoNsfw = { scope.launch { container.settings.setAutoNsfwFromAddons(it) } },
                historyCount = watching.size + reading.size,
                bookmarksCount = bookmarks.size,
                onClearHistory = { confirmClearHistory = true },
                onClearBookmarks = { confirmClearBookmarks = true },
            )
        }
    }

    pendingWatchRemoval?.let { entry ->
        ConfirmRemoveDialog(
            title = "Remove from Continue Watching?",
            message = "\u201c${entry.item.title}\u201d will be removed and its saved progress cleared.",
            onConfirm = { vm.removeNsfwWatch(entry.item.key); pendingWatchRemoval = null },
            onDismiss = { pendingWatchRemoval = null },
        )
    }
    pendingBookmarkRemoval?.let { item ->
        ConfirmRemoveDialog(
            title = "Remove bookmark?",
            message = "\u201c${item.title}\u201d will be removed from your private bookmarks.",
            onConfirm = { vm.removeNsfwFavorite(item); pendingBookmarkRemoval = null },
            onDismiss = { pendingBookmarkRemoval = null },
        )
    }
    if (confirmClearHistory) {
        ConfirmRemoveDialog(
            title = "Clear adult history?",
            message = "All adult Continue Watching and Continue Reading entries and their saved progress will be removed. Your public history is untouched.",
            onConfirm = { vm.clearNsfwHistory(); confirmClearHistory = false },
            onDismiss = { confirmClearHistory = false },
        )
    }
    if (confirmClearBookmarks) {
        ConfirmRemoveDialog(
            title = "Clear private bookmarks?",
            message = "All adult bookmarks will be removed. Your public favourites are untouched.",
            onConfirm = { vm.clearNsfwFavorites(); confirmClearBookmarks = false },
            onDismiss = { confirmClearBookmarks = false },
        )
    }
}

/** Unlocked-state banner: purple-accented card marking the area as private, with a Lock action. */
@Composable
private fun NsfwStatusCard(historyCount: Int, onLock: () -> Unit) {
    val nova = LocalNovaColors.current
    GlassSurface(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 6.dp)) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(nova.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, null, tint = nova.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Private area unlocked",
                    style = MaterialTheme.typography.titleSmall,
                    color = nova.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (historyCount == 0) "Adult history stays inside this hub."
                    else "$historyCount adult title${if (historyCount == 1) "" else "s"} in progress — hidden everywhere else.",
                    style = MaterialTheme.typography.labelSmall,
                    color = nova.textTertiary,
                )
            }
            TextButton(onClick = onLock) { Text("Lock") }
        }
    }
}

/** Horizontal progress cards for the hub's isolated history (mirrors the Home rail, hub-local). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NsfwHistoryRail(
    title: String,
    reading: Boolean,
    entries: List<WatchEntry>,
    onOpen: (WatchEntry) -> Unit,
    onRemove: (WatchEntry) -> Unit,
    onClearAll: () -> Unit,
) {
    val nova = LocalNovaColors.current
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            title,
            trailing = { TextButton(onClick = onClearAll) { Text("Clear") } },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = AppSpacing.screen),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { it.item.key }) { entry ->
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .combinedClickable(onClick = { onOpen(entry) }, onLongClick = { onRemove(entry) }),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(nova.surfaceElevated),
                    ) {
                        val img = entry.item.backdrop ?: entry.item.poster
                        if (!img.isNullOrBlank()) {
                            AsyncImage(img, entry.item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))),
                        )
                        Icon(
                            if (reading) Icons.AutoMirrored.Filled.MenuBook else Icons.Filled.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(40.dp),
                        )
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.White.copy(alpha = 0.25f)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(entry.progress.coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .background(nova.accent),
                            )
                        }
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { onRemove(entry) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.62f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        entry.item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = nova.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.videoTitle?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = nova.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Horizontal rail of isolated adult bookmarks with remove + clear-all management. */
@Composable
private fun NsfwBookmarksRail(
    items: List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    onRemove: (MediaItem) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            "Private bookmarks",
            trailing = { TextButton(onClick = onClearAll) { Text("Clear") } },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = AppSpacing.screen),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.key }) { item ->
                PosterCard(
                    item = item,
                    onClick = { onOpen(item) },
                    width = 110,
                    onLongClick = { onRemove(item) },
                    onRemove = { onRemove(item) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Jump cards into the three browse tabs, so the Private dashboard is the hub's front door. */
@Composable
private fun NsfwBrowseShortcuts(onOpen: (Int) -> Unit) {
    val nova = LocalNovaColors.current
    val entries = listOf(
        Triple("NSFW Anime", "AniList, Jikan and add-on catalogues", 1),
        Triple("NSFW Manga", "MangaDex adult catalogue", 2),
        Triple("Real 18+", "Built-in sources and NSFW add-ons", 3),
    )
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.forEach { (title, subtitle, index) ->
            GlassSurface(Modifier.fillMaxWidth(), onClick = { onOpen(index) }) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = nova.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = nova.textTertiary)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = nova.accent, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * Quick management tools for the private library: the isolation opt-ins plus destructive
 * history/bookmark clears (each behind a confirmation dialog owned by [NsfwPrivateTab]).
 */
@Composable
private fun NsfwManageCard(
    globalSearch: Boolean,
    onGlobalSearch: (Boolean) -> Unit,
    autoNsfw: Boolean,
    onAutoNsfw: (Boolean) -> Unit,
    historyCount: Int,
    bookmarksCount: Int,
    onClearHistory: () -> Unit,
    onClearBookmarks: () -> Unit,
) {
    val nova = LocalNovaColors.current
    GlassSurface(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Private library tools",
                style = MaterialTheme.typography.titleSmall,
                color = nova.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            // The isolation opt-in. Off by default, so the general Search screen and Home feed never
            // surface adult content/extensions unless the user turns this on inside the hub.
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Show NSFW in general search",
                        style = MaterialTheme.typography.bodyMedium,
                        color = nova.textPrimary,
                    )
                    Text(
                        "Off means Search stays adult-free. Adult add-ons are always available in this hub.",
                        style = MaterialTheme.typography.labelSmall,
                        color = nova.textTertiary,
                    )
                }
                Spacer(Modifier.size(8.dp))
                NovaSwitch(checked = globalSearch, onCheckedChange = onGlobalSearch)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Include NSFW add-ons in Real 18+",
                        style = MaterialTheme.typography.bodyMedium,
                        color = nova.textPrimary,
                    )
                    Text(
                        "Off means Real 18+ uses built-in sources only.",
                        style = MaterialTheme.typography.labelSmall,
                        color = nova.textTertiary,
                    )
                }
                Spacer(Modifier.size(8.dp))
                NovaSwitch(checked = autoNsfw, onCheckedChange = onAutoNsfw)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClearHistory,
                enabled = historyCount > 0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = nova.surfaceElevated,
                    disabledContentColor = nova.textTertiary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.History, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear adult history ($historyCount)")
            }
            Button(
                onClick = onClearBookmarks,
                enabled = bookmarksCount > 0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = nova.surfaceElevated,
                    disabledContentColor = nova.textTertiary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Favorite, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear private bookmarks ($bookmarksCount)")
            }
        }
    }
}

@Composable
private fun NsfwSection(
    nav: NavHostController,
    rows: List<com.novastream.app.data.model.CatalogRow>,
    results: List<com.novastream.app.data.model.MediaItem>,
    loading: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    emptyHint: String,
    errors: List<String> = emptyList(),
    searchErrors: List<String> = emptyList(),
    searching: Boolean = false,
    /** The query this section's ViewModel last searched for (`null` = never, i.e. fresh VM). */
    searchedFor: String? = null,
    /**
     * Phase 36 source-filter chips ("All" + one per entry). Empty by default, so the
     * anime/manga sections render exactly as before; Real 18+ passes its built-ins.
     */
    sourceOptions: List<Pair<String, String>> = emptyList(),
    selectedSource: String? = null,
    onSelectSource: (String?) -> Unit = {},
) {
    val nova = LocalNovaColors.current
    val listState = rememberLazyGridState()
    var actionsFor by remember { mutableStateOf<com.novastream.app.data.model.MediaItem?>(null) }

    // Process-death restore: the query survived in saved state but this section's ViewModel was
    // recreated empty. Re-run the search exactly once so the results come back with the term.
    // Guarded by `searchedFor != query`, so it never fires on keystrokes or when the ViewModel
    // already holds these results — a legitimately empty result is never re-queried on a
    // sub-tab switch (the ViewModel outlives those, unlike this composable).
    LaunchedEffect(Unit) {
        if (query.isNotBlank() && query != searchedFor && results.isEmpty() && !searching) {
            onSearch(query)
        }
    }

    // Home shows per-source failures; a non-empty query shows failures only when nothing came
    // back (so an empty search can explain why it is empty).
    val visibleErrors = when {
        query.isBlank() -> errors
        results.isEmpty() && !searching -> searchErrors
        else -> emptyList()
    }
    // Phase 36 source filter: applied to both rails and search results before render.
    val (visibleRows, visibleResults) = remember(rows, results, selectedSource) {
        applySourceFilter(rows, results, selectedSource)
    }
    // Compact, dismissible notice. Hidden until the user refreshes to new errors (keyed on the
    // error set), and collapsed to a single line by default so it never dominates the page.
    var errorsHidden by rememberSaveable { mutableStateOf(false) }
    var errorsExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(visibleErrors) { errorsHidden = false }

    Column(Modifier.fillMaxSize()) {
        SearchField(
            value = query,
            onValueChange = { onQueryChange(it); onSearch(it) },
            placeholder = "Search…",
            searching = searching,
            onClear = { onQueryChange(""); onSearch("") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 8.dp),
        )
        // Phase 36 source-filter chips: "All" plus one chip per built-in source for
        // instantaneous per-site narrowing of rails and search results.
        if (sourceOptions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = AppSpacing.screen),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    TagChip(
                        text = "All",
                        selected = selectedSource == null,
                        onClick = { onSelectSource(null) },
                    )
                }
                items(sourceOptions.size) { i ->
                    val (id, name) = sourceOptions[i]
                    TagChip(
                        text = name,
                        selected = selectedSource == id,
                        onClick = { onSelectSource(if (selectedSource == id) null else id) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (visibleErrors.isNotEmpty() && !errorsHidden) {
            Surface(
                color = nova.outline.copy(alpha = 0.20f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 4.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { errorsExpanded = !errorsExpanded }
                            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (visibleErrors.size == 1) "1 source unavailable"
                            else "${visibleErrors.size} sources unavailable",
                            style = MaterialTheme.typography.labelMedium,
                            color = nova.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { errorsHidden = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = nova.textTertiary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    if (errorsExpanded) {
                        visibleErrors.forEach { err ->
                            Text(
                                "\u2022 $err",
                                style = MaterialTheme.typography.labelSmall,
                                color = nova.textTertiary,
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 2.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        // One adaptive grid for both rails and posters, matching Search / Library. Results used to
        // be faked with `chunked(3)` + Rows, which pinned every screen to exactly 3 columns.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = AppSpacing.posterMin),
            state = listState,
            // Hard-clip the viewport so a poster can't paint over the search field / tab row above it.
            modifier = Modifier.fillMaxSize().clipToBounds(),
            contentPadding = PaddingValues(
                start = AppSpacing.screen,
                end = AppSpacing.screen,
                bottom = 24.dp + LocalFloatingNavBottomPadding.current,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.item),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.item),
        ) {
            if (query.isNotBlank()) {
                when {
                    visibleResults.isEmpty() && (loading || searching) ->
                        item(span = { GridItemSpan(maxLineSpan) }) { LoadingRow() }
                    visibleResults.isEmpty() ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            "No results",
                            if (selectedSource != null) "No results from this source — try All or a different query."
                            else if (searchErrors.isNotEmpty()) "Sources are unavailable right now \u2014 see the notice above."
                            else "Try a different query.",
                        )
                    }
                    else -> items(visibleResults, key = { it.key }) { item ->
                        PosterCard(
                            item = item,
                            onClick = { nav.navigate(Routes.detail(item)) },
                            modifier = Modifier.fillMaxWidth(),
                            width = 0,
                            onLongClick = { actionsFor = item },
                        )
                    }
                }
            } else {
                if (loading && visibleRows.isEmpty()) {
                    items(3, span = { GridItemSpan(maxLineSpan) }) { LoadingRow() }
                }
                if (!loading && visibleRows.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("Nothing here yet", emptyHint) }
                }
                items(visibleRows, span = { GridItemSpan(maxLineSpan) }) { row ->
                    // Same card width as the Home feed: `MediaRow`'s 132 dp default made every
                    // NSFW/Real 18+ rail narrower than Home's, which is what read as "narrow cards"
                    // next to the edge-to-edge Home rows.
                    MediaRow(
                        row.title,
                        row.items,
                        onClick = { nav.navigate(Routes.detail(it)) },
                        cardWidth = AppSpacing.railCard,
                    )
                }
            }
        }
    }

    actionsFor?.let { target ->
        PosterQuickActionsSheet(
            item = target,
            onDismiss = { actionsFor = null },
            onOpen = { nav.navigate(Routes.detail(target)); actionsFor = null },
        )
    }
}
