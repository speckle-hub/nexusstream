package com.moovio.desktop.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.ui.components.EmptyState
import com.moovio.desktop.ui.components.FilterChip
import com.moovio.desktop.ui.components.LoadingIndicator
import com.moovio.desktop.ui.components.PosterCard
import com.moovio.desktop.ui.components.SearchField
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.glass
import kotlinx.coroutines.delay

private val suggestions = listOf("Dune", "Severance", "Frieren", "The Bear", "One Piece", "Oppenheimer", "Arcane")

@Composable
fun SearchOverlay(
    app: AppState,
    container: AppContainer,
    onOpen: (MediaItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var failures by remember { mutableStateOf<List<String>>(emptyList()) }
    var recent by remember { mutableStateOf<List<String>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searched = false
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(320)
        val outcome = container.catalog.search(query)
        results = outcome.first.filter { !it.adult }
        failures = outcome.second.map { "${it.addon}: ${it.message}" }
        searched = true
        loading = false
        if (query.trim().length >= 2) {
            recent = (listOf(query.trim()) + recent.filterNot { it.equals(query.trim(), true) }).take(6)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(enabled = false) {}
            .padding(top = 64.dp, bottom = 40.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 1180.dp)
                .fillMaxSize()
                .padding(horizontal = 40.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search movies, series and anime…",
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    trailing = {
                        Text(
                            "ESC",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.textTertiary,
                            modifier = Modifier
                                .clickable { app.closeSearch() }
                                .padding(start = 8.dp),
                        )
                    },
                )
                Box(
                    Modifier
                        .size(46.dp)
                        .background(Palette.surfaceHigh, RoundedCornerShape(14.dp))
                        .clickable { app.closeSearch() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, "Close search", Modifier.size(19.dp), tint = Palette.textSecondary)
                }
            }

            Spacer(Modifier.height(22.dp))

            when {
                query.isBlank() -> {
                    Text(
                        "TRY SOMETHING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.textTertiary,
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions) { suggestion ->
                            FilterChip(suggestion, false) { query = suggestion }
                        }
                    }
                    if (recent.isNotEmpty()) {
                        Spacer(Modifier.height(26.dp))
                        Text("RECENT", style = MaterialTheme.typography.labelSmall, color = Palette.textTertiary)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            recent.take(4).forEach { term ->
                                FilterChip(term, false) { query = term }
                            }
                        }
                    }
                    Spacer(Modifier.height(46.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.Search,
                                null,
                                Modifier.size(34.dp),
                                tint = Palette.textTertiary.copy(alpha = 0.55f),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Search across Cinemeta, AniList and your add-ons",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Palette.textTertiary,
                            )
                        }
                    }
                }

                loading && results.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().height(260.dp)) { LoadingIndicator() }
                }

                results.isEmpty() && searched -> EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "No results for “$query”",
                    message = "Check the spelling, or install a metadata add-on in Settings → Add-ons.",
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                )

                else -> {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${results.size} results",
                            style = MaterialTheme.typography.labelMedium,
                            color = Palette.textTertiary,
                        )
                        if (loading) {
                            Spacer(Modifier.width(10.dp))
                            Text("updating…", style = MaterialTheme.typography.labelSmall, color = Palette.textTertiary)
                        }
                        if (failures.isNotEmpty()) {
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "${failures.size} add-on issue${if (failures.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Palette.warning,
                            )
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 40.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(results, key = { index, item -> "${item.key}:$index" }) { _, item ->
                            PosterCard(
                                item = item,
                                width = 152.dp,
                                onClick = {
                                    app.closeSearch()
                                    onOpen(item)
                                },
                                onPlay = {
                                    app.closeSearch()
                                    app.requestPlay(PlayTarget(media = item))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
