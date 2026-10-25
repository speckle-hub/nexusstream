package com.moovio.desktop.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Icons
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovio.desktop.data.local.ContinueEntry
import com.moovio.desktop.data.local.LibraryItem
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.ui.nav.NavController
import com.moovio.desktop.ui.components.RemoteImage
import com.moovio.desktop.ui.components.PosterCard
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.AccentViolet
import com.moovio.desktop.ui.theme.MoovioSurface
import com.moovio.desktop.ui.theme.MoovioTextPrimary
import com.moovio.desktop.ui.theme.MoovioTextSecondary
import com.moovio.desktop.ui.theme.moovioColors

@Composable
fun LibraryScreen(container: AppContainer, nav: NavController) {
    val favorites by remember { mutableStateOf(container.library.favoriteItems()) }
    val continueList by remember { mutableStateOf(container.library.continueWatchingList) }

    Column(
        modifier = Modifier.fillMaxSize().padding(AppSpacing.xxl),
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.Start,
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.h3,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = AppSpacing.xl),
        )

        // Favorites
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.h4,
            color = MoovioTextPrimary,
            modifier = Modifier.padding(bottom = AppSpacing.m),
        )
        if (favorites.isEmpty()) {
            Text("No favorites yet.", style = MaterialTheme.typography.body1, color = MoovioTextSecondary)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AppSpacing.m),
            ) {
                favorites.forEach { item ->
                    PosterCard(
                        item = MediaItem(item.mediaId, MediaType.MOVIE, item.title, poster = item.poster),
                        onClick = { nav.openDetail(item.mediaId, item.title) },
                        modifier = Modifier.width(140.dp),
                    )
                    IconButton(
                        onClick = { confirmRemove = item.mediaId; container.library.toggleFavorite(item) },
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = MoovioTextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xxl))

        // Continue watching
        Text(
            text = "Continue Watching",
            style = MaterialTheme.typography.h4,
            color = MoovioTextPrimary,
            modifier = Modifier.padding(bottom = AppSpacing.m),
        )
        if (continueList.isEmpty()) {
            Text("Nothing to resume.", style = MaterialTheme.typography.body1, color = MoovioTextSecondary)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(continueList, key = { it.mediaId }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.s)
                            .clickable { nav.openDetail(entry.mediaId, entry.title) },
                    ) {
                        RemoteImage(
                            url = entry.poster,
                            modifier = Modifier.size(56.dp, 80.dp),
                            contentDescription = entry.title,
                        )
                        Column(
                            modifier = Modifier.weight(1f).padding(start = AppSpacing.m),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        ) {
                            Text(text = entry.title, style = MaterialTheme.typography.subtitle1, color = MoovioTextPrimary)
                            Text(
                                text = "Resume · ${entry.type}",
                                style = MaterialTheme.typography.body2,
                                color = MoovioTextSecondary,
                            )
                        }
                        IconButton(onClick = { container.library.removeContinue(entry.mediaId) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = MoovioTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

private object Spacer {
    fun modifier(modifier: Modifier = Modifier) = modifier
}


