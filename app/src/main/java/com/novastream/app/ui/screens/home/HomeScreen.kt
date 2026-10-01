package com.novastream.app.ui.screens.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.novastream.app.data.model.CatalogRow
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.WatchEntry
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.LoadingRow
import com.novastream.app.ui.components.MediaRow
import com.novastream.app.ui.components.SectionHeader
import com.novastream.app.ui.components.ShimmerBox
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.HomeViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

@Composable
fun HomeScreen(nav: NavHostController) {
    val vm = novaViewModel { HomeViewModel(it) }
    val nova = LocalNovaColors.current
    val rows by vm.rows.collectAsStateSafe()
    val loading by vm.loading.collectAsStateSafe()
    val error by vm.error.collectAsStateSafe()
    val continueWatching by vm.continueWatching.collectAsStateSafe()

    val featured = remember(rows) {
        rows.firstOrNull { it.title.contains("Trending", true) }?.items?.firstOrNull()
            ?: rows.firstOrNull()?.items?.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 44.dp, bottom = 28.dp),
    ) {
        item {
            HomeTopBar(
                onAddons = { nav.navigate(Routes.ADDONS) },
                onRefresh = { vm.refresh() },
            )
        }

        // ---- Hero / featured banner -----------------------------------------
        if (featured != null) {
            item {
                HeroBanner(featured) { nav.navigate(Routes.detail(it)) }
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
                ContinueWatchingRow(continueWatching) { entry ->
                    nav.navigate(Routes.detail(entry.item))
                }
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
}

@Composable
private fun HeroBanner(item: MediaItem, onClick: (MediaItem) -> Unit) {
    val nova = LocalNovaColors.current
    val img = item.backdrop ?: item.poster
    val heroShape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            // Ambient drop-shadow + hairline outline so dark poster art separates from the background.
            .shadow(elevation = 16.dp, shape = heroShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(heroShape)
            .border(BorderStroke(1.dp, nova.outline.copy(alpha = 0.6f)), heroShape)
            .clickable { onClick(item) },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
            if (!img.isNullOrBlank()) {
                AsyncImage(img, item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
            }
            // Cinematic gradient scrims
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.15f),
                        0.5f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Black.copy(alpha = 0.85f),
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                    )
                )
            )

            Column(
                Modifier.align(Alignment.BottomStart).padding(18.dp),
            ) {
                Text(
                    "FEATURED",
                    style = MaterialTheme.typography.labelSmall,
                    color = nova.accent,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.rating?.let { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC94D), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(String.format("%.1f", r), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    item.year?.let { Text(it, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium) }
                    if (item.genres.isNotEmpty()) {
                        Text(item.genres.take(2).joinToString(" \u00b7 "), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onClick(item) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }
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

@Composable
private fun ContinueWatchingRow(entries: List<WatchEntry>, onClick: (WatchEntry) -> Unit) {
    val nova = LocalNovaColors.current
    Column(Modifier.fillMaxWidth()) {
        SectionHeader("Continue Watching")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { it.item.key }) { entry ->
                Column(
                    modifier = Modifier.width(220.dp).clickable { onClick(entry) },
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
