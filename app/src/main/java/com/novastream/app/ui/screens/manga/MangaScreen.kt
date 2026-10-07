package com.novastream.app.ui.screens.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.components.LoadingRow
import com.novastream.app.ui.components.MediaRow
import com.novastream.app.ui.components.SectionHeader
import com.novastream.app.ui.components.SourceBadge
import com.novastream.app.ui.nav.LocalFloatingNavBottomPadding
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.MangaViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

/**
 * @param embedded when true the screen omits its own title/header row because a host (the Browse
 *   tab) is already drawing one above it.
 */
@Composable
fun MangaScreen(nav: NavHostController, embedded: Boolean = false) {
    val vm = novaViewModel { MangaViewModel(it) }
    val nova = LocalNovaColors.current
    val rows by vm.rows.collectAsStateSafe()
    val loading by vm.loading.collectAsStateSafe()
    val installed by vm.installedExt.collectAsStateSafe()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().clipToBounds(),
        contentPadding = PaddingValues(
            top = if (embedded) 0.dp else 48.dp,
            bottom = 24.dp + LocalFloatingNavBottomPadding.current,
        ),
    ) {
        if (!embedded) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(10f)
                        .background(nova.background)
                        .padding(horizontal = AppSpacing.screen, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Manga", style = MaterialTheme.typography.displaySmall, color = nova.textPrimary)
                    }
                    SourceBadge("Powered by MangaDex")
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { nav.navigate(Routes.ADDONS) }) {
                        Icon(Icons.Filled.Extension, "Extensions", tint = nova.accent)
                    }
                }
            }
        }

        if (installed.isNotEmpty()) {
            item {
                SectionHeader("Installed Manga Sources")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(installed, key = { it.pkg }) { ext ->
                        GlassSurface(modifier = Modifier.width(200.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(ext.name, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary, maxLines = 2)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${ext.lang ?: "en"} · ${ext.sources.size} sources",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = nova.textTertiary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (loading && rows.isEmpty()) items(4) { LoadingRow() }
        if (!loading && rows.isEmpty()) item { EmptyState("No manga found", "Check your connection and try again.") }

        items(rows) { row ->
            // Same card width as Home so a Manga rail card is the same size as a Home rail card.
            MediaRow(
                row.title,
                row.items,
                onClick = { nav.navigate(Routes.detail(it)) },
                cardWidth = AppSpacing.railCard,
            )
        }
    }
}
