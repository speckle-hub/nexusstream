package com.novastream.app.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.PosterCard
import com.novastream.app.ui.components.PosterCardSkeleton
import com.novastream.app.ui.components.PosterQuickActionsSheet
import com.novastream.app.ui.components.SearchField
import com.novastream.app.ui.components.TagChip
import com.novastream.app.ui.nav.LocalFloatingNavBottomPadding
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.SearchViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

/**
 * Curated starter queries.
 *
 * The empty state used to be a single centred "Start typing" block — a dead end that gave a new
 * user nothing to do. These give the screen somewhere to go, and double as search history for
 * anyone who hasn't searched yet.
 */
private val trendingTerms = listOf(
    "Action", "Comedy", "Drama", "Sci-Fi", "Fantasy", "Horror", "Romance", "Thriller",
)

@Composable
fun SearchScreen(nav: NavHostController, initialQuery: String = "") {
    val vm = novaViewModel { SearchViewModel(it) }
    val nova = LocalNovaColors.current
    // Saved across tab switches/process death so returning to Search keeps the term + results.
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val results by vm.results.collectAsStateSafe()
    val loading by vm.loading.collectAsStateSafe()
    val section by vm.section.collectAsStateSafe()
    val recents by vm.recentSearches.collectAsStateSafe()

    // Only kick off the initial search when we don't already have results (e.g. restoring the
    // tab) — re-running it would clear the visible list and flash a loading state.
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && results.isEmpty()) vm.search(initialQuery, remember = true)
    }

    val resultsListState = rememberLazyGridState()
    var actionsFor by remember { mutableStateOf<com.novastream.app.data.model.MediaItem?>(null) }
    var sortLabel by rememberSaveable { mutableStateOf("Relevant") }
    val sortedResults = remember(results, sortLabel) {
        when (sortLabel) {
            "Title" -> results.sortedBy { it.title.lowercase() }
            "Year" -> results.sortedByDescending { it.year.orEmpty() }
            "Rating" -> results.sortedByDescending { it.rating ?: 0.0 }
            else -> results
        }
    }

    val filters = listOf(
        null to "All",
        MediaType.MOVIE to "Movies",
        MediaType.SERIES to "TV",
        MediaType.ANIME to "Anime",
        MediaType.MANGA to "Manga",
    )

    /** Run a query the user picked from a chip — remembered, unlike live keystrokes. */
    fun runQuery(term: String) {
        query = term
        vm.search(term, remember = true)
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.zIndex(10f).background(nova.background).statusBarsPadding()) {
        Text(
            "Search",
            style = MaterialTheme.typography.displaySmall,
            color = nova.textPrimary,
            modifier = Modifier.padding(start = AppSpacing.screen, top = 8.dp, bottom = 8.dp),
        )
        SearchField(
            value = query,
            // Update the local (saveable) query *and* dispatch the debounced search. Setting the
            // state is what actually makes the typed characters appear in the field; without it
            // the field stayed visually empty and every keystroke looked "dead".
            onValueChange = { query = it; vm.search(it) },
            placeholder = "Movies, TV, anime, manga…",
            searching = loading,
            onClear = {
                query = ""
                vm.search("")
            },
            modifier = Modifier.padding(horizontal = AppSpacing.screen),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = AppSpacing.screen, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filters) { (type, label) ->
                TagChip(label, selected = section == type) {
                    vm.setSection(type)
                    if (query.isNotBlank()) vm.search(query, remember = true)
                }
            }
        }
        }

        // Sort options for the result set. "Relevant" keeps the add-on merge order.
        if (results.isNotEmpty()) {
            val sortOptions = listOf("Relevant", "Title", "Year", "Rating")
            LazyRow(
                contentPadding = PaddingValues(horizontal = AppSpacing.screen, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sortOptions) { label ->
                    TagChip(label, selected = sortLabel == label) { sortLabel = label }
                }
            }
        }

        // One adaptive grid for results. Previously results were faked with `chunked(3)` + Rows,
        // which pinned every screen to exactly 3 columns and made Library/Home/Search cards three
        // different sizes.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = AppSpacing.posterMin),
            state = resultsListState,
            // Hard-clip the viewport so a poster can never paint into the header above it.
            modifier = Modifier.fillMaxSize().clipToBounds(),
            contentPadding = PaddingValues(
                start = AppSpacing.screen,
                end = AppSpacing.screen,
                bottom = 24.dp + LocalFloatingNavBottomPadding.current,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.item),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (query.isBlank()) {
                // ---- Empty state: recent searches + trending starter terms --------------
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        "What do you want to watch?",
                        "Search across TMDB, AniList, MangaDex and your add-ons.",
                        icon = Icons.Filled.Search,
                    )
                }
                if (recents.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SuggestionHeader(
                            label = "Recent",
                            icon = Icons.Outlined.History,
                            onClear = vm::clearRecentSearches,
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SuggestionWrap(recents, ::runQuery)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SuggestionHeader(label = "Trending", icon = Icons.Outlined.TrendingUp)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SuggestionWrap(trendingTerms, ::runQuery)
                }
            } else if (loading && results.isEmpty()) {
                // Shimmer skeletons shaped exactly like the PosterCards that replace them, so the
                // grid doesn't visibly reflow when results land.
                items(SKELETON_COUNT) {
                    PosterCardSkeleton(modifier = Modifier.fillMaxWidth(), width = 0)
                }
            } else if (results.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState("No results", "Nothing matched “$query”. Try another keyword.", icon = Icons.Outlined.SearchOff)
                }
            } else {
                items(sortedResults, key = { it.key }) { item ->
                    PosterCard(
                        item = item,
                        onClick = { nav.navigate(Routes.detail(item)) },
                        modifier = Modifier.fillMaxWidth(),
                        width = 0,
                        onLongClick = { actionsFor = item },
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

/** Small icon + label heading above a wrap of suggestion chips, with an optional "Clear". */
@Composable
private fun SuggestionHeader(
    label: String,
    icon: ImageVector,
    onClear: (() -> Unit)? = null,
) {
    val nova = LocalNovaColors.current
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = nova.textTertiary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = nova.textSecondary)
        if (onClear != null) {
            Spacer(Modifier.width(4.dp))
            // A full 48dp touch target with the standard ripple, instead of a bare clickable Text.
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onClear() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelMedium,
                    color = nova.accent,
                )
            }
        }
    }
}

/**
 * Chips that wrap onto as many lines as needed.
 *
 * A `LazyRow` can't wrap, so a single row of suggestions would be all that fits — this lays the
 * terms out in rows of three instead.
 */
@Composable
private fun SuggestionWrap(terms: List<String>, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        terms.chunked(TERMS_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { term ->
                    TagChip(term, onClick = { onPick(term) })
                }
            }
        }
    }
}

/** Number of skeleton cards shown while the first search of a query is in flight. */
private const val SKELETON_COUNT = 9

/** Suggestion chips per wrapped row. */
private const val TERMS_PER_ROW = 3
