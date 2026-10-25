package com.moovio.desktop.ui.screens.detail

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subtitles
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.Episode
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.domain.model.StreamSource
import com.moovio.desktop.ui.components.CircleIconButton
import com.moovio.desktop.ui.components.EpisodeRow
import com.moovio.desktop.ui.components.FilterChip
import com.moovio.desktop.ui.components.GhostButton
import com.moovio.desktop.ui.components.LoadingIndicator
import com.moovio.desktop.ui.components.PrimaryButton
import com.moovio.desktop.ui.components.RemoteImage
import com.moovio.desktop.ui.components.formatDuration
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.theme.AccentColor
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.glass

@Composable
fun DetailScreen(
    app: AppState,
    container: AppContainer,
    item: MediaItem,
    onBack: () -> Unit,
) {
    val viewModel = remember(item.key) { DetailViewModel(container, item) }
    DisposableEffect(viewModel) { onDispose { viewModel.dispose() } }
    val state by viewModel.state.collectAsState()
    val streamsState by viewModel.streams.collectAsState()
    val library by container.library.state.collectAsState()

    var selectedSeason by remember(item.key) { mutableStateOf<Int?>(null) }
    var sourcesOpen by remember { mutableStateOf(false) }
    var sourcesEpisode by remember { mutableStateOf<Pair<Int?, Int?>?>(null) }

    LaunchedEffect(item.key) { viewModel.load() }

    val detail = state.detail
    val media = detail?.toMediaItem() ?: item
    val progress = library.progress.firstOrNull { it.mediaKey == media.key }?.takeIf { !it.finished }
    val seasons = detail?.episodes?.groupBy { it.season }?.keys?.sorted().orEmpty()
    val activeSeason = selectedSeason
        ?.takeIf { seasons.contains(it) }
        ?: progress?.season?.takeIf { seasons.contains(it) }
        ?: seasons.firstOrNull()

    val resumeEpisode = detail?.episodes?.firstOrNull {
        it.season == progress?.season && it.episode == progress?.episode
    }
    val selectedEpisode = detail?.episodes?.firstOrNull { it.season == activeSeason }
    val playEpisode = resumeEpisode ?: selectedEpisode

    val isSeries = detail?.episodes?.isNotEmpty() == true
    val inLibrary = library.favorites.any { it.key == media.key }

    fun play(episode: Episode?, source: StreamSource? = null) {
        val start = progress
            ?.takeIf { episode == null || (it.season == episode.season && it.episode == episode.episode) }
            ?.positionMs ?: 0
        app.requestPlay(
            PlayTarget(
                media = media,
                season = episode?.season ?: if (isSeries) playEpisode?.season else null,
                episode = episode?.episode ?: if (isSeries) playEpisode?.episode else null,
                episodeTitle = episode?.title ?: playEpisode?.title,
                startPositionMs = start,
                source = source,
                detail = detail,
            ),
        )
    }

    fun openSources(episode: Episode?) {
        sourcesEpisode = episode?.season to episode?.episode
        sourcesOpen = true
        viewModel.loadStreams(episode?.season, episode?.episode)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 56.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item("hero") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(468.dp)
                        .background(Palette.backgroundElevated),
                ) {
                    RemoteImage(
                        url = detail?.background ?: item.background ?: detail?.poster ?: item.poster,
                        contentDescription = media.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        Palette.background.copy(alpha = 0.75f),
                                        Palette.background,
                                    ),
                                ),
                            ),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Palette.background.copy(alpha = 0.85f),
                                        Color.Transparent,
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        modifier = Modifier.padding(start = 34.dp, top = 28.dp),
                        size = 40.dp,
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 56.dp, end = 56.dp, bottom = 34.dp)
                            .fillMaxWidth(0.68f),
                    ) {
                        if (state.loading) {
                            Text(
                                "Loading details…",
                                style = MaterialTheme.typography.labelMedium,
                                color = Palette.textTertiary,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                media.type.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentColor,
                                fontWeight = FontWeight.Bold,
                            )
                            (detail?.year ?: media.year)?.let {
                                Text("·", color = Palette.textTertiary)
                                Text(it.toString(), style = MaterialTheme.typography.labelMedium, color = Palette.textSecondary)
                            }
                            (detail?.runtime ?: media.runtime)?.let {
                                Text("·", color = Palette.textTertiary)
                                Text(it, style = MaterialTheme.typography.labelMedium, color = Palette.textSecondary)
                            }
                            (detail?.rating ?: media.rating)?.let {
                                Text("·", color = Palette.textTertiary)
                                Icon(Icons.Rounded.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFC857))
                                Text(
                                    "%.1f".format(it),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Palette.textSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            media.name,
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val genres = detail?.genres ?: media.genres
                        if (genres.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                genres.take(4).joinToString("  ·  "),
                                style = MaterialTheme.typography.labelMedium,
                                color = Palette.textSecondary,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            detail?.description ?: media.description ?: state.error ?: "No synopsis available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.textSecondary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val playLabel = when {
                                isSeries && resumeEpisode != null ->
                                    "Resume · S${resumeEpisode.season} E${resumeEpisode.episode}"
                                isSeries -> "Play · S${playEpisode?.season ?: 1} E${playEpisode?.episode ?: 1}"
                                progress != null -> "Resume · ${formatDuration(progress.positionMs)}"
                                else -> "Play"
                            }
                            PrimaryButton(
                                text = playLabel,
                                icon = Icons.Rounded.PlayArrow,
                                onClick = {
                                    if (isSeries) play(playEpisode) else play(null)
                                },
                                enabled = !state.loading,
                            )
                            GhostButton(
                                text = "Sources",
                                icon = Icons.Rounded.Subtitles,
                                onClick = { openSources(if (isSeries) playEpisode else null) },
                            )
                            GhostButton(
                                text = if (inLibrary) "In Library" else "Add to Library",
                                icon = if (inLibrary) Icons.Rounded.Check else Icons.Rounded.Add,
                                active = inLibrary,
                                onClick = {
                                    val added = container.library.toggleFavorite(media)
                                    app.showToast(if (added) "Added to Library" else "Removed from Library")
                                },
                            )
                        }
                    }
                }
            }

            if (detail?.cast?.isNotEmpty() == true) {
                item("cast") {
                    Row(
                        Modifier.padding(horizontal = 56.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Starring",
                            style = MaterialTheme.typography.labelMedium,
                            color = Palette.textTertiary,
                        )
                        Text(
                            detail?.cast?.take(6)?.joinToString(", ").orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (isSeries && detail != null) {
                item("seasons") {
                    Column(Modifier.padding(top = 22.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 56.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Episodes",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Palette.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${detail.episodes.size} episodes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Palette.textTertiary,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 56.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(seasons) { season ->
                                FilterChip(
                                    text = if (season == 0) "Specials" else "Season $season",
                                    selected = activeSeason == season,
                                    onClick = { selectedSeason = season },
                                )
                            }
                        }
                    }
                }
                items(
                    detail.episodes.filter { it.season == activeSeason },
                    key = { it.id },
                ) { episode ->
                    EpisodeRow(
                        episode = episode,
                        onClick = { play(episode) },
                        modifier = Modifier.padding(horizontal = 56.dp, vertical = 5.dp),
                        progress = if (progress?.episode == episode.episode && progress?.season == episode.season) {
                            progress?.fraction ?: 0f
                        } else 0f,
                        isNextUp = episode == resumeEpisode,
                    )
                }
            } else if (detail != null) {
                item("movie-info") {
                    Row(
                        Modifier.padding(horizontal = 56.dp, vertical = 26.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        InfoCard("Type", detail.type.label, Modifier.weight(1f))
                        InfoCard("Released", detail.releaseInfo ?: detail.year?.toString() ?: "—", Modifier.weight(1f))
                        InfoCard("Rating", detail.rating?.let { "%.1f / 10".format(it) } ?: "—", Modifier.weight(1f))
                        InfoCard("Runtime", detail.runtime ?: "—", Modifier.weight(1f))
                    }
                }
            }

            item("footer") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 56.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Metadata from ${if (media.id.startsWith("anilist:")) "AniList" else "Cinemeta"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.textTertiary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        media.id,
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.textTertiary.copy(alpha = 0.6f),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = sourcesOpen,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SourcesOverlay(
                sources = streamsState,
                title = media.name,
                onDismiss = { sourcesOpen = false },
                onPlay = { source ->
                    sourcesOpen = false
                    val ep = sourcesEpisode?.let { (season, number) ->
                        detail?.episodes?.firstOrNull { it.season == season && it.episode == number }
                    }
                    play(ep, source)
                },
                onReload = { viewModel.loadStreams(sourcesEpisode?.first, sourcesEpisode?.second) },
            )
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .glass(RoundedCornerShape(14.dp), alpha = 0.045f)
            .padding(16.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.textTertiary)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Palette.textPrimary)
    }
}

@Composable
private fun SourcesOverlay(
    sources: com.moovio.desktop.ui.screens.detail.StreamsUiState,
    title: String,
    onDismiss: () -> Unit,
    onPlay: (StreamSource) -> Unit,
    onReload: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.76f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(0.7f)
                .heightIn(max = 640.dp)
                .glass(RoundedCornerShape(22.dp), alpha = 0.09f)
                .background(Palette.background.copy(alpha = 0.92f), RoundedCornerShape(22.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Choose a source", style = MaterialTheme.typography.headlineMedium, color = Palette.textPrimary)
                    Text(
                        title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                GhostButton("Refresh", onClick = onReload)
            }
            Spacer(Modifier.height(18.dp))

            when {
                sources.loading -> Box(Modifier.fillMaxWidth().height(220.dp)) { LoadingIndicator() }
                sources.sources.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.CloudOff, null, Modifier.size(34.dp), tint = Palette.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No streams available",
                        style = MaterialTheme.typography.titleMedium,
                        color = Palette.textPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Install a stream add-on in Settings → Add-ons. Torrent-only results can't be played directly in v1.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.textTertiary,
                        modifier = Modifier.widthIn(max = 420.dp),
                    )
                    if (sources.failures.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            sources.failures.joinToString(" · ") { "${it.addon}: ${it.message}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.warning,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(sources.sources, key = { it.fingerprint + it.addonId }) { source ->
                        SourceRow(source = source, onPlay = { onPlay(source) })
                    }
                    if (sources.failures.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Issues: " + sources.failures.joinToString(" · ") { "${it.addon} ${it.message}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = Palette.warning,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(source: StreamSource, onPlay: () -> Unit) {
    val playable = source.isPlayable
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .glass(RoundedCornerShape(12.dp), alpha = 0.04f)
            .clickable(enabled = playable, onClick = onPlay)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    source.addonName,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(AccentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                source.quality?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                source.seeders?.let {
                    Text(
                        "$it seeders",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.textTertiary,
                    )
                }
                if (source.isTorrent) {
                    Text(
                        "TORRENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.warning,
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                source.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (playable) Palette.textPrimary else Palette.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(14.dp))
        if (playable) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(AccentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, "Play", Modifier.size(19.dp), tint = AccentColor)
            }
        } else {
            Text(
                "Not in v1",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.textTertiary,
            )
        }
    }
}
