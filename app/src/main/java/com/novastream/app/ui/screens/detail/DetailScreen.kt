package com.novastream.app.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.novastream.app.data.download.VideoKind
import com.novastream.app.data.model.CastMember
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.SubtitleTrack
import com.novastream.app.data.model.Video
import com.novastream.app.data.remote.TorrentStreamer
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.components.MediaRow
import com.novastream.app.ui.components.RatingBadge
import com.novastream.app.ui.components.ResolutionBadge
import com.novastream.app.ui.components.SectionHeader
import com.novastream.app.ui.components.ShimmerBox
import com.novastream.app.ui.components.TagChip
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.player.PlayerActivity
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.Motion
import com.novastream.app.ui.theme.PosterPalette
import com.novastream.app.ui.theme.rememberPosterPalette
import com.novastream.app.ui.vm.DetailViewModel
import com.novastream.app.ui.vm.LocalContainer
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(nav: NavHostController, item: MediaItem) {
    val vm = novaViewModel(key = item.key) { DetailViewModel(it, item) }
    val nova = LocalNovaColors.current
    val context = LocalContext.current
    val detail by vm.detail.collectAsStateSafe()
    val streams by vm.streams.collectAsStateSafe()
    val streamErrors by vm.streamErrors.collectAsStateSafe()
    val subtitles by vm.subtitles.collectAsStateSafe()
    val related by vm.related.collectAsStateSafe()
    val chapters by vm.chapters.collectAsStateSafe()
    val loading by vm.loading.collectAsStateSafe()
    val streamsLoading by vm.streamsLoading.collectAsStateSafe()
    val selectedVideo by vm.selectedVideo.collectAsStateSafe()
    val fav by vm.isFavorite.collectAsStateSafe()
    val seasons by vm.seasons.collectAsStateSafe()
    val expandedSeason by vm.expandedSeason.collectAsStateSafe()
    val seasonEpisodes by vm.seasonEpisodes.collectAsStateSafe()
    val seasonLoading by vm.seasonLoading.collectAsStateSafe()
    val error by vm.error.collectAsStateSafe()

    val isManga = item.type == MediaType.MANGA || item.type == MediaType.NSFW_MANGA
    val container = LocalContainer.current
    val external by container.settings.playerExternal.collectAsStateSafe()
    var torrentPreparing by remember { mutableStateOf(false) }
    // Saved playback progress, drawn as a thin bar under each episode (Phase 13).
    val watchHistory by container.libraryStore.history.collectAsState(emptyList())
    // Stream picker bottom sheet (Phase 13) — mirrors moved off the page body.
    var streamSheetOpen by remember { mutableStateOf(false) }
    // Subtitle track picked in the stream picker (null = let the player auto-choose). The chosen
    // track is handed to the player first, which marks it as the default caption.
    var selectedSubId by remember { mutableStateOf<String?>(null) }
    // Clear a selection whose track disappeared (episode switch / fresh resolve).
    LaunchedEffect(subtitles) {
        if (selectedSubId != null && subtitles.none { it.id == selectedSubId }) selectedSubId = null
    }
    // Season currently being bulk-downloaded, or null when idle.
    var seasonDownloading by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    // Dynamic Material You theming: derive an accent from the artwork and animate to it.
    val palette = rememberPosterPalette(detail?.background ?: item.backdrop ?: item.poster)
    val accent by animateColorAsState(palette?.vibrantColor ?: nova.accent, label = "detailAccent")

    // Flat, ordered episode list + the currently selected episode's index, handed to the player
    // so it can autoplay the next episode and populate its episode sheet.
    val orderedEpisodes = remember(seasonEpisodes) {
        seasonEpisodes.values.flatten().sortedWith(compareBy({ it.season ?: 0 }, { it.episode ?: 0 }))
    }
    val currentEpisodeIndex = orderedEpisodes.indexOfFirst { it.id == selectedVideo?.id }

    // Keep the primary Play button pointed at the picked season's first episode, so a series is
    // playable straight away instead of only after the picker has been opened once.
    LaunchedEffect(expandedSeason, seasonEpisodes) {
        if (isManga) return@LaunchedEffect
        val eps = expandedSeason?.let { seasonEpisodes[it] }.orEmpty()
        if (eps.isEmpty()) return@LaunchedEffect
        val current = selectedVideo?.id
        if (current != null && eps.any { it.id == current }) return@LaunchedEffect
        vm.loadStreams(eps.first())
    }

    fun openExternal(source: StreamSource) {
        val url = source.playableUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(url), "video/*") }
        runCatching { context.startActivity(intent) }
    }

    /** Queue a direct/HLS stream for offline download (torrent-only streams have no HTTP URL). */
    fun downloadStream(source: StreamSource) {
        val url = source.playableUrl ?: return
        val kind = if (url.contains(".m3u8", ignoreCase = true)) VideoKind.HLS else VideoKind.PROGRESSIVE
        val title = source.title ?: source.name ?: item.title
        container.videoDownloadManager.enqueue(
            item, url, title, selectedVideo?.title, kind,
            headers = source.headers,
            alternates = source.alternates,
        )
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    }

    /**
     * Bulk-download a whole season: resolve each episode's streams and queue the first directly
     * playable one. Episodes are resolved sequentially so add-ons are not hammered all at once;
     * the Media3 queue still downloads in parallel (see DownloadManagerProvider).
     */
    fun downloadSeason(season: Int) {
        if (seasonDownloading != null) return
        seasonDownloading = season
        scope.launch {
            val eps = runCatching { vm.loadSeasonEpisodes(season) }.getOrDefault(emptyList())
            var queued = 0
            for (ep in eps) {
                val sources = runCatching { container.streamRepository.streamsFor(item, ep.id) }
                    .getOrDefault(emptyList())
                val source = sources.firstOrNull { it.playableUrl != null } ?: continue
                val url = source.playableUrl ?: continue
                val kind = if (url.contains(".m3u8", ignoreCase = true)) VideoKind.HLS else VideoKind.PROGRESSIVE
                val label = buildString {
                    append(item.title)
                    if (ep.season != null && ep.episode != null) append(" \u00b7 S${ep.season}E${ep.episode}")
                }
                container.videoDownloadManager.enqueue(
                    item, url, label, ep.title, kind,
                    headers = source.headers,
                    alternates = source.alternates,
                )
                queued++
            }
            seasonDownloading = null
            Toast.makeText(
                context,
                when {
                    queued == 0 -> "No downloadable streams found for this season"
                    queued == 1 -> "Queued 1 episode for download"
                    else -> "Queued $queued episodes for download"
                },
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    /**
     * Open the stream picker for [video] (null = the title itself, i.e. a movie).
     *
     * The lookup is skipped when the target is already loaded, so reopening the picker for a
     * movie doesn't re-query every add-on.
     */
    fun pickStreams(video: Video?) {
        // Reload when the target changed, **or** when the previous lookup for this same target came
        // back empty (a failed/blocked resolve) so tapping the episode again can retry it instead of
        // reopening a permanently empty sheet.
        val targetChanged = selectedVideo?.id != video?.id
        val canRetry = streams.isEmpty() && !streamsLoading
        if (targetChanged || canRetry) vm.loadStreams(video)
        streamSheetOpen = true
    }

    fun play(source: StreamSource) {
        // Chosen subtitle first so Media3 auto-selects it; the rest keep catalogue order.
        val subList = selectedSubId?.let { id -> subtitles.sortedByDescending { it.id == id } } ?: subtitles
        // Dismiss the picker first: it owns its own window, so the torrent "Preparing stream…"
        // overlay below would otherwise render *behind* it.
        streamSheetOpen = false
        // 1) A direct HTTP(S) stream — play immediately.
        if (source.playableUrl != null) {
            if (external) openExternal(source)
            else context.startActivity(
                PlayerActivity.intent(context, item, source, subList, orderedEpisodes, currentEpisodeIndex)
            )
            return
        }
        // 2) A YouTube-backed stream.
        source.ytId?.let {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$it")))
            }
            return
        }
        // 3) A torrent / P2P stream (the norm for Real 18+ add-ons) — stream it in-app.
        if (source.infoHash != null) {
            torrentPreparing = true
            TorrentStreamer.stream(
                context = context,
                source = source,
                onReady = { url ->
                    torrentPreparing = false
                    val playable = source.copy(url = url)
                    if (external) openExternal(playable)
                    else context.startActivity(
                        PlayerActivity.intent(context, item, playable, subList, orderedEpisodes, currentEpisodeIndex)
                    )
                },
                onError = { msg ->
                    torrentPreparing = false
                    Toast.makeText(context, "Torrent playback failed: $msg", Toast.LENGTH_LONG).show()
                    // Last-resort: hand the magnet link to an external torrent app, if any.
                    source.magnetUri?.let { m ->
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m))) }
                    }
                },
            )
            return
        }
        Toast.makeText(context, "This stream can't be played.", Toast.LENGTH_SHORT).show()
    }

    fun openTrailer(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    /** Sub-heading for the stream picker: the picked episode, or the title itself for a movie. */
    val sheetHeading = selectedVideo?.let { v ->
        buildString {
            v.episode?.let { append("E$it \u00b7 ") }
            append(v.title?.takeIf { it.isNotBlank() } ?: "Episode ${v.episode ?: ""}")
        }
    } ?: (detail?.name ?: item.title)

    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {

        // ---- Banner / cover header -------------------------------------------
        item {
            DetailHeader(
                item = item,
                detail = detail,
                loading = loading,
                palette = palette,
                onBack = { nav.popBackStack() },
                // The heart lives in the header's app bar rather than as a full-width button under
                // the CTAs — it is a secondary toggle, not something competing with "Play".
                favourite = fav,
                onToggleFavourite = { vm.toggleFavorite() },
                accent = accent,
            )
        }

        // ---- Primary action --------------------------------------------------
        // One dominant CTA. "Add to Library" used to be a *full-width* button stacked under Play,
        // which gave two competing actions equal visual weight; it is now a compact toggle in the
        // header app bar (see DetailHeader).
        item {
            // Watch/read progress for this title, surfaced on the floating primary pill.
            val resumeEntry = watchHistory.firstOrNull {
                it.item.key == item.key && it.progress in 0.01f..0.94f
            }
            val resumeProgress = resumeEntry?.progress
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screen),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.item),
            ) {
                if (isManga) {
                    val canRead = chapters.isNotEmpty()
                    PrimaryActionButton(
                        label = if (resumeProgress != null) "Resume" else "Read",
                        enabled = canRead,
                        accent = accent,
                        progress = resumeProgress,
                        onClick = {
                            chapters.firstOrNull()?.let {
                                context.startActivity(com.novastream.app.ui.player.MangaReaderActivity.intent(context, item, it))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.MenuBook, null, modifier = Modifier.size(20.dp))
                    }
                } else {
                    val firstPlayable = streams.firstOrNull {
                        it.playableUrl != null || it.infoHash != null || it.ytId != null
                    }
                    PrimaryActionButton(
                        label = if (streamsLoading && streams.isEmpty()) {
                            "Finding\u2026"
                        } else {
                            resumeEntry?.videoTitle?.takeIf { it.isNotBlank() }
                                ?.let { "Resume $it" } ?: if (resumeEntry != null) "Resume" else "Play"
                        },
                        enabled = !streamsLoading && firstPlayable != null,
                        accent = accent,
                        progress = resumeProgress,
                        onClick = { firstPlayable?.let { play(it) } },
                        modifier = Modifier.weight(1f),
                    ) {
                        if (streamsLoading && streams.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                detail?.trailerUrl?.let { url ->
                    // Secondary: an outlined, equal-height companion rather than a competing filled CTA.
                    OutlinedButton(
                        onClick = { openTrailer(url) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(50.dp),
                    ) {
                        Icon(Icons.Outlined.SmartDisplay, null, modifier = Modifier.size(20.dp))
                    }
                }
                if (!isManga) {
                    // Secondary: open the stream picker directly (mirrors now live in a sheet
                    // rather than in a long list further down the page). For a series this targets
                    // the episode already on screen so the selection is never silently cleared.
                    OutlinedButton(
                        onClick = { pickStreams(selectedVideo) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(50.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.PlaylistPlay, "Choose a stream", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // ---- Graceful error / empty state ------------------------------------
        if (detail == null && !loading) {
            item {
                GlassSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Couldn't load details",
                            style = MaterialTheme.typography.titleMedium,
                            color = nova.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            error ?: "No metadata is available for this title right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = nova.textSecondary,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { vm.retry() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = nova.accent),
                        ) {
                            Text("Retry", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        // ---- Genres / tags ---------------------------------------------------
        detail?.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
            item {
                // Breathing room between the primary CTA row and the genre chips — they used to
                // butt straight up against the Play button.
                Spacer(Modifier.height(AppSpacing.item))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(genres) { g ->
                        TagChip(g) { nav.navigate(Routes.searchFor(g)) }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        // ---- Synopsis (expandable) ------------------------------------------
        detail?.description?.takeIf { it.isNotBlank() }?.let { desc ->
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Synopsis", style = MaterialTheme.typography.titleMedium, color = nova.textPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    ExpandableText(desc)
                    Spacer(Modifier.height(18.dp))
                }
            }
        }

        // ---- Cast / characters ----------------------------------------------
        val cast = detail?.castMembers ?: emptyList()
        if (cast.isNotEmpty()) {
            item {
                SectionHeader("Cast & Characters")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(cast) { member -> CastCard(member) }
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        // ---- Episodes / chapters --------------------------------------------
        if (isManga && chapters.isNotEmpty()) {
            item { SectionHeader("Chapters (${chapters.size})") }
            items(chapters, key = { it.id }) { ch ->
                ChapterRow(ch) {
                    context.startActivity(com.novastream.app.ui.player.MangaReaderActivity.intent(context, item, ch))
                }
            }
        }

        if (!isManga && seasons.isNotEmpty()) {
            val selectedSeason = expandedSeason ?: seasons.first().season
            val eps = seasonEpisodes[selectedSeason].orEmpty()
            val header = seasons.firstOrNull { it.season == selectedSeason }

            // One block: section title + the horizontal season picker + the episode-count hint.
            item(key = "season-picker") {
                SectionHeader(
                    if (item.type == MediaType.SERIES) "Seasons & Episodes" else "Episodes",
                    trailing = {
                        // Bulk "download season" action, moved here from the old per-season card.
                        if (seasonLoading == selectedSeason || seasonDownloading == selectedSeason) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = nova.accent,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            IconButton(onClick = { downloadSeason(selectedSeason) }) {
                                Icon(Icons.Filled.Download, "Download season", tint = nova.textSecondary)
                            }
                        }
                    },
                )
                SeasonPickerRow(
                    seasons = seasons,
                    selected = selectedSeason,
                    onSelect = { vm.selectSeason(it) },
                )
                Text(
                    when {
                        seasonLoading == selectedSeason -> "Loading episodes\u2026"
                        eps.isNotEmpty() -> "${eps.size} episodes"
                        header?.episodeCount != null -> "${header.episodeCount} episodes"
                        else -> "No episodes found"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = nova.textTertiary,
                    modifier = Modifier.padding(horizontal = AppSpacing.screen, vertical = 4.dp),
                )
            }

            if (eps.isEmpty()) {
                item {
                    if (seasonLoading == selectedSeason) {
                        EmptyState("Loading episodes\u2026", "Fetching this season's episode list.")
                    } else {
                        EmptyState("No episodes found", "This add-on returned no episodes for this season.")
                    }
                }
            } else {
                items(eps, key = { "ep:${it.id}" }) { v ->
                    EpisodeRow(
                        v = v,
                        selected = selectedVideo?.id == v.id,
                        progress = watchHistory.firstOrNull { it.videoId == v.id },
                        onClick = { pickStreams(v) },
                    )
                }
            }
        }

        // ---- Recommendations -------------------------------------------------
        if (related.isNotEmpty()) {
            item { MediaRow("You may also like", related, onClick = { nav.navigate(Routes.detail(it)) }) }
        }
    }

        // ---- Torrent preparation overlay -------------------------------------
        if (torrentPreparing) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                GlassSurface(Modifier.padding(32.dp)) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = nova.accent)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Preparing stream\u2026",
                            style = MaterialTheme.typography.titleMedium,
                            color = nova.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Connecting to peers via torrent",
                            style = MaterialTheme.typography.bodySmall,
                            color = nova.textSecondary,
                        )
                    }
                }
            }
        }

        // ---- Stream picker (Phase 13) ---------------------------------------
        // Mirrors live here instead of in a list on the page body, which is what made TV/anime
        // detail pages unreadable. Tapping an episode opens it pre-loaded with that episode's
        // streams; the header button opens it for the title itself.
        if (streamSheetOpen && !isManga) {
            StreamSheet(
                subtitle = sheetHeading,
                streams = streams,
                loading = streamsLoading,
                subtitles = subtitles,
                selectedSubId = selectedSubId,
                onSelectSub = { selectedSubId = it },
                errors = streamErrors,
                emptyHint = if (item.type == MediaType.REAL)
                    "This NSFW add-on returned no streams. Make sure it is enabled and try again."
                else
                    "No playable streams found. Install or enable a stream add-on under Settings \u2192 Add-on Manager.",
                onDismiss = { streamSheetOpen = false },
                onPlay = { play(it) },
                onDownload = { downloadStream(it) },
            )
        }
    }
}

/**
 * The screen's single dominant call to action.
 *
 * Filled with the artwork-derived accent, a matching outer glow so it reads as *the* button on the
 * page, and the app's standard 0.96 press shrink so it feels physical like every other control.
 */
@Composable
private fun PrimaryActionButton(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    progress: Float? = null,
    icon: @Composable (() -> Unit)? = null,
) {
    val nova = LocalNovaColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.PRESS_SCALE else 1f,
        animationSpec = tween(Motion.PRESS),
        label = "primaryPress",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (enabled) 0.45f else 0f,
        animationSpec = tween(Motion.MEDIUM),
        label = "primaryGlow",
    )
    // A floating capsule pill rather than a boxy button: fully rounded, elevated, and able to carry
    // an embedded mini progress bar when there is watch/read progress to resume.
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            // Accent-tinted outer glow, faded in so it doesn't pop on first composition.
            .shadow(
                elevation = if (glowAlpha > 0f) 12.dp else 0.dp,
                shape = shape,
                ambientColor = accent,
                spotColor = accent,
            )
            .clip(shape)
            .background(if (enabled) accent else nova.surfaceElevated)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            icon?.invoke()
            if (icon != null) Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color.White else nova.textTertiary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Embedded mini progress bar hugging the bottom of the capsule.
        if (progress != null && enabled) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.25f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Color.White),
                )
            }
        }
    }
}

/**
 * A translucent circular icon button for use over artwork.
 *
 * A bare [IconButton] over a bright poster is unreadable; the dimmed circular backdrop keeps the
 * glyph legible regardless of what's behind it.
 */
@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.PRESS_SCALE else 1f,
        animationSpec = tween(Motion.PRESS),
        label = "circlePress",
    )
    // 48dp touch target (accessibility minimum); the visible disc stays 40dp so it doesn't grow
    // over the artwork.
    Box(
        Modifier
            .padding(4.dp)
            .size(48.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.38f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DetailHeader(
    item: MediaItem,
    detail: com.novastream.app.data.model.MetaDetail?,
    loading: Boolean,
    palette: PosterPalette?,
    onBack: () -> Unit,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    accent: Color,
) {
    val nova = LocalNovaColors.current
    val img = detail?.background ?: item.backdrop ?: item.poster
    // Palette-driven glow behind the artwork; animates as the color resolves / title changes.
    val glow by animateColorAsState(palette?.darkVibrantColor ?: Color.Black, label = "headerGlow")
    val halo by animateColorAsState(palette?.vibrantColor ?: Color.Transparent, label = "headerHalo")
    // Height derived from the screen width (like the home hero) rather than a fixed 16:11, so the
    // bottom title block always clears the header app bar. At 16:11 the poster+title stack grew
    // up into the back/heart buttons on narrow phones.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val headerHeight = (screenWidthDp * 0.95f).coerceIn(320f, 460f).dp
    Box(Modifier.fillMaxWidth().height(headerHeight)) {
        if (!img.isNullOrBlank()) {
            AsyncImage(
                img,
                item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        }
        // Ambient halo tinted by the artwork's dominant color.
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(halo.copy(alpha = 0.32f), Color.Transparent),
                )
            )
        )
        // Scrim for legibility, seeded from the palette's dark swatch so dark art stays cohesive.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to glow.copy(alpha = 0.55f),
                    0.45f to Color.Transparent,
                    1f to nova.background,
                )
            )
        )
        // ---- Top app bar ----------------------------------------------------
        // A dedicated scrim behind the status bar + controls. Without it the back arrow and heart
        // sat directly on raw artwork and vanished against a bright frame; both now also carry a
        // translucent circular backdrop so they stay legible over any image.
        Box(
            Modifier
                .fillMaxWidth()
                .height(104.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
        Row(
            Modifier
                .fillMaxWidth()
                // Proper status-bar inset (was a hardcoded 36dp) so the buttons never sit under or
                // on top of the system status bar, and the title below them stays clear.
                .statusBarsPadding()
                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(Modifier.weight(1f))
            // The heart moved here from the full-width button under the CTAs — it is a secondary
            // toggle, so it belongs in the app bar rather than competing with "Play".
            CircleIconButton(
                icon = if (favourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favourite) "Remove from library" else "Add to library",
                tint = if (favourite) accent else Color.White,
                onClick = onToggleFavourite,
            )
        }


        // Poster + titles overlaid at the bottom
        Row(
            Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val poster = detail?.poster ?: item.poster
            if (!poster.isNullOrBlank()) {
                AsyncImage(
                    poster, item.title, contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(96.dp).aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    detail?.name ?: item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    // Capped at 2 lines so a long title can't grow the block upward into the app bar.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val alt = detail?.altTitles?.firstOrNull()
                if (!alt.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(alt, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                MetaStatsRow(item, detail)
            }
        }
    }
}

@Composable
private fun MetaStatsRow(item: MediaItem, detail: com.novastream.app.data.model.MetaDetail?) {
    val nova = LocalNovaColors.current
    val rating = detail?.imdbRating ?: item.rating?.let { String.format("%.1f", it) }
    val votes = detail?.votes
    val year = detail?.releaseInfo ?: item.year
    val status = detail?.status
    val count = when {
        detail?.totalEpisodes != null -> "${detail.totalEpisodes} eps"
        detail?.totalChapters != null -> "${detail.totalChapters} ch"
        detail?.runtime != null -> detail.runtime
        else -> null
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        rating?.let {
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(nova.accent.copy(alpha = 0.22f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Star, null, tint = nova.accent, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(it, style = MaterialTheme.typography.labelMedium, color = nova.accent, fontWeight = FontWeight.SemiBold)
            }
        }
        votes?.takeIf { it > 0 }?.let {
            Text("${formatCount(it)} votes", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        }
        year?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f)) }
        status?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f)) }
        count?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f)) }
    }
}

@Composable
private fun ExpandableText(text: String, collapsedMaxLines: Int = 5) {
    val nova = LocalNovaColors.current
    var expanded by remember { mutableStateOf(false) }
    val isLong = text.length > 260 || text.count { it == '\n' } > collapsedMaxLines
    Column {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = nova.textSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        if (isLong) {
            Spacer(Modifier.height(6.dp))
            // Same press-shrink feedback as every other control — this used to be a bare
            // clickable with no visual response at all.
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) Motion.PRESS_SCALE else 1f,
                animationSpec = tween(Motion.PRESS),
                label = "readMorePress",
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .scale(scale)
                    .clickable(interactionSource = interaction, indication = null) { expanded = !expanded }
                    // Widen the hit area vertically to a comfortable target.
                    .heightIn(min = 32.dp),
            ) {
                Text(
                    if (expanded) "Show less" else "Read more",
                    style = MaterialTheme.typography.labelLarge,
                    color = nova.accent,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    null, tint = nova.accent, modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CastCard(member: CastMember) {
    val nova = LocalNovaColors.current
    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(nova.surfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            if (!member.image.isNullOrBlank()) {
                AsyncImage(member.image, member.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    member.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = nova.textTertiary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            member.name,
            style = MaterialTheme.typography.labelMedium,
            color = nova.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        member.character?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = nova.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The season picker (Phase 13): a horizontal scroll row of chips that replaces the old stack of
 * expandable season cards. One season is selected at a time, so the page never accumulates a card
 * (and its poster) per season before the user has even chosen one.
 */
@Composable
private fun SeasonPickerRow(
    seasons: List<com.novastream.app.data.model.SeasonInfo>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val nova = LocalNovaColors.current
    val shape = RoundedCornerShape(50)
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppSpacing.screen),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(seasons, key = { it.season }) { s ->
            val active = s.season == selected
            Box(
                Modifier
                    .clip(shape)
                    .background(if (active) nova.accent.copy(alpha = 0.20f) else nova.surfaceElevated)
                    .border(
                        BorderStroke(1.dp, if (active) nova.accent else nova.outline.copy(alpha = 0.6f)),
                        shape,
                    )
                    .clickable { onSelect(s.season) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    s.title ?: "Season ${s.season}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) nova.accent else nova.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * One episode: thumbnail, number + title, runtime/air date and (when the user has already watched
 * part of it) a saved-progress bar. Tapping opens the stream picker for this episode.
 */
@Composable
private fun EpisodeRow(
    v: Video,
    selected: Boolean,
    progress: com.novastream.app.data.model.WatchEntry?,
    onClick: () -> Unit,
) {
    val nova = LocalNovaColors.current
    val thumb = v.thumbnail
    GlassSurface(
        Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Fixed 16:9 frame so every card lines up, artwork or not.
            Box(
                Modifier
                    .width(96.dp).height(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(nova.surfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                if (!thumb.isNullOrBlank()) {
                    AsyncImage(thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.PlayArrow, null, tint = nova.textTertiary, modifier = Modifier.size(20.dp))
                }
                if (selected) {
                    Box(Modifier.fillMaxSize().background(nova.accent.copy(alpha = 0.25f)))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        v.episode?.let { append("E$it \u00b7 ") }
                        append(v.title?.takeIf { it.isNotBlank() } ?: "Episode ${v.episode ?: ""}")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) nova.accent else nova.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                // Runtime when the source knows it (TMDB), otherwise the air date.
                val meta = listOfNotNull(
                    v.runtimeMin?.takeIf { it > 0 }?.let { formatMinutes(it) },
                    v.released?.takeIf { it.isNotBlank() }?.take(10),
                ).joinToString(" \u00b7 ")
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = nova.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val watched = progress?.takeIf { it.durationMs > 0 && it.positionMs > 0 }
                if (watched != null) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { watched.progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = nova.accent,
                        trackColor = nova.outline.copy(alpha = 0.35f),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${formatClock(watched.positionMs)} / ${formatClock(watched.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = nova.textTertiary,
                    )
                }
            }
            if (selected) {
                Icon(Icons.Filled.PlayArrow, null, tint = nova.accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ChapterRow(v: Video, onClick: () -> Unit) {
    val nova = LocalNovaColors.current
    GlassSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(v.title ?: "Chapter", style = MaterialTheme.typography.titleMedium, color = nova.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                v.released?.let { Text(it.take(10), style = MaterialTheme.typography.labelMedium, color = nova.textTertiary) }
            }
            Icon(Icons.Filled.MenuBook, null, tint = nova.accent)
        }
    }
}

@Composable
private fun StreamRow(s: StreamSource, onPlay: () -> Unit, onDownload: () -> Unit) {
    val nova = LocalNovaColors.current
    // Only directly playable HTTP(S) streams can be saved for offline use.
    val downloadable = s.playableUrl != null
    GlassSurface(
        Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 4.dp),
        onClick = onPlay,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.title ?: s.name ?: "Stream", style = MaterialTheme.typography.titleMedium, color = nova.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // The raw quality string is no longer repeated here — the tiered badge conveys it.
                Text(
                    listOfNotNull(s.addonName, if (s.isTorrent) "torrent" else null).joinToString(" \u00b7 ")
                        .ifBlank { s.quality ?: "" },
                    style = MaterialTheme.typography.labelMedium, color = nova.textTertiary,
                )
            }
            ResolutionBadge(s.quality)
            if (downloadable) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Filled.Download, "Download", tint = nova.textSecondary)
                }
            }
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}

/** "45m" / "1h 05m" from a whole-minute runtime. */
private fun formatMinutes(min: Int): String = when {
    min >= 60 -> "${min / 60}h ${"%02d".format(min % 60)}m"
    else -> "${min}m"
}

/** "1:05:09" / "12:04" from milliseconds — the episode-card progress readout. */
private fun formatClock(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * The stream picker (Phase 13).
 *
 * Mirrors used to be a full-width list further down the page body — on a TV/anime title that put
 * dozens of rows between the synopsis and the recommendations, and it buried the episode list under
 * an ever-growing "Finding streams…" block. Moving them into a modal sheet keeps the page to
 * header → CTAs → genres → episodes → cast → related, while every mirror, its quality badge, its
 * add-on attribution and the offline action stay one tap away.
 *
 * Bounded to [STREAM_SHEET_MAX_HEIGHT] so the sheet never swallows the whole screen; the list
 * scrolls inside it. The weighted child uses `fill = false` so a short mirror list still collapses
 * the sheet instead of leaving dead space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamSheet(
    subtitle: String,
    streams: List<StreamSource>,
    loading: Boolean,
    subtitles: List<SubtitleTrack>,
    selectedSubId: String?,
    onSelectSub: (String?) -> Unit,
    errors: List<String>,
    emptyHint: String,
    onDismiss: () -> Unit,
    onPlay: (StreamSource) -> Unit,
    onDownload: (StreamSource) -> Unit,
) {
    val nova = LocalNovaColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = STREAM_SHEET_MAX_HEIGHT)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = AppSpacing.screen, end = 8.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Choose a stream",
                        style = MaterialTheme.typography.titleMedium,
                        color = nova.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = nova.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = nova.textSecondary)
                }
            }

            LazyColumn(
                Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when {
                    loading -> item {
                        EmptyState("Resolving mirrors\u2026", "Asking every enabled stream add-on.")
                    }
                    streams.isEmpty() -> item {
                        Column {
                            EmptyState("No streams found", emptyHint)
                            if (errors.isNotEmpty()) StreamErrorList(errors)
                        }
                    }
                    else -> items(streams) { s ->
                        StreamRow(
                            s,
                            onPlay = { onPlay(s) },
                            onDownload = { onDownload(s) },
                        )
                    }
                }

                if (subtitles.isNotEmpty()) {
                    item { SectionHeader("Subtitles") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = AppSpacing.screen),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(subtitles) { t ->
                                TagChip(t.lang.ifBlank { t.id }, selected = t.id == selectedSubId) {
                                    // Tap the active chip again to clear and let the player auto-pick.
                                    onSelectSub(if (t.id == selectedSubId) null else t.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Ceiling for the stream picker so the sheet never covers the entire screen. */
private val STREAM_SHEET_MAX_HEIGHT = 560.dp

/**
 * Per-add-on failure reasons shown under an empty stream list, so a blank picker is not mistaken
 * for "this title has no mirrors" when in fact every add-on timed out or returned an error.
 */
@Composable
private fun StreamErrorList(errors: List<String>) {
    val nova = LocalNovaColors.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Some add-ons could not be reached",
            style = MaterialTheme.typography.labelLarge,
            color = nova.textSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        errors.take(8).forEach { err ->
            Text(
                "\u2022 $err",
                style = MaterialTheme.typography.labelMedium,
                color = nova.textTertiary,
            )
        }
        if (errors.size > 8) {
            Text(
                "and ${errors.size - 8} more\u2026",
                style = MaterialTheme.typography.labelMedium,
                color = nova.textTertiary,
            )
        }
    }
}
