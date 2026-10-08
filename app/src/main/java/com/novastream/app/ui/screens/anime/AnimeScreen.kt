package com.novastream.app.ui.screens.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.data.model.MediaItem
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.LoadingRow
import com.novastream.app.ui.components.MediaRow
import com.novastream.app.ui.components.PosterCard
import com.novastream.app.ui.components.PosterQuickActionsSheet
import com.novastream.app.ui.components.SearchField
import com.novastream.app.ui.nav.LocalFloatingNavBottomPadding
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.AnimeViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

/**
 * Browse → Anime.
 *
 * Pulls **only** anime-tagged content: AniList's trending/popular/genre rails, Stremio add-on
 * catalogs mapped to [com.novastream.app.data.model.MediaType.ANIME], and a search over those same
 * SFW-only sources. This is the segment that replaced NSFW on the Browse screen — the combined
 * movie/TV feed is a different destination, and adult content now lives behind the isolated NSFW
 * Hub.
 *
 * @param embedded when true the screen omits its own title row because a host (the Browse tab) is
 *   already drawing one above it.
 */
@Composable
fun AnimeScreen(nav: NavHostController, embedded: Boolean = false) {
    val vm = novaViewModel { AnimeViewModel(it) }
    val nova = LocalNovaColors.current
    val rows by vm.rows.collectAsStateSafe()
    val results by vm.results.collectAsStateSafe()
    val loading by vm.loading.collectAsStateSafe()
    val searching by vm.searching.collectAsStateSafe()

    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyGridState()
    var actionsFor by remember { mutableStateOf<MediaItem?>(null) }

    // Process-death restore: the term survived in saved state but this ViewModel came back empty.
    // Re-run the search once so the results return with the query.
    LaunchedEffect(Unit) {
        if (query.isNotBlank() && query != vm.searchedFor && results.isEmpty() && !searching) {
            vm.search(query)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (!embedded) {
            Text(
                "Anime",
                style = MaterialTheme.typography.displaySmall,
                color = nova.textPrimary,
                modifier = Modifier.statusBarsPadding().padding(start = AppSpacing.screen, top = 8.dp, bottom = 8.dp),
            )
        }
        SearchField(
            value = query,
            onValueChange = { query = it; vm.search(it) },
            placeholder = "Search anime…",
            searching = searching,
            onClear = { query = ""; vm.search("") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 8.dp),
        )

        // One adaptive grid for both the browse rails and search results, matching Search / Library.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = AppSpacing.posterMin),
            state = listState,
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
                            EmptyState("No results", "Try a different query.")
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
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            "No anime found",
                            "Check your connection and try again.",
                            actionLabel = "Retry",
                            onAction = { vm.refresh() },
                        )
                    }
                }
                items(rows, span = { GridItemSpan(maxLineSpan) }) { row ->
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
