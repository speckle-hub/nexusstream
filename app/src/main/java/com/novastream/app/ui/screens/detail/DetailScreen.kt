package com.novastream.app.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.novastream.app.data.model.CastMember
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.Video
import com.novastream.app.data.remote.TorrentStreamer
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.components.MediaRow
import com.novastream.app.ui.components.RatingBadge
import com.novastream.app.ui.components.SectionHeader
import com.novastream.app.ui.components.ShimmerBox
import com.novastream.app.ui.components.TagChip
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.player.PlayerActivity
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.PosterPalette
import com.novastream.app.ui.theme.rememberPosterPalette
import com.novastream.app.ui.vm.DetailViewModel
import com.novastream.app.ui.vm.LocalContainer
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

@Composable
fun DetailScreen(nav: NavHostController, item: MediaItem) {
    val vm = novaViewModel(key = item.key) { DetailViewModel(it, item) }
    val nova = LocalNovaColors.current
    val context = LocalContext.current
    val detail by vm.detail.collectAsStateSafe()
    val streams by vm.streams.collectAsStateSafe()
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

    // Dynamic Material You theming: derive an accent from the artwork and animate to it.
    val palette = rememberPosterPalette(detail?.background ?: item.backdrop ?: item.poster)
    val accent by animateColorAsState(palette?.vibrantColor ?: nova.accent, label = "detailAccent")

    // Flat, ordered episode list + the currently selected episode's index, handed to the player
    // so it can autoplay the next episode and populate its episode sheet.
    val orderedEpisodes = remember(seasonEpisodes) {
        seasonEpisodes.values.flatten().sortedWith(compareBy({ it.season ?: 0 }, { it.episode ?: 0 }))
    }
    val currentEpisodeIndex = orderedEpisodes.indexOfFirst { it.id == selectedVideo?.id }

    fun openExternal(source: StreamSource) {
        val url = source.playableUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(url), "video/*") }
        runCatching { context.startActivity(intent) }
    }

    fun play(source: StreamSource) {
        // 1) A direct HTTP(S) stream — play immediately.
        if (source.playableUrl != null) {
            if (external) openExternal(source)
            else context.startActivity(
                PlayerActivity.intent(context, item, source, subtitles, orderedEpisodes, currentEpisodeIndex)
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
                        PlayerActivity.intent(context, item, playable, subtitles, orderedEpisodes, currentEpisodeIndex)
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
            )
        }

        // ---- Action buttons --------------------------------------------------
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isManga) {
                        val canRead = chapters.isNotEmpty()
                        Button(
                            onClick = {
                                chapters.firstOrNull()?.let {
                                    context.startActivity(com.novastream.app.ui.player.MangaReaderActivity.intent(context, item, it))
                                }
                            },
                            enabled = canRead,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.MenuBook, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Read", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        val firstPlayable = streams.firstOrNull {
                            it.playableUrl != null || it.infoHash != null || it.ytId != null
                        }
                        Button(
                            onClick = { firstPlayable?.let { play(it) } },
                            enabled = !streamsLoading && firstPlayable != null,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (streamsLoading && streams.isEmpty()) "Finding\u2026" else "Play",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    detail?.trailerUrl?.let { url ->
                        OutlinedButton(
                            onClick = { openTrailer(url) },
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Outlined.SmartDisplay, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Trailer")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.toggleFavorite() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (fav) accent.copy(alpha = 0.22f) else nova.surfaceElevated,
                        contentColor = if (fav) accent else nova.textPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (fav) "In Library" else "Add to Library", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
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
            item { SectionHeader(if (item.type == MediaType.SERIES) "Seasons & Episodes" else "Episodes") }
            seasons.forEach { s ->
                val expanded = expandedSeason == s.season
                val eps = seasonEpisodes[s.season].orEmpty()
                item(key = "season-${s.season}") {
                    SeasonRow(
                        title = s.title ?: "Season ${s.season}",
                        poster = s.poster,
                        subtitle = when {
                            seasonLoading == s.season -> "Loading episodes\u2026"
                            eps.isNotEmpty() -> "${eps.size} episodes"
                            expanded -> "No episodes found"
                            s.episodeCount != null -> "${s.episodeCount} episodes"
                            else -> "Tap to show episodes"
                        },
                        expanded = expanded,
                        loading = seasonLoading == s.season,
                        onClick = { vm.toggleSeason(s.season) },
                    )
                }
                if (expanded && eps.isNotEmpty()) {
                    items(eps, key = { it.id }) { v ->
                        EpisodeRow(v, selected = selectedVideo?.id == v.id, indented = true) { vm.loadStreams(v) }
                    }
                }
            }
        }

        // ---- Stream sources --------------------------------------------------
        if (!isManga) {
            item { SectionHeader(if (streamsLoading) "Finding streams\u2026" else "Streams (${streams.size})") }
            if (streamsLoading) {
                item { EmptyState("Searching add-ons\u2026", "Querying every enabled stream add-on.") }
            } else if (streams.isEmpty()) {
                item {
                    EmptyState(
                        "No streams found",
                        if (item.type == MediaType.REAL)
                            "This NSFW add-on returned no streams. Make sure it is enabled and try again."
                        else
                            "Install a stream add-on (e.g. Cinemeta + a stream provider).",
                    )
                }
            } else {
                items(streams) { s -> StreamRow(s) { play(s) } }
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
    }
}

@Composable
private fun DetailHeader(
    item: MediaItem,
    detail: com.novastream.app.data.model.MetaDetail?,
    loading: Boolean,
    palette: PosterPalette?,
    onBack: () -> Unit,
) {
    val nova = LocalNovaColors.current
    val img = detail?.background ?: item.backdrop ?: item.poster
    // Palette-driven glow behind the artwork; animates as the color resolves / title changes.
    val glow by animateColorAsState(palette?.darkVibrantColor ?: Color.Black, label = "headerGlow")
    val halo by animateColorAsState(palette?.vibrantColor ?: Color.Transparent, label = "headerHalo")
    Box(Modifier.fillMaxWidth().aspectRatio(16f / 11f)) {
        if (!img.isNullOrBlank()) {
            AsyncImage(img, item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 36.dp, start = 4.dp)) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded },
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

@Composable
private fun SeasonRow(
    title: String,
    poster: String?,
    subtitle: String,
    expanded: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val nova = LocalNovaColors.current
    GlassSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!poster.isNullOrBlank()) {
                AsyncImage(
                    poster, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.width(48.dp).height(72.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = nova.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = nova.textTertiary)
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = nova.accent,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    null, tint = nova.accent, modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(v: Video, selected: Boolean, indented: Boolean = false, onClick: () -> Unit) {
    val nova = LocalNovaColors.current
    val startPad = if (indented) 30.dp else 16.dp
    GlassSurface(
        Modifier.fillMaxWidth().padding(start = startPad, end = 16.dp, top = 4.dp, bottom = 4.dp),
        onClick = onClick,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!v.thumbnail.isNullOrBlank()) {
                AsyncImage(v.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.width(96.dp).height(54.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        v.episode?.let { append("E$it \u00b7 ") }
                        append(v.title?.takeIf { it.isNotBlank() } ?: "Episode ${v.episode ?: ""}")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) nova.accent else nova.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                v.overview?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = nova.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                v.released?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = nova.textTertiary)
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
private fun StreamRow(s: StreamSource, onClick: () -> Unit) {
    val nova = LocalNovaColors.current
    GlassSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.title ?: s.name ?: "Stream", style = MaterialTheme.typography.titleMedium, color = nova.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(s.addonName, s.quality, if (s.isTorrent) "torrent" else null).joinToString(" \u00b7 "),
                    style = MaterialTheme.typography.labelMedium, color = nova.textTertiary,
                )
            }
            s.quality?.let { RatingBadge(it) }
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}
