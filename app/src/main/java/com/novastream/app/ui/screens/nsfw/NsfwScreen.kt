package com.novastream.app.ui.screens.nsfw

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.LoadingRow
import com.novastream.app.ui.components.MediaRow
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.LocalContainer
import com.novastream.app.ui.vm.NsfwViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel
import kotlinx.coroutines.launch

@Composable
fun NsfwScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val lock by container.settings.nsfwLock.collectAsStateSafe()
    val pin by container.settings.nsfwPin.collectAsStateSafe()
    val biometric by container.settings.nsfwBiometric.collectAsStateSafe()
    val unlocked by container.settings.nsfwUnlocked.collectAsStateSafe()

    if (lock && !unlocked) {
        NsfwLockScreen(
            storedPinHash = pin,
            biometricEnabled = biometric,
            onUnlocked = { scope.launch { container.settings.setNsfwUnlocked(true) } },
        )
        return
    }

    NsfwContent(nav)
}

@Composable
private fun NsfwContent(nav: NavHostController) {
    val vm = novaViewModel { NsfwViewModel(it) }
    val nova = LocalNovaColors.current
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("NSFW Anime", "NSFW Manga", "Real 18+")

    Column(Modifier.fillMaxSize()) {
        Text(
            "NSFW",
            style = MaterialTheme.typography.displaySmall,
            color = nova.textPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 48.dp, bottom = 8.dp),
        )
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
                searching = vm.searching.collectAsStateSafe().value,
                onSearch = { vm.searchAnime(it) },
                emptyHint = "Install NSFW anime add-ons or search AniList/Jikan.",
            )
            1 -> NsfwSection(
                nav = nav,
                rows = vm.mangaRows.collectAsStateSafe().value,
                results = vm.mangaResults.collectAsStateSafe().value,
                loading = vm.mangaLoading.collectAsStateSafe().value,
                searching = vm.searching.collectAsStateSafe().value,
                onSearch = { vm.searchManga(it) },
                emptyHint = "Search MangaDex's adult catalogue.",
            )
            else -> NsfwSection(
                nav = nav,
                rows = vm.realRows.collectAsStateSafe().value,
                results = vm.realResults.collectAsStateSafe().value,
                loading = vm.realLoading.collectAsStateSafe().value,
                searching = vm.realSearching.collectAsStateSafe().value,
                onSearch = { vm.searchReal(it) },
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
    onSearch: (String) -> Unit,
    emptyHint: String,
    errors: List<String> = emptyList(),
    searchErrors: List<String> = emptyList(),
    searching: Boolean = false,
) {
    val nova = LocalNovaColors.current
    var query by remember { mutableStateOf("") }
    // Home shows per-source failures; a non-empty query shows failures only when nothing came
    // back (so an empty search can explain why it is empty).
    val visibleErrors = when {
        query.isBlank() -> errors
        results.isEmpty() && !searching -> searchErrors
        else -> emptyList()
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; onSearch(it) },
            placeholder = { Text("Search…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (visibleErrors.isNotEmpty()) {
            androidx.compose.material3.Surface(
                color = nova.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Some sources are unavailable right now",
                        style = MaterialTheme.typography.labelLarge,
                        color = nova.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    visibleErrors.forEach { err ->
                        Text(
                            "\u2022 $err",
                            style = MaterialTheme.typography.labelMedium,
                            color = nova.textTertiary,
                        )
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (query.isNotBlank()) {
                when {
                    results.isEmpty() && (loading || searching) -> item { LoadingRow() }
                    results.isEmpty() -> item {
                        EmptyState(
                            "No results",
                            if (searchErrors.isNotEmpty()) "Sources are unavailable right now \u2014 see the notice above."
                            else "Try a different query.",
                        )
                    }
                    else -> items(results.chunked(3)) { chunk -> Row3(chunk, nav) }
                }
            } else {
                if (loading && rows.isEmpty()) items(3) { LoadingRow() }
                if (!loading && rows.isEmpty()) item { EmptyState("Nothing here yet", emptyHint) }
                items(rows) { row -> MediaRow(row.title, row.items, onClick = { nav.navigate(Routes.detail(it)) }) }
            }
        }
    }
}

@Composable
private fun Row3(chunk: List<com.novastream.app.data.model.MediaItem>, nav: NavHostController) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) {
        chunk.forEach { item ->
            com.novastream.app.ui.components.PosterCard(
                item = item,
                onClick = { nav.navigate(Routes.detail(item)) },
                modifier = Modifier.weight(1f),
                width = 0,
            )
        }
        repeat(3 - chunk.size) {
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        }
    }
}
