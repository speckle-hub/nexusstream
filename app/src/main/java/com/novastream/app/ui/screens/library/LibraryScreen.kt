package com.novastream.app.ui.screens.library

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.novastream.app.data.download.DownloadStatus
import com.novastream.app.data.download.MangaDownload
import com.novastream.app.data.download.VideoDownload
import com.novastream.app.data.download.VideoDownloadStatus
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.Video
import com.novastream.app.data.remote.Http
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.components.PosterCard
import com.novastream.app.ui.components.SectionHeader
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.player.MangaReaderActivity
import com.novastream.app.ui.player.PlayerActivity
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.LibraryViewModel
import com.novastream.app.ui.vm.LocalContainer
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel
import java.io.File

@Composable
fun LibraryScreen(nav: NavHostController) {
    val vm = novaViewModel { LibraryViewModel(it) }
    val nova = LocalNovaColors.current
    val container = LocalContainer.current
    val context = LocalContext.current
    val favorites by vm.favorites.collectAsStateSafe()
    val history by vm.history.collectAsStateSafe()
    val downloads by container.mangaDownloadManager.downloads.collectAsStateSafe()
    val videoDownloads by container.videoDownloadManager.downloads.collectAsStateSafe()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 48.dp, bottom = 24.dp),
    ) {
        item {
            Text(
                "My Library",
                style = MaterialTheme.typography.displaySmall,
                color = nova.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ---- Offline manga downloads ----------------------------------------
        item { SectionHeader("Downloaded Chapters") }
        if (downloads.isEmpty()) {
            item {
                EmptyState(
                    "No chapters downloaded",
                    "Download chapters from the manga reader to read them offline.",
                    icon = Icons.Outlined.Download,
                )
            }
        } else {
            val ordered = downloads.values.sortedByDescending { it.updatedAt }
            items(ordered, key = { it.key }) { d ->
                MangaDownloadRow(d) { context.openChapter(d) }
            }
        }

        // ---- Offline video downloads ----------------------------------------
        item { SectionHeader("Downloaded Videos") }
        if (videoDownloads.isEmpty()) {
            item {
                EmptyState(
                    "No videos downloaded",
                    "Download a movie or episode from its detail page to watch offline.",
                    icon = Icons.Outlined.Download,
                )
            }
        } else {
            val ordered = videoDownloads.values.sortedByDescending { it.updatedAt }
            items(ordered, key = { it.id }) { d ->
                VideoDownloadRow(d) { context.playDownloaded(d) }
            }
        }

        item { SectionHeader("Continue Watching") }
        if (history.isEmpty()) {
            item { EmptyState("Nothing in progress", "Content you watch will appear here.", icon = Icons.Outlined.History) }
        } else {
            items(history.chunked(3)) { chunk -> GridRow(chunk.map { it.item }, nav) }
        }

        item { SectionHeader("Favorites") }
        if (favorites.isEmpty()) {
            item { EmptyState("No favorites yet", "Tap the heart on any title to save it here.", icon = Icons.Outlined.FavoriteBorder) }
        } else {
            items(favorites.chunked(3)) { chunk -> GridRow(chunk, nav) }
        }
    }
}

/** Open a downloaded chapter in the reader (pages resolve from disk, no network). */
private fun Context.openChapter(d: MangaDownload) {
    val item = MediaItem(id = d.mangaId, type = MediaType.MANGA, title = d.mangaTitle, poster = d.cover)
    val chapter = Video(id = d.chapterId, title = d.chapterTitle)
    startActivity(MangaReaderActivity.intent(this, item, chapter))
}

/** Play a downloaded video from its local file, reusing the original item identity. */
private fun Context.playDownloaded(d: VideoDownload) {
    val item = runCatching { Http.gson.fromJson(d.itemJson, MediaItem::class.java) }.getOrNull() ?: return
    val file = d.filePath?.let { File(it) }?.takeIf { it.exists() } ?: return
    val source = StreamSource(url = Uri.fromFile(file).toString(), title = d.title, addonName = "Download")
    startActivity(PlayerActivity.intent(this, item, source, emptyList()))
}

@Composable
private fun MangaDownloadRow(d: MangaDownload, onClick: () -> Unit) {
    val nova = LocalNovaColors.current
    val manager = LocalContainer.current.mangaDownloadManager
    GlassSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!d.cover.isNullOrBlank()) {
                AsyncImage(
                    d.cover, null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    d.chapterTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = nova.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    d.mangaTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = nova.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                when (d.status) {
                    DownloadStatus.DOWNLOADING -> {
                        LinearProgressIndicator(
                            progress = { d.progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = nova.accent,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${d.downloaded}/${d.total} · ${formatBytes(d.bytes)}" +
                                if (d.speedBytesPerSec > 0) " · ${formatBytes(d.speedBytesPerSec)}/s" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = nova.textTertiary,
                        )
                    }
                    DownloadStatus.COMPLETED -> Text(
                        "Downloaded · ${formatBytes(d.bytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = androidx.compose.ui.graphics.Color(0xFF35E0A1),
                    )
                    DownloadStatus.PAUSED -> Text(
                        "Paused · ${d.downloaded}/${d.total}",
                        style = MaterialTheme.typography.labelMedium,
                        color = nova.textTertiary,
                    )
                    DownloadStatus.QUEUED -> Text("Queued", style = MaterialTheme.typography.labelMedium, color = nova.textTertiary)
                    DownloadStatus.FAILED -> Text(
                        d.error ?: "Failed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            when (d.status) {
                DownloadStatus.DOWNLOADING -> IconButton(onClick = { manager.pause(d.key) }) {
                    Icon(Icons.Filled.Pause, "Pause", tint = nova.textSecondary)
                }
                DownloadStatus.PAUSED, DownloadStatus.FAILED -> IconButton(onClick = { manager.resume(d.key) }) {
                    Icon(Icons.Filled.PlayArrow, "Resume", tint = nova.accent)
                }
                else -> {}
            }
            IconButton(onClick = { manager.delete(d.key) }) {
                Icon(Icons.Filled.Delete, "Delete", tint = nova.textSecondary)
            }
        }
    }
}

@Composable
private fun VideoDownloadRow(d: VideoDownload, onClick: () -> Unit) {
    val nova = LocalNovaColors.current
    val manager = LocalContainer.current.videoDownloadManager
    GlassSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!d.poster.isNullOrBlank()) {
                AsyncImage(
                    d.poster, null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    d.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = nova.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                d.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = nova.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                when (d.status) {
                    VideoDownloadStatus.DOWNLOADING -> {
                        LinearProgressIndicator(
                            progress = { d.progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = nova.accent,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            buildString {
                                append("${(d.progress * 100).toInt()}% · ${formatBytes(d.bytes)}")
                                if (d.speedBytesPerSec > 0) append(" · ${formatBytes(d.speedBytesPerSec)}/s")
                                if (d.etaSeconds > 0) append(" · ETA ${formatEta(d.etaSeconds)}")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = nova.textTertiary,
                        )
                    }
                    VideoDownloadStatus.COMPLETED -> Text(
                        "Downloaded · ${formatBytes(d.bytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = androidx.compose.ui.graphics.Color(0xFF35E0A1),
                    )
                    VideoDownloadStatus.PAUSED -> Text(
                        "Paused · ${(d.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = nova.textTertiary,
                    )
                    VideoDownloadStatus.QUEUED -> Text("Queued", style = MaterialTheme.typography.labelMedium, color = nova.textTertiary)
                    VideoDownloadStatus.FAILED -> Text(
                        d.error ?: "Failed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            when (d.status) {
                VideoDownloadStatus.DOWNLOADING -> IconButton(onClick = { manager.pause(d.id) }) {
                    Icon(Icons.Filled.Pause, "Pause", tint = nova.textSecondary)
                }
                VideoDownloadStatus.PAUSED, VideoDownloadStatus.FAILED -> IconButton(onClick = { manager.resume(d.id) }) {
                    Icon(Icons.Filled.PlayArrow, "Resume", tint = nova.accent)
                }
                else -> {}
            }
            IconButton(onClick = { manager.delete(d.id) }) {
                Icon(Icons.Filled.Delete, "Delete", tint = nova.textSecondary)
            }
        }
    }
}

private fun formatEta(seconds: Long): String = when {
    seconds >= 3600 -> "%d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    else -> "%d:%02d".format(seconds / 60, seconds % 60)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
    else -> "$bytes B"
}

@Composable
private fun GridRow(chunk: List<MediaItem>, nav: NavHostController) {
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
