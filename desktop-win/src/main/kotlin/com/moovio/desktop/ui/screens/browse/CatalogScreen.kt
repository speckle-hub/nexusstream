package com.moovio.desktop.ui.screens.browse

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.ui.components.CircleIconButton
import com.moovio.desktop.ui.components.EmptyState
import com.moovio.desktop.ui.components.FilterChip
import com.moovio.desktop.ui.components.LoadingIndicator
import com.moovio.desktop.ui.components.PosterCard
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val movieGenres = listOf("Action", "Adventure", "Comedy", "Crime", "Drama", "Sci-Fi", "Thriller", "Animation")
private val seriesGenres = listOf("Drama", "Comedy", "Crime", "Mystery", "Sci-Fi", "Action", "Animation", "Reality")

@Composable
fun CatalogScreen(
    app: AppState,
    container: AppContainer,
    type: MediaType,
    title: String,
    subtitle: String,
) {
    var items by remember(type) { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember(type) { mutableStateOf(true) }
    var error by remember(type) { mutableStateOf<String?>(null) }
    var genre by remember(type) { mutableStateOf<String?>(null) }
    var reload by remember(type) { mutableStateOf(0) }

    val genres = when (type) {
        MediaType.MOVIE -> movieGenres
        MediaType.SERIES -> seriesGenres
        else -> emptyList()
    }

    LaunchedEffect(type, genre, reload) {
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) {
            runCatching { container.catalog.browse(type, genre) }
        }
        result.onSuccess { items = it }
            .onFailure { error = it.message ?: "Could not load catalog" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 40.dp, top = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = Palette.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.textTertiary)
            }
            CircleIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = "Search",
                onClick = { app.openSearch() },
                size = 40.dp,
            )
        }

        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip("All", genre == null) { genre = null }
                }
                items(genres) { g ->
                    FilterChip(g, genre == g) { genre = if (genre == g) null else g }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        when {
            loading && items.isEmpty() -> LoadingIndicator(Modifier.fillMaxSize())
            error != null && items.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Search,
                title = "Couldn't load this catalog",
                message = error ?: "The metadata provider did not respond.",
                actionLabel = "Retry",
                onAction = { reload++ },
                modifier = Modifier.fillMaxSize(),
            )
            items.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Search,
                title = "No titles found",
                message = "Try a different genre.",
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(156.dp),
                contentPadding = PaddingValues(start = 48.dp, end = 48.dp, bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.key }) { item ->
                    PosterCard(
                        item = item,
                        onClick = { app.openDetail(item) },
                        width = 160.dp,
                        onPlay = { app.requestPlay(com.moovio.desktop.domain.model.PlayTarget(media = item)) },
                        progress = container.library.progressFor(item.key)?.fraction,
                    )
                }
            }
        }
    }
}
