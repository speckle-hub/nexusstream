package com.moovio.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovio.desktop.data.local.ContinueEntry
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.ui.nav.NavController
import com.moovio.desktop.ui.components.HeroCarousel
import com.moovio.desktop.ui.components.MediaRow
import com.moovio.desktop.ui.components.PosterCard
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.MoovioTextSecondary
import com.moovio.desktop.ui.theme.moovioColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Home: full-bleed hero carousel, Continue Watching, per-type catalog rows. */
@Composable
fun HomeScreen(container: AppContainer, nav: NavController) {
    val featured = remember {
        listOf(
            MediaItem("feat-1", MediaType.MOVIE, "Interstellar", 2014, 8.6, backdrop = null, genre = "Sci-Fi · Adventure"),
            MediaItem("feat-2", MediaType.TV, "The Last of Us", 2023, 8.7, genre = "Drama · Horror"),
            MediaItem("feat-3", MediaType.ANIME, "Cyberpunk: Edgerunners", 2022, 8.4, genre = "Anime · Action"),
            MediaItem("feat-4", MediaType.MOVIE, "Dune: Part Two", 2024, 8.5, genre = "Sci-Fi · Epic"),
        )
    }

    val continueItems by container.library.continueWatchingList.map { it } // observed below
    val continueList = remember(container.library) { mutableStateOf<List<ContinueEntry>>(emptyList()) }
    LaunchedEffect(container.library) {
        withContext(Dispatchers.Default) {
            continueList.value = container.library.continueWatchingList
        }
    }

    val catalogRows = remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    LaunchedEffect(container) {
        withContext(Dispatchers.Default) {
            val rows = mapOf(
                "Movies" to runCatching { container.catalogRepository.homeRow(MediaType.MOVIE, "trending") }.getOrDefault(emptyList()),
                "TV" to runCatching { container.catalogRepository.homeRow(MediaType.TV, "trending") }.getOrDefault(emptyList()),
                "Anime" to runCatching { container.catalogRepository.homeRow(MediaType.ANIME, "trending") }.getOrDefault(emptyList()),
            )
            catalogRows.value = rows
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = AppSpacing.xxl),
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.Start,
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.h3,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = AppSpacing.xxl, bottom = AppSpacing.xl),
        )

        HeroCarousel(
            items = featured,
            onItemClick = { nav.openDetail(it.id, it.title) },
            modifier = Modifier.fillMaxWidth().height(420.dp),
        )

        if (continueList.value.isNotEmpty()) {
            Text(
                text = "Continue Watching",
                style = MaterialTheme.typography.h4,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = AppSpacing.xxl, top = AppSpacing.xxl, bottom = AppSpacing.m),
            )
            MediaRow(
                title = "Continue Watching",
                items = continueList.value.map { entry ->
                    MediaItem(entry.mediaId, MediaType.MOVIE, entry.title, poster = entry.poster)
                },
                onItemClick = { nav.openDetail(it.id, it.title) },
            )
        }

        catalogRows.value.entries.forEach { (label, items) ->
            MediaRow(
                title = label,
                items = items,
                onItemClick = { nav.openDetail(it.id, it.title) },
                modifier = Modifier.padding(horizontal = AppSpacing.xxl, top = AppSpacing.m),
            )
        }
    }
}


