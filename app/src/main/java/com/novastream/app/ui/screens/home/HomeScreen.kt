package com.novastream.app.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.novastream.app.data.model.CatalogRow
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.WatchEntry
import com.novastream.app.ui.components.ConfirmRemoveDialog
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.LoadingRow
import com.novastream.app.ui.components.MediaRow
import com.novastream.app.ui.components.SectionHeader
import com.novastream.app.ui.components.ShimmerBox
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.rememberPosterPalette
import com.novastream.app.ui.vm.HomeViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(nav: NavHostController) {
    val vm = novaViewModel { HomeViewModel(it) }
    val rows by vm.rows.collectAsStateSafe()
    val loading by vm.loading.collectAsStateSafe()
    val error by vm.error.collectAsStateSafe()
    val continueWatching by vm.continueWatching.collectAsStateSafe()

    // Featured carousel pool: trending titles first (they have backdrops), then the rest of the
    // feed, deduped. Prefer items with real artwork so a premium banner never falls back to a
    // portrait poster unless nothing else exists.
    val featured = remember(rows) {
        val trending = rows.firstOrNull { it.title.contains("Trending", true) }?.items.orEmpty()
        val pool = (trending + rows.flatMap { it.items }).distinctBy { it.key }
        val withArt = pool.filter { !it.backdrop.isNullOrBlank() }
        (withArt.ifEmpty { pool }).take(8)
    }

    // Entry awaiting confirmation before it is dropped from Continue Watching.
    var pendingRemoval by remember { mutableStateOf<WatchEntry?>(null) }

    // Keeps the Home scroll position when returning from another tab or a detail screen.
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 44.dp, bottom = 28.dp),
    ) {
        item {
            HomeTopBar(
                onAddons = { nav.navigate(Routes.ADDONS) },
                onRefresh = { vm.refresh() },
            )
        }

        // ---- Featured carousel ----------------------------------------------
        if (featured.isNotEmpty()) {
            item {
                FeaturedCarousel(featured) { nav.navigate(Routes.detail(it)) }
            }
        } else if (loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ShimmerBox(Modifier.fillMaxWidth().aspectRatio(16f / 9f), RoundedCornerShape(20.dp))
                }
            }
        }

        if (continueWatching.isNotEmpty()) {
            item {
                ContinueWatchingRow(
                    entries = continueWatching,
                    onClick = { nav.navigate(Routes.detail(it.item)) },
                    onRemove = { pendingRemoval = it },
                )
            }
        }

        if (loading && rows.isEmpty()) {
            items(4) { LoadingRow() }
        }
        if (!loading && rows.isEmpty()) {
            item {
                EmptyState(
                    "Nothing here yet",
                    "Install add-ons from the Add-on Manager to populate your home feed.",
                )
            }
        }
        error?.let {
            item { EmptyState("Couldn't load content", it) }
        }

        items(rows) { row ->
            MediaRow(
                title = row.title,
                items = row.items,
                onClick = { nav.navigate(Routes.detail(it)) },
                cardWidth = 150,
                trailing = {
                    SeeAll { nav.navigate(Routes.collection(row.type)) }
                },
            )
        }
    }

    pendingRemoval?.let { entry ->
        ConfirmRemoveDialog(
            title = "Remove from Continue Watching?",
            message = "\u201c${entry.item.title}\u201d will be removed and its saved progress cleared.",
            onConfirm = {
                vm.removeContinueWatching(entry)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }
    }
}

/**
 * Full-bleed featured banners with swipe support and overlaying dot indicators.
 *
 * The artwork runs edge-to-edge (no card, radius, border or side padding) and fades into the
 * page background through top/bottom gradient scrims. Auto-advance pauses while dragging.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedCarousel(items: List<MediaItem>, onClick: (MediaItem) -> Unit) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
            }
        }
    }

    Box(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(0.dp),
            pageSpacing = 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            HeroSlide(items[page], onClick)
        }
        // Pagination indicators overlay the bottom gradient of the active slide.
        CarouselDots(
            count = items.size,
            selected = pagerState.currentPage,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
        )
    }
}

@Composable
private fun CarouselDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    if (count <= 1) return
    Row(
        modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val active = i == selected
            val width by animateDpAsState(if (active) 24.dp else 7.dp, label = "dotWidth")
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(50))
                    // White reads cleanly over the artwork/gradient at every palette tint.
                    .background(if (active) Color.White else Color.White.copy(alpha = 0.35f)),
            )
        }
    }
}

/** Centre-aligned metadata line, e.g. "Movie • Action • 2024 • ★ 7.8". */
private fun heroMeta(item: MediaItem): String = buildList {
    // Labels are plural ("Movies", "TV Shows"); drop the plural for the single-title row.
    add(item.type.label.removeSuffix("s"))
    item.genres.firstOrNull()?.let { add(it) }
    item.year?.takeIf { it.isNotBlank() }?.let { add(it) }
    item.rating?.let { add(String.format("%.1f", it)) }
}.joinToString(" \u00b7 ")

/**
 * One immersive hero slide: full-width artwork, top/bottom scrims that blend into the page
 * background, and centre-aligned title, metadata and pill CTA.
 */
@Composable
private fun HeroSlide(item: MediaItem, onClick: (MediaItem) -> Unit) {
    val nova = LocalNovaColors.current
    val img = item.backdrop ?: item.poster
    // Dynamic theming: the accent + scrim shift to match each banner's artwork as it changes.
    val palette = rememberPosterPalette(img)
    val glow by animateColorAsState(palette?.vibrantColor ?: nova.accent, label = "heroAccent")
    val scrim by animateColorAsState(palette?.darkVibrantColor ?: Color.Black, label = "heroScrim")

    // Immersive portrait-ish height, clamped so tablets/landscape never stretch it into the feed.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val heroHeight = (screenWidthDp / 0.84f).coerceIn(400f, 540f).dp

    Box(
        Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clickable { onClick(item) },
    ) {
        if (!img.isNullOrBlank()) {
            AsyncImage(img, item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        }
        // Ambient halo tinted by the artwork's vibrant swatch.
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(glow.copy(alpha = 0.24f), Color.Transparent))
            )
        )
        // Top + bottom scrims: the art fades into the page background at both edges.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.20f to Color.Transparent,
                    0.48f to Color.Transparent,
                    0.76f to scrim.copy(alpha = 0.60f),
                    1f to nova.background,
                )
            )
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Bottom room for the overlaying pagination dots.
                .padding(start = 24.dp, end = 24.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "FEATURED",
                style = MaterialTheme.typography.labelSmall,
                color = glow,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                heroMeta(item),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(8.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.80f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            // Pill CTA.
            Button(
                onClick = { onClick(item) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 26.dp, vertical = 11.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Play", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SeeAll(onClick: () -> Unit) {
    val nova = LocalNovaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onClick() }.padding(start = 8.dp),
    ) {
        Text("See all", style = MaterialTheme.typography.labelLarge, color = nova.accent, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Filled.ChevronRight, null, tint = nova.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HomeTopBar(onAddons: () -> Unit, onRefresh: () -> Unit) {
    val nova = LocalNovaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "NexusStream",
                style = MaterialTheme.typography.displaySmall,
                color = nova.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Movies \u00b7 TV \u00b7 Anime \u00b7 Manga",
                style = MaterialTheme.typography.labelMedium,
                color = nova.textTertiary,
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, "Refresh", tint = nova.textSecondary)
        }
        IconButton(onClick = onAddons) {
            Icon(Icons.Filled.Extension, "Add-ons", tint = nova.accent)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingRow(
    entries: List<WatchEntry>,
    onClick: (WatchEntry) -> Unit,
    onRemove: (WatchEntry) -> Unit,
) {
    val nova = LocalNovaColors.current
    Column(Modifier.fillMaxWidth()) {
        SectionHeader("Continue Watching")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { it.item.key }) { entry ->
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        // Tap opens the title; long-press offers the remove path.
                        .combinedClickable(onClick = { onClick(entry) }, onLongClick = { onRemove(entry) }),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(nova.surfaceElevated),
                    ) {
                        val img = entry.item.backdrop ?: entry.item.poster
                        if (!img.isNullOrBlank()) {
                            AsyncImage(img, entry.item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))),
                        )
                        Icon(
                            Icons.Filled.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(40.dp),
                        )
                        // progress bar
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.White.copy(alpha = 0.25f)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(entry.progress.coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .background(nova.accent),
                            )
                        }
                        // Small "×" so the entry can be dropped from Continue Watching.
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(26.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.62f))
                                .clickable { onRemove(entry) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                "Remove from Continue Watching",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        entry.item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = nova.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.videoTitle?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = nova.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
