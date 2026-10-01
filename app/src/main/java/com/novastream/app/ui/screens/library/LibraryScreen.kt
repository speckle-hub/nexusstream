package com.novastream.app.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.data.model.MediaItem
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.PosterCard
import com.novastream.app.ui.components.SectionHeader
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.LibraryViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

@Composable
fun LibraryScreen(nav: NavHostController) {
    val vm = novaViewModel { LibraryViewModel(it) }
    val nova = LocalNovaColors.current
    val favorites by vm.favorites.collectAsStateSafe()
    val history by vm.history.collectAsStateSafe()

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
