package com.novastream.app.ui.screens.library

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.novastream.app.data.model.WatchEntry
import com.novastream.app.data.integrations.WatchStats
import com.novastream.app.data.local.LibraryCodec
import com.novastream.app.data.remote.Http
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.novastream.app.ui.components.ConfirmRemoveDialog
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.components.PillSegmentedControl
import com.novastream.app.ui.components.PosterCard
import com.novastream.app.ui.components.SectionHeader
import com.novastream.app.ui.nav.LocalFloatingNavBottomPadding
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.player.MangaReaderActivity
import com.novastream.app.ui.player.PlayerActivity
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.Motion
import com.novastream.app.ui.theme.tnum
import com.novastream.app.ui.vm.LibraryViewModel
import com.novastream.app.ui.vm.LocalContainer
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

/** Which curated list a pending removal targets, and which title it targets. */
private data class PendingRemoval(val favorite: Boolean, val item: MediaItem)

/**
 * The category chip bar under the segmented control. Both content sections (Downloads and My
 * Library) are filterable by the same set of categories, so one enum drives both.
 */
private enum class LibraryFilter(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    TV("TV Shows"),
    ANIME("Anime"),
    NSFW("NSFW");

    /**
     * Whether a title of [type] belongs here. `null` (an unknown/legacy type) only passes
     * [ALL] — it is never misfiled into a specific category.
     *
     * Manga is deliberately grouped with Anime rather than getting its own chip: the catalogue
     * tabs pair the two, and a separate chip would either strand manga behind "All" or clutter the
     * bar with a rarely-used entry.
     */
    fun accepts(type: MediaType?): Boolean = when (this) {
        ALL -> true
        MOVIES -> type == MediaType.MOVIE
        TV -> type == MediaType.SERIES
        ANIME -> type == MediaType.ANIME || type == MediaType.MANGA
        NSFW -> type == MediaType.NSFW_ANIME || type == MediaType.NSFW_MANGA || type == MediaType.REAL
    }
}

private fun MediaItem.matches(filter: LibraryFilter): Boolean = filter.accepts(type)

/** Downloads carry the original [MediaItem] as JSON, which is decoded tolerantly. */
private fun VideoDownload.matches(filter: LibraryFilter): Boolean =
    filter.accepts(LibraryCodec.decodeFavorites(itemJson).firstOrNull()?.type)

private fun MangaDownload.matches(filter: LibraryFilter): Boolean =
    filter.accepts(if (nsfw) MediaType.NSFW_MANGA else MediaType.MANGA)

/**
 * The Library screen, split into two top sections behind a segmented control:
 *
 *  1. **Downloads** — purely offline media (downloaded videos + downloaded manga chapters).
 *  2. **My Library** — saved media and progress (Continue Watching + Favourites).
 *
 * They used to be stacked in one long vertical list, so offline downloads were buried between the
 * watch history and the favourites grid.
 */
@Composable
fun LibraryScreen(nav: NavHostController) {
    val vm = novaViewModel { LibraryViewModel(it) }
    val nova = LocalNovaColors.current
    val container = LocalContainer.current
    val context = LocalContext.current
    val favorites by vm.favorites.collectAsStateSafe()
    val history by vm.history.collectAsStateSafe()

    // Title awaiting confirmation before it is dropped from Favourites / Continue Watching.
    var pendingRemoval by remember { mutableStateOf<PendingRemoval?>(null) }
    // rememberSaveable so returning to the Library tab keeps the section the user was on.
    var section by rememberSaveable { mutableIntStateOf(0) }
    // Category chip selection, also saved so switching tabs does not reset the filter.
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    val filter = LibraryFilter.entries[filterIndex.coerceIn(0, LibraryFilter.entries.lastIndex)]

    // Scope download state to the active category filter: a progress tick on a *different*
    // category's download no longer recomposes the whole Library screen.
    val downloads by remember(filter) {
        container.mangaDownloadManager.downloads
            .map { m -> m.values.filter { it.matches(filter) } }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(
        container.mangaDownloadManager.downloads.value.values.filter { it.matches(filter) }
    )
    val videoDownloads by remember(filter) {
        container.videoDownloadManager.downloads
            .map { list -> list.filter { it.matches(filter) } }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(
        container.videoDownloadManager.downloads.value.filter { it.matches(filter) }
    )

    Column(Modifier.fillMaxSize()) {
        Text(
            "Library",
            style = MaterialTheme.typography.displaySmall,
            color = nova.textPrimary,
            modifier = Modifier.statusBarsPadding().padding(start = AppSpacing.screen, top = 8.dp, bottom = 8.dp),
        )
        PillSegmentedControl(
            labels = listOf("Downloads", "My Library", "Stats"),
            selected = section,
            onSelect = { section = it },
            modifier = Modifier.padding(horizontal = AppSpacing.screen, vertical = 4.dp),
        )
        // The chips only mean something for the two content sections — Stats is a global view.
        if (section != 2) {
            LibraryFilterBar(
                selected = filterIndex,
                onSelect = { filterIndex = it },
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
            )
        }
        when (section) {
            0 -> DownloadsGrid(downloads, videoDownloads, filter, context)
            1 -> ActivityGrid(nav, history, favorites, filter) { pendingRemoval = it }
            else -> StatsTab(favorites, history)
        }
    }

    pendingRemoval?.let { pending ->
        ConfirmRemoveDialog(
            title = if (pending.favorite) "Remove from favourites?" else "Remove from Continue Watching?",
            message = if (pending.favorite) {
                "\u201c${pending.item.title}\u201d will be removed from your library."
            } else {
                "\u201c${pending.item.title}\u201d will be removed and its saved progress cleared."
            },
            onConfirm = {
                if (pending.favorite) vm.removeFavorite(pending.item) else vm.removeWatch(pending.item.key)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }
}

/** Downloads section: offline video downloads and downloaded manga chapters only. */
@Composable
private fun DownloadsGrid(
    downloads: List<MangaDownload>,
    videoDownloads: List<VideoDownload>,
    filter: LibraryFilter,
    context: Context,
) {
    val listState = rememberLazyGridState()
    val videos = videoDownloads
    val chapters = downloads
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
        if (filter != LibraryFilter.ALL && videos.isEmpty() && chapters.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    "Nothing in ${filter.label}",
                    "Downloads in this category will appear here.",
                    icon = Icons.Outlined.Download,
                )
            }
            return@LazyVerticalGrid
        }

        // Under "All" both sections always render (with their own empty states, as before); a
        // specific category only renders the sections that actually have matching downloads.
        if (filter == LibraryFilter.ALL || videos.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Downloaded Videos", modifier = Modifier.zIndex(10f))
            }
            if (videos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        "No videos downloaded",
                        "Download a movie or episode from its detail page to watch offline.",
                        icon = Icons.Outlined.Download,
                    )
                }
            } else {
                val ordered = videos.sortedByDescending { it.updatedAt }
                items(ordered, key = { "vid:${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { d ->
                    VideoDownloadRow(d) { context.playDownloaded(d) }
                }
            }
        }

        if (filter == LibraryFilter.ALL || chapters.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Downloaded Chapters", modifier = Modifier.zIndex(10f))
            }
            if (chapters.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        "No chapters downloaded",
                        "Download chapters from the manga reader to read them offline.",
                        icon = Icons.Outlined.Download,
                    )
                }
            } else {
                val ordered = chapters.sortedByDescending { it.updatedAt }
                items(ordered, key = { "ch:${it.key}" }, span = { GridItemSpan(maxLineSpan) }) { d ->
                    MangaDownloadRow(d) { context.openChapter(d) }
                }
            }
        }
    }
}

/** My Library section: saved titles and in-progress watch history. */
@Composable
private fun ActivityGrid(
    nav: NavHostController,
    history: List<WatchEntry>,
    favorites: List<MediaItem>,
    filter: LibraryFilter,
    onRemove: (PendingRemoval) -> Unit,
) {
    val listState = rememberLazyGridState()
    val watched = history.filter { it.item.matches(filter) && !it.item.isReadable }
    // Reading progress lands in the same history list as watch progress, but it belongs to its own
    // section: a manga title must never show up under a Play CTA.
    val reading = history.filter { it.item.matches(filter) && it.item.isReadable }
    val saved = favorites.filter { it.matches(filter) }
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
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Continue Watching", modifier = Modifier.zIndex(10f))
        }
        if (watched.isEmpty() && reading.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (filter == LibraryFilter.ALL) {
                    EmptyState("Nothing in progress", "Content you watch or read will appear here.", icon = Icons.Outlined.History)
                } else {
                    EmptyState(
                        "Nothing in ${filter.label}",
                        "In-progress titles in this category will appear here.",
                        icon = Icons.Outlined.History,
                    )
                }
            }
        } else if (watched.isNotEmpty()) {
            // Prefixed keys: history and favourites share one grid, so an identical title in both
            // would otherwise collide (both keyed on `item.key`) and reuse the wrong card state.
            items(watched.map { it.item }, key = { "hist:${it.key}" }) { item ->
                PosterCard(
                    item = item,
                    onClick = { nav.navigate(Routes.detail(item)) },
                    modifier = Modifier.fillMaxWidth(),
                    width = 0,
                    // Long-press and the card's "×" open the same confirmation dialog.
                    onLongClick = { onRemove(PendingRemoval(favorite = false, item = item)) },
                    onRemove = { onRemove(PendingRemoval(favorite = false, item = item)) },
                )
            }
        }

        // ---- Continue Reading (manga, normal + NSFW) -------------------------
        if (reading.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Continue Reading", modifier = Modifier.zIndex(10f))
            }
            items(reading.map { it.item }, key = { "read:${it.key}" }) { item ->
                PosterCard(
                    item = item,
                    onClick = { nav.navigate(Routes.detail(item)) },
                    modifier = Modifier.fillMaxWidth(),
                    width = 0,
                    onLongClick = { onRemove(PendingRemoval(favorite = false, item = item)) },
                    onRemove = { onRemove(PendingRemoval(favorite = false, item = item)) },
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Favorites", modifier = Modifier.zIndex(10f))
        }
        if (saved.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (filter == LibraryFilter.ALL) {
                    EmptyState("No favorites yet", "Tap the heart on any title to save it here.", icon = Icons.Outlined.FavoriteBorder)
                } else {
                    EmptyState(
                        "Nothing in ${filter.label}",
                        "Favourites in this category will appear here.",
                        icon = Icons.Outlined.FavoriteBorder,
                    )
                }
            }
        } else {
            items(saved, key = { "fav:${it.key}" }) { item ->
                PosterCard(
                    item = item,
                    onClick = { nav.navigate(Routes.detail(item)) },
                    modifier = Modifier.fillMaxWidth(),
                    width = 0,
                    onLongClick = { onRemove(PendingRemoval(favorite = true, item = item)) },
                    onRemove = { onRemove(PendingRemoval(favorite = true, item = item)) },
                )
            }
        }
    }
}

/** Watch/read statistics dashboard (Phase 12). */
@Composable
private fun StatsTab(favorites: List<MediaItem>, history: List<WatchEntry>) {
    val nova = LocalNovaColors.current
    // Defensive: stats are derived from the same history that used to be able to carry a null
    // `type`; a failure here must degrade to an empty dashboard, not take the Library tab down.
    val stats = remember(favorites, history) {
        runCatching { WatchStats.compute(history, favorites) }
            .getOrElse { WatchStats.compute(emptyList(), emptyList()) }
    }
    val listState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = listState,
        modifier = Modifier.fillMaxSize().clipToBounds(),
        contentPadding = PaddingValues(
            start = AppSpacing.screen,
            end = AppSpacing.screen,
            top = AppSpacing.screen,
            bottom = AppSpacing.screen + LocalFloatingNavBottomPadding.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.item),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.item),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            StatCard("Watch time", "${String.format("%.1f", stats.watchedHours)} h")
        }
        item { StatCard("Titles in progress", stats.watchedItems.toString()) }
        item { StatCard("Completed", stats.completed.toString()) }
        item { StatCard("Favourites", stats.favorites.toString()) }

        if (stats.byType.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("By type", modifier = Modifier.zIndex(10f))
            }
            stats.byType.forEach { (type, count) ->
                item { StatCard(type, count.toString()) }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Top genres", modifier = Modifier.zIndex(10f))
        }
        if (stats.topGenres.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("No stats yet", "Watch or save a few titles to build your dashboard.", icon = Icons.Outlined.History)
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        stats.topGenres.forEach { (genre, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(genre, style = MaterialTheme.typography.bodyMedium, color = nova.textPrimary, modifier = Modifier.width(140.dp))
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / stats.topGenres.first().second.coerceAtLeast(1) },
                                    modifier = Modifier.weight(1f).height(8.dp),
                                    color = nova.accent,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(count.toString(), style = MaterialTheme.typography.labelMedium.tnum, color = nova.textTertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    val nova = LocalNovaColors.current
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Tabular figures so stat numbers keep a stable width as they load/change.
            Text(value, style = MaterialTheme.typography.headlineMedium.tnum, color = nova.accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = nova.textTertiary)
        }
    }
}

/**
 * A compact pill segmented control, matching the Browse tab's control so the two-level navigation
 * feels consistent.
 */
@Composable
private fun LibrarySegmentedControl(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nova = LocalNovaColors.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(nova.surfaceElevated)
            .border(BorderStroke(1.dp, nova.outline.copy(alpha = 0.6f)), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) nova.accent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) nova.accent else nova.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * The horizontal category chip bar (Phase 13). Sits directly under the segmented control and
 * filters whichever content section is open. It deliberately lives *outside* the scrollable grid,
 * so changing a chip re-filters in place instead of scrolling the user back to the top.
 */
@Composable
private fun LibraryFilterBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nova = LocalNovaColors.current
    val shape = RoundedCornerShape(50)
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = AppSpacing.screen),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(LibraryFilter.entries.size) { index ->
            val filter = LibraryFilter.entries[index]
            val active = index == selected
            // Animate the selected chip's fill + outline so switching filters reads as one control
            // changing state rather than two chips blinking.
            val chipBg by animateColorAsState(
                targetValue = if (active) nova.accent.copy(alpha = 0.18f) else nova.surfaceElevated,
                animationSpec = tween(Motion.FAST),
                label = "chipBg",
            )
            val chipBorder by animateColorAsState(
                targetValue = if (active) nova.accent else nova.outline.copy(alpha = 0.6f),
                animationSpec = tween(Motion.FAST),
                label = "chipBorder",
            )
            Box(
                Modifier
                    .clip(shape)
                    .background(chipBg)
                    .border(BorderStroke(1.dp, chipBorder), shape)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    filter.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) nova.accent else nova.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** Open a downloaded chapter in the reader (pages resolve from disk, no network). */
private fun Context.openChapter(d: MangaDownload) {
    val item = MediaItem(id = d.mangaId, type = MediaType.MANGA, title = d.mangaTitle, poster = d.cover)
    val chapter = Video(id = d.chapterId, title = d.chapterTitle)
    startActivity(MangaReaderActivity.intent(this, item, chapter))
}

/** Play a downloaded video; the player reads the shared download cache, so it works offline. */
private fun Context.playDownloaded(d: VideoDownload) {
    if (d.itemJson.isBlank()) return
    val item = runCatching { Http.gson.fromJson(d.itemJson, MediaItem::class.java) }.getOrNull() ?: return
    val source = StreamSource(url = d.url, title = d.title, addonName = "Download")
    startActivity(PlayerActivity.intent(this, item, source, emptyList()))
}

@Composable
private fun MangaDownloadRow(d: MangaDownload, onClick: () -> Unit) {
    val nova = LocalNovaColors.current
    val manager = LocalContainer.current.mangaDownloadManager
    // Full-width rows: span the grid line so the card fills the row instead of being cell-sized.
    GlassSurface(
        Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 4.dp),
        onClick = onClick,
    ) {
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
                            // Tabular figures: the counter and byte figures must not jitter as they tick.
                            style = MaterialTheme.typography.labelMedium.tnum,
                            color = nova.textTertiary,
                        )
                    }
                    DownloadStatus.COMPLETED -> Text(
                        "Downloaded · ${formatBytes(d.bytes)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = Color(0xFF35E0A1),
                    )
                    DownloadStatus.PAUSED -> Text(
                        "Paused · ${d.downloaded}/${d.total}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
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
    GlassSurface(
        Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 4.dp),
        onClick = onClick,
    ) {
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
                            // Tabular figures so the scrolling % / bytes / ETA don't jitter.
                            style = MaterialTheme.typography.labelMedium.tnum,
                            color = nova.textTertiary,
                        )
                    }
                    VideoDownloadStatus.COMPLETED -> Text(
                        "Downloaded · ${formatBytes(d.bytes)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = Color(0xFF35E0A1),
                    )
                    VideoDownloadStatus.PAUSED -> Text(
                        "Paused · ${(d.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
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
