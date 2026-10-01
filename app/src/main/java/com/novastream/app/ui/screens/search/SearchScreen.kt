package com.novastream.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.PosterCard
import com.novastream.app.ui.components.TagChip
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.SearchViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

@Composable
fun SearchScreen(nav: NavHostController, initialQuery: String = "") {
    val vm = novaViewModel { SearchViewModel(it) }
    val nova = LocalNovaColors.current
    // Saved across tab switches/process death so returning to Search keeps the term + results.
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val results by vm.results.collectAsStateSafe()
    val loading by vm.loading.collectAsStateSafe()
    val section by vm.section.collectAsStateSafe()

    // Only kick off the initial search when we don't already have results (e.g. restoring the
    // tab) — re-running it would clear the visible list and flash a loading state.
    androidx.compose.runtime.LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && results.isEmpty()) vm.search(initialQuery)
    }

    val resultsListState = rememberLazyListState()

    val filters = listOf(
        null to "All",
        MediaType.MOVIE to "Movies",
        MediaType.SERIES to "TV",
        MediaType.ANIME to "Anime",
        MediaType.MANGA to "Manga",
    )

    Column(Modifier.fillMaxSize()) {
        Text(
            "Search",
            style = MaterialTheme.typography.displaySmall,
            color = nova.textPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 48.dp, bottom = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; vm.search(it) },
            placeholder = { Text("Search movies, TV, anime, manga…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filters) { (type, label) ->
                TagChip(label, selected = section == type) {
                    vm.setSection(type)
                    if (query.isNotBlank()) vm.search(query)
                }
            }
        }
        LazyColumn(
            state = resultsListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (query.isBlank()) {
                item {
                    EmptyState(
                        "Start typing",
                        "Search across TMDB, AniList, MangaDex and your add-ons.",
                        icon = Icons.Filled.Search,
                    )
                }
            } else if (loading && results.isEmpty()) {
                item { EmptyState("Searching…", null, icon = null) }
            } else if (results.isEmpty()) {
                item { EmptyState("No results", "Try another keyword.", icon = Icons.Outlined.SearchOff) }
            } else {
                items(results.chunked(3)) { chunk -> ResultRow(chunk, nav) }
            }
        }
    }
}

@Composable
private fun ResultRow(chunk: List<MediaItem>, nav: NavHostController) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        chunk.forEach { item ->
            PosterCard(item, { nav.navigate(Routes.detail(item)) }, modifier = Modifier.weight(1f), width = 0)
        }
        repeat(3 - chunk.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
    }
}
