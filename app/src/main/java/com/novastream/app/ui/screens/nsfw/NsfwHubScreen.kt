package com.novastream.app.ui.screens.nsfw

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.LoadingRow
import com.novastream.app.ui.components.MediaRow
import com.novastream.app.ui.components.NovaSwitch
import com.novastream.app.ui.components.PosterCard
import com.novastream.app.ui.components.PosterQuickActionsSheet
import com.novastream.app.ui.components.SearchField
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

    Column(Modifier.fillMaxSize()) {
        NsfwHubHeader(nav)
        if (lock && !unlocked) {
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
private fun NsfwHubHeader(nav: NavHostController) {
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
    }
}

@Composable
private fun NsfwContent(nav: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val vm = novaViewModel { NsfwViewModel(it) }
    val nova = LocalNovaColors.current
    val globalSearch by container.settings.nsfwGlobalSearch.collectAsStateSafe()
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
    val titles = listOf("NSFW Anime", "NSFW Manga", "Real 18+")

    Column(Modifier.fillMaxSize()) {
        // The isolation opt-in. Off by default, so the general Search screen and Home feed never
        // surface adult content/extensions unless the user turns this on inside the hub.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screen, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Show NSFW in general search",
                    style = MaterialTheme.typography.titleSmall,
                    color = nova.textPrimary,
                )
                Text(
                    "Off means Search stays adult-free. Adult add-ons are always available in this hub.",
                    style = MaterialTheme.typography.labelSmall,
                    color = nova.textTertiary,
                )
            }
            Spacer(Modifier.size(8.dp))
            NovaSwitch(
                checked = globalSearch,
                onCheckedChange = { scope.launch { container.settings.setNsfwGlobalSearch(it) } },
            )
        }

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
            0 -> NsfwSection(
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
            1 -> NsfwSection(
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
            )
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
                    results.isEmpty() && (loading || searching) ->
                        item(span = { GridItemSpan(maxLineSpan) }) { LoadingRow() }
                    results.isEmpty() ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            "No results",
                            if (searchErrors.isNotEmpty()) "Sources are unavailable right now \u2014 see the notice above."
                            else "Try a different query.",
                        )
                    }
                    else -> items(results, key = { it.key }) { item ->
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
                if (loading && rows.isEmpty()) {
                    items(3, span = { GridItemSpan(maxLineSpan) }) { LoadingRow() }
                }
                if (!loading && rows.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("Nothing here yet", emptyHint) }
                }
                items(rows, span = { GridItemSpan(maxLineSpan) }) { row ->
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
