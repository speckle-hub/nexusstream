package com.moovio.desktop.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.domain.model.WatchProgress
import com.moovio.desktop.ui.components.ConfirmDialog
import com.moovio.desktop.ui.components.EmptyState
import com.moovio.desktop.ui.components.FilterChip
import com.moovio.desktop.ui.components.GhostButton
import com.moovio.desktop.ui.components.PosterCard
import com.moovio.desktop.ui.components.PrimaryButton
import com.moovio.desktop.ui.components.ProgressLine
import com.moovio.desktop.ui.components.RemoteImage
import com.moovio.desktop.ui.components.SectionHeader
import com.moovio.desktop.ui.components.formatDuration
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.glass

@Composable
fun LibraryScreen(
    app: AppState,
    container: AppContainer,
    onOpen: (MediaItem) -> Unit,
) {
    val library by container.library.state.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var pendingRemoveMedia by remember { mutableStateOf<MediaItem?>(null) }
    var pendingRemoveProgress by remember { mutableStateOf<WatchProgress?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val favorites = library.favorites.filter { !it.adult }
    val history = library.progress.filter { !it.adult && !it.finished && it.positionMs > 15_000 }
        .sortedByDescending { it.updatedAt }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Library", style = MaterialTheme.typography.headlineLarge, color = Palette.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${favorites.size} saved · ${history.size} in progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.textTertiary,
                )
            }
            if (history.isNotEmpty()) {
                GhostButton("Clear history", Icons.Rounded.DeleteOutline, onClick = { confirmClear = true })
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(Modifier.padding(horizontal = 48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Favorites", tab == 0) { tab = 0 }
            FilterChip("Continue Watching", tab == 1) { tab = 1 }
        }
        Spacer(Modifier.height(22.dp))

        when (tab) {
            0 -> if (favorites.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.BookmarkBorder,
                    title = "Your library is empty",
                    message = "Use the + button on any detail page to keep a title here. Favorites are stored locally on this PC.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(start = 48.dp, end = 48.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(favorites, key = { it.key }) { item ->
                        LibraryFavoriteCard(
                            item = item,
                            progress = container.library.progressFor(item.key)?.takeIf { !it.finished }?.fraction,
                            onClick = { onOpen(item) },
                            onPlay = { app.requestPlay(PlayTarget(media = item)) },
                            onRemove = { pendingRemoveMedia = item },
                        )
                    }
                }
            }

            else -> if (history.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.PlayArrow,
                    title = "Nothing in progress",
                    message = "Start a movie or episode and it will appear here so you can pick up exactly where you left off.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 48.dp, end = 48.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(history, key = { it.mediaKey }) { progress ->
                        HistoryRow(
                            progress = progress,
                            onResume = {
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
                            onRemove = { pendingRemoveProgress = progress },
                        )
                    }
                }
            }
        }
    }

    pendingRemoveMedia?.let { item ->
        ConfirmDialog(
            title = "Remove from Library?",
            message = "“${item.name}” and its progress will be removed from this PC.",
            onConfirm = {
                container.library.remove(item)
                pendingRemoveMedia = null
            },
            onDismiss = { pendingRemoveMedia = null },
        )
    }

    pendingRemoveProgress?.let { progress ->
        ConfirmDialog(
            title = "Remove from Continue Watching?",
            message = "“${progress.name}” will be removed from your history.",
            onConfirm = {
                container.library.removeProgress(progress.mediaKey)
                pendingRemoveProgress = null
            },
            onDismiss = { pendingRemoveProgress = null },
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear Continue Watching?",
            message = "All watch progress will be deleted. Favorites stay untouched.",
            confirmLabel = "Clear history",
            onConfirm = {
                container.library.clearHistory()
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun LibraryFavoriteCard(
    item: MediaItem,
    progress: Float?,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    Box(Modifier.hoverable(source)) {
        PosterCard(
            item = item,
            onClick = onClick,
            onPlay = onPlay,
            progress = progress,
            width = 160.dp,
        )
        if (hovered) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(26.dp)
                    .shadow(8.dp, RoundedCornerShape(50))
                    .background(Palette.background.copy(alpha = 0.86f), RoundedCornerShape(50))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, "Remove", Modifier.size(14.dp), tint = Palette.textSecondary)
            }
        }
    }
}

@Composable
private fun HistoryRow(progress: WatchProgress, onResume: () -> Unit, onRemove: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(14.dp), alpha = if (hovered) 0.085f else 0.045f)
            .hoverable(source)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(190.dp)
                .aspectRatio(16f / 9f)
                .background(Palette.surfaceHigh, RoundedCornerShape(9.dp))
                .clickable(onClick = onResume),
        ) {
            RemoteImage(progress.background ?: progress.poster, progress.name, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                progress.name,
                style = MaterialTheme.typography.titleMedium,
                color = Palette.textPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                buildString {
                    if (progress.season != null && progress.episode != null) {
                        append("S").append(progress.season).append(" · E").append(progress.episode).append("  ")
                    }
                    append(formatDuration(progress.positionMs)).append(" of ").append(formatDuration(progress.durationMs))
                },
                style = MaterialTheme.typography.bodySmall,
                color = Palette.textTertiary,
            )
            Spacer(Modifier.height(10.dp))
            ProgressLine(progress.fraction, Modifier.fillMaxWidth(0.7f), height = 3.dp)
        }
        Spacer(Modifier.width(20.dp))
        PrimaryButton("Resume", Icons.Rounded.PlayArrow, onClick = onResume)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, "Remove", Modifier.size(15.dp), tint = Palette.textTertiary)
        }
    }
}
