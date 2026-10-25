package com.moovio.desktop.ui.screens.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.ui.components.CircleIconButton
import com.moovio.desktop.ui.components.ConfirmDialog
import com.moovio.desktop.ui.components.ContinueCard
import com.moovio.desktop.ui.components.EmptyState
import com.moovio.desktop.ui.components.HeroCarousel
import com.moovio.desktop.ui.components.LoadingIndicator
import com.moovio.desktop.ui.components.MediaRow
import com.moovio.desktop.ui.components.SectionHeader
import com.moovio.desktop.ui.components.formatDuration
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.glass

@Composable
fun HomeScreen(
    app: AppState,
    container: AppContainer,
    onOpen: (MediaItem) -> Unit,
) {
    val viewModel = remember { HomeViewModel(container) }
    DisposableEffect(viewModel) { onDispose { viewModel.dispose() } }
    val state by viewModel.state.collectAsState()
    val library by container.library.state.collectAsState()
    var pendingRemove by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    val continueWatching = library.progress
        .filter { !it.adult && !it.finished && it.positionMs > 15_000 }
        .sortedByDescending { it.updatedAt }
        .take(12)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 56.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item("hero") {
                Box {
                    HeroCarousel(
                        items = state.hero,
                        onOpen = onOpen,
                        onPlay = { media -> app.requestPlay(PlayTarget(media = media)) },
                        isInLibrary = { container.library.isFavorite(it) },
                        onToggleLibrary = { media ->
                            val added = container.library.toggleFavorite(media)
                            app.showToast(if (added) "Added to Library" else "Removed from Library")
                        },
                    )
                    HomeTopBar(
                        onSearch = { app.openSearch() },
                        onRefresh = { viewModel.refresh() },
                        loading = state.loading,
                    )
                }
            }

            if (continueWatching.isNotEmpty()) {
                item("continue") {
                    Column(Modifier.padding(top = 30.dp)) {
                        Box(Modifier.padding(horizontal = 48.dp)) {
                            SectionHeader(
                                title = "Continue Watching",
                                subtitle = "${continueWatching.size} title${if (continueWatching.size == 1) "" else "s"} in progress",
                                trailing = {
                                    CircleIconButton(
                                        icon = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh",
                                        onClick = { viewModel.refresh() },
                                        size = 30.dp,
                                    )
                                },
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(continueWatching, key = { it.mediaKey }) { progress ->
                                val subtitle = when {
                                    progress.episodeTitle != null -> progress.episodeTitle
                                    progress.season != null && progress.episode != null ->
                                        "S${progress.season} · E${progress.episode}"
                                    else -> formatDuration(progress.positionMs) + " watched"
                                }
                                ContinueCard(
                                    name = progress.name,
                                    subtitle = subtitle.orEmpty(),
                                    imageUrl = progress.background ?: progress.poster,
                                    progress = progress.fraction,
                                    onClick = {
                                        app.requestPlay(
                                            PlayTarget(
                                                media = MediaItem(
                                                    id = progress.mediaId,
                                                    type = progress.type,
                                                    name = progress.name,
                                                    poster = progress.poster,
                                                    background = progress.background,
                                                    adult = progress.adult,
                                                ),
                                                season = progress.season,
                                                episode = progress.episode,
                                                episodeTitle = progress.episodeTitle,
                                                startPositionMs = progress.positionMs,
                                            ),
                                        )
                                    },
                                    onRemove = { pendingRemove = progress.mediaKey },
                                )
                            }
                        }
                    }
                }
            }

            item("rows-start") { Spacer(Modifier.height(34.dp)) }

            items(state.rows, key = { it.id }) { row ->
                MediaRow(
                    title = row.title,
                    items = row.items.filter { !it.adult },
                    onOpen = onOpen,
                    onPlay = { media -> app.requestPlay(PlayTarget(media = media)) },
                    modifier = Modifier.padding(bottom = 30.dp),
                    progressFor = { media ->
                        container.library.progressFor(media.key)?.takeIf { !it.finished }?.fraction
                    },
                )
            }

            if (state.loading && state.rows.isEmpty()) {
                item("loading") {
                    LoadingIndicator(Modifier.fillMaxWidth().height(220.dp))
                }
            }

            if (state.isEmpty) {
                item("empty") {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = "Nothing to show yet",
                        message = state.error ?: "No catalogs responded. Check your connection or add a metadata add-on in Settings.",
                        actionLabel = "Try again",
                        onAction = { viewModel.refresh() },
                        modifier = Modifier.height(340.dp),
                    )
                }
            }

            if (state.rows.isNotEmpty()) {
                item("tail") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Metadata by Cinemeta, AniList and your installed add-ons",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.textTertiary,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.loading && state.hero.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 96.dp, end = 56.dp),
        ) {
            Box(
                Modifier
                    .height(3.dp)
                    .width(120.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Transparent, Palette.textTertiary, Color.Transparent)),
                    ),
            )
        }
    }

    pendingRemove?.let { key ->
        val progress = library.progress.firstOrNull { it.mediaKey == key }
        ConfirmDialog(
            title = "Remove from Continue Watching?",
            message = "“${progress?.name ?: "This title"}” will be removed from your history on this PC.",
            confirmLabel = "Remove",
            onConfirm = {
                container.library.removeProgress(key)
                pendingRemove = null
            },
            onDismiss = { pendingRemove = null },
        )
    }
}

@Composable
private fun HomeTopBar(
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    loading: Boolean,
) {
    val hour = remember { java.time.LocalTime.now().hour }
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 40.dp, top = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                greeting,
                style = MaterialTheme.typography.labelLarge,
                color = Palette.textSecondary,
            )
            Text(
                "What are we watching tonight?",
                style = MaterialTheme.typography.titleMedium,
                color = Palette.textPrimary.copy(alpha = 0.9f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                Text("Updating", style = MaterialTheme.typography.labelSmall, color = Palette.textTertiary)
                Spacer(Modifier.width(2.dp))
            }
            CircleIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh feed",
                onClick = onRefresh,
                size = 38.dp,
            )
            Row(
                Modifier
                    .width(190.dp)
                    .height(38.dp)
                    .glass(RoundedCornerShape(12.dp), alpha = 0.05f)
                    .clickable(onClick = onSearch)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Search, null, Modifier.size(15.dp), tint = Palette.textTertiary)
                Text(
                    "Search",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.textTertiary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Ctrl K",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.textTertiary,
                )
            }
        }
    }
}
