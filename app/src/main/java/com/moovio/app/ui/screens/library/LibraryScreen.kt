package com.moovio.app.ui.screens.library

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.moovio.app.data.download.DownloadStatus
import com.moovio.app.data.download.MangaDownload
import com.moovio.app.data.download.VideoDownload
import com.moovio.app.data.download.VideoDownloadStatus
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.model.Video
import com.moovio.app.data.model.WatchEntry
import com.moovio.app.data.integrations.WatchStats
import com.moovio.app.data.local.LibraryCodec
import com.moovio.app.data.remote.Http
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.moovio.app.ui.components.ConfirmRemoveDialog
import com.moovio.app.ui.components.EmptyState
import com.moovio.app.ui.components.SurfaceCard
import com.moovio.app.ui.components.PillSegmentedControl
import com.moovio.app.ui.components.PosterCard
import com.moovio.app.ui.components.SectionHeader
import com.moovio.app.ui.nav.LocalFloatingNavBottomPadding
import com.moovio.app.ui.nav.Routes
import com.moovio.app.ui.player.MangaReaderActivity
import com.moovio.app.ui.player.PlayerActivity
import com.moovio.app.ui.theme.AppRadius
import com.moovio.app.ui.theme.AppSpacing
import com.moovio.app.ui.theme.LocalMoovioColors
import com.moovio.app.ui.theme.Motion
import com.moovio.app.ui.theme.rememberMoovioHaptics
import com.moovio.app.ui.theme.tnum
import com.moovio.app.ui.vm.LibraryViewModel
import com.moovio.app.ui.vm.LocalContainer
import com.moovio.app.ui.vm.collectAsStateSafe
import com.moovio.app.ui.vm.moovioViewModel

/** Which curated list a pending removal targets, and which title it targets. */
private data class PendingRemoval(val favorite: Boolean, val item: MediaItem)

/**
 * The category chip bar under the segmented control. Both content sections (Downloads and My
 * Library) are filterable by the same set of categories, so one enum drives both.
 *
 * Phase 28 isolation: there is deliberately **no NSFW chip in the ALL filter** — adult downloads
 * and adult favourites/playback history are scrubbed from the public Library entirely and live
 * only inside the PIN/biometric-gated NSFW Hub. The NSFW chip below exists so the Hub can surface
 * its own isolated downloads grid without mixing adult and SFW content.
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
     *
     * NSFW matches only adult titles — never SFW — so the chip acts as an isolation boundary.
     */
    fun accepts(type: MediaType?): Boolean = when (this) {
        ALL -> type != null && !type.isAdult
        MOVIES -> type == MediaType.MOVIE
        TV -> type == MediaType.SERIES
        ANIME -> type == MediaType.ANIME || type == MediaType.MANGA
        NSFW -> type != null && type.isAdult
    }
}

private fun MediaItem.matches(filter: LibraryFilter): Boolean = filter.accepts(type)

/** Downloads carry the original [MediaItem] as JSON, which is decoded tolerantly. */
private fun VideoDownload.matches(filter: LibraryFilter): Boolean =
    filter.accepts(LibraryCodec.decodeFavorites(itemJson).firstOrNull()?.type) || (filter == LibraryFilter.NSFW && nsfw)

private fun MangaDownload.matches(filter: LibraryFilter): Boolean =
    filter.accepts(if (nsfw) MediaType.NSFW_MANGA else MediaType.MANGA) || (filter == LibraryFilter.NSFW && nsfw)

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
    val vm = moovioViewModel { LibraryViewModel(it) }
    val moovio = LocalMoovioColors.current
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
            color = moovio.textPrimary,
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
    // Phase 29: offline storage summary — total bytes across completed video downloads and
    // downloaded manga chapters, so the tab answers "how much space is this using" at a glance.
    val totalBytes = remember(videos, chapters) {
        videos.sumOf { it.bytes } + chapters.sumOf { it.bytes }
    }
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
            DownloadsStorageCard(
                videoCount = videos.size,
                chapterCount = chapters.size,
                totalBytes = totalBytes,
            )
        }
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
    // Phase 28 isolation (defense in depth): the ViewModels already strip adult entries, but
    // this grid is the last stop before pixels — a mis-tagged item reaching it is dropped here
    // so the public Library rails can never render an adult poster, whatever feeds them.
    val watched = history.filter { !it.item.isAdult && it.item.matches(filter) && !it.item.isReadable }
    // Reading progress lands in the same history list as watch progress, but it belongs to its own
    // section: a manga title must never show up under a Play CTA.
    val reading = history.filter { !it.item.isAdult && it.item.matches(filter) && it.item.isReadable }
    val saved = favorites.filter { !it.isAdult && it.matches(filter) }
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
        // Only show the header when it has content (or when the whole section is empty and the
        // empty state below needs its anchor). Without this guard a manga-only user saw a
        // dangling "Continue Watching" header with no cards under it.
        if (watched.isNotEmpty() || (watched.isEmpty() && reading.isEmpty())) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Continue Watching", modifier = Modifier.zIndex(10f))
            }
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

        // ---- Continue Reading (SFW manga; adult manga lives in the NSFW Hub) -
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
    val moovio = LocalMoovioColors.current
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
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        stats.topGenres.forEach { (genre, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(genre, style = MaterialTheme.typography.bodyMedium, color = moovio.textPrimary, modifier = Modifier.width(140.dp))
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / stats.topGenres.first().second.coerceAtLeast(1) },
                                    modifier = Modifier.weight(1f).height(8.dp),
                                    color = moovio.accent,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(count.toString(), style = MaterialTheme.typography.labelMedium.tnum, color = moovio.textTertiary)
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
    val moovio = LocalMoovioColors.current
    SurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Tabular figures so stat numbers keep a stable width as they load/change.
            Text(value, style = MaterialTheme.typography.headlineMedium.tnum, color = moovio.accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary)
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
    val moovio = LocalMoovioColors.current
    val shape = RoundedCornerShape(AppRadius.chip.dp)
    val haptics = rememberMoovioHaptics()
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = AppSpacing.screen),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(LibraryFilter.entries.size) { index ->
            val filter = LibraryFilter.entries[index]
            val active = index == selected
            // Animate the selected chip's fill + outline so switching filters reads as one control
            // changing state rather than two chips blinking. Selection is a surface step and a
            // brighter hairline; the accent tint that used to fill the chip has gone back to the
            // single primary action.
            val chipBg by animateColorAsState(
                targetValue = if (active) moovio.surfaceElevated else androidx.compose.ui.graphics.Color.Transparent,
                animationSpec = tween(Motion.FAST),
                label = "chipBg",
            )
            val chipBorder by animateColorAsState(
                targetValue = if (active) moovio.textSecondary.copy(alpha = 0.5f) else moovio.hairline,
                animationSpec = tween(Motion.FAST),
                label = "chipBorder",
            )
            Box(
                Modifier
                    .clip(shape)
                    .background(chipBg)
                    .border(BorderStroke(1.dp, chipBorder), shape)
                    .clickable {
                        if (!active) {
                            haptics.select()
                            onSelect(index)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    filter.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) moovio.textPrimary else moovio.textSecondary,
                )
            }
        }
    }
}

/** Offline storage summary card at the top of the Downloads tab (Phase 29). */
@Composable
private fun DownloadsStorageCard(videoCount: Int, chapterCount: Int, totalBytes: Long) {
    val moovio = LocalMoovioColors.current
    SurfaceCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Download, null, tint = moovio.accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // Tabular figures so the byte readout keeps a stable width as downloads tick.
                Text(
                    formatBytes(totalBytes),
                    style = MaterialTheme.typography.headlineSmall.tnum,
                    color = moovio.textPrimary,
                )
                Text(
                    "$videoCount video${if (videoCount == 1) "" else "s"} · " +
                        "$chapterCount chapter${if (chapterCount == 1) "" else "s"} stored offline",
                    style = MaterialTheme.typography.labelMedium,
                    color = moovio.textTertiary,
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
    val moovio = LocalMoovioColors.current
    val manager = LocalContainer.current.mangaDownloadManager
    // Full-width rows: span the grid line so the card fills the row instead of being cell-sized.
    SurfaceCard(
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
                    color = moovio.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    d.mangaTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = moovio.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                when (d.status) {
                    DownloadStatus.DOWNLOADING -> {
                        LinearProgressIndicator(
                            progress = { d.progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = moovio.accent,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${d.downloaded}/${d.total} · ${formatBytes(d.bytes)}" +
                                if (d.speedBytesPerSec > 0) " · ${formatBytes(d.speedBytesPerSec)}/s" else "",
                            // Tabular figures: the counter and byte figures must not jitter as they tick.
                            style = MaterialTheme.typography.labelMedium.tnum,
                            color = moovio.textTertiary,
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
                        color = moovio.textTertiary,
                    )
                    DownloadStatus.QUEUED -> Text("Queued", style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary)
                    DownloadStatus.FAILED -> Text(
                        d.error ?: "Failed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            when (d.status) {
                DownloadStatus.DOWNLOADING -> IconButton(onClick = { manager.pause(d.key) }) {
                    Icon(Icons.Filled.Pause, "Pause", tint = moovio.textSecondary)
                }
                DownloadStatus.PAUSED, DownloadStatus.FAILED -> IconButton(onClick = { manager.resume(d.key) }) {
                    Icon(Icons.Filled.PlayArrow, "Resume", tint = moovio.accent)
                }
                else -> {}
            }
            IconButton(onClick = { manager.delete(d.key) }) {
                Icon(Icons.Filled.Delete, "Delete", tint = moovio.textSecondary)
            }
        }
    }
}

@Composable
private fun VideoDownloadRow(d: VideoDownload, onClick: () -> Unit) {
    val moovio = LocalMoovioColors.current
    val manager = LocalContainer.current.videoDownloadManager
    SurfaceCard(
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
                    color = moovio.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                d.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                when (d.status) {
                    VideoDownloadStatus.DOWNLOADING -> {
                        LinearProgressIndicator(
                            progress = { d.progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = moovio.accent,
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
                            color = moovio.textTertiary,
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
                        color = moovio.textTertiary,
                    )
                    VideoDownloadStatus.QUEUED -> Text("Queued", style = MaterialTheme.typography.labelMedium, color = moovio.textTertiary)
                    VideoDownloadStatus.FAILED -> Text(
                        d.error ?: "Failed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            when (d.status) {
                VideoDownloadStatus.DOWNLOADING -> IconButton(onClick = { manager.pause(d.id) }) {
                    Icon(Icons.Filled.Pause, "Pause", tint = moovio.textSecondary)
                }
                VideoDownloadStatus.PAUSED, VideoDownloadStatus.FAILED -> IconButton(onClick = { manager.resume(d.id) }) {
                    Icon(Icons.Filled.PlayArrow, "Resume", tint = moovio.accent)
                }
                else -> {}
            }
            IconButton(onClick = { manager.delete(d.id) }) {
                Icon(Icons.Filled.Delete, "Delete", tint = moovio.textSecondary)
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
