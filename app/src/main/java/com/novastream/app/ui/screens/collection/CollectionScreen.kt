package com.novastream.app.ui.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.data.model.MediaType
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.LoadingRow
import com.novastream.app.ui.components.PosterCard
import com.novastream.app.ui.components.PosterQuickActionsSheet
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.SectionViewModel
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel

/** "See all" browse screen — a full grid of everything in a given section. */
@Composable
fun CollectionScreen(nav: NavHostController, section: MediaType) {
    val vm = novaViewModel(key = "collection-${section.id}") { SectionViewModel(it, section) }
    val nova = LocalNovaColors.current
    val rows by vm.rows.collectAsStateSafe()
    val loading by vm.loading.collectAsStateSafe()

    val items = rows.flatMap { it.items }.distinctBy { it.key }
    val listState = rememberLazyGridState()
    var actionsFor by remember { mutableStateOf<com.novastream.app.data.model.MediaItem?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .zIndex(10f)
                .background(nova.background)
                // Dynamic status-bar inset instead of a hardcoded 44dp, so tall status bars and
                // notches don't clip the title.
                .statusBarsPadding()
                .padding(start = 4.dp, end = AppSpacing.screen, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = nova.textPrimary)
            }
            Text(
                section.label,
                style = MaterialTheme.typography.headlineSmall,
                color = nova.textPrimary,
            )
        }

        when {
            loading && items.isEmpty() -> Column { repeat(3) { LoadingRow() } }
            items.isEmpty() -> EmptyState("Nothing here yet", "Install add-ons to populate this section.")
            else -> LazyVerticalGrid(
                // Adaptive rather than Fixed(3): a 7" phone and a tablet now get the same sensible
                // card size instead of 3 cramped columns on one and 3 huge ones on the other.
                columns = GridCells.Adaptive(minSize = AppSpacing.posterMin),
                state = listState,
                // Hard-clip the viewport so a poster can't paint over the header above it.
                modifier = Modifier.fillMaxSize().clipToBounds(),
                contentPadding = PaddingValues(
                    start = AppSpacing.screen,
                    end = AppSpacing.screen,
                    top = 8.dp,
                    bottom = 32.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.item),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(items, key = { it.key }) { item ->
                    PosterCard(item, { nav.navigate(Routes.detail(item)) }, modifier = Modifier.fillMaxWidth(), width = 0, onLongClick = { actionsFor = item })
                }
            }
        }
    }

    actionsFor?.let { target ->
        PosterQuickActionsSheet(
            item = target,
            onDismiss = { actionsFor = null },
            onOpen = { nav.navigate(Routes.detail(target)); actionsFor = null },
        )
    }
}
