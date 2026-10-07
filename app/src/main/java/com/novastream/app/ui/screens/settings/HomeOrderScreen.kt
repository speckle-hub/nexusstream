package com.novastream.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.data.model.CatalogRow
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.HomeViewModel
import com.novastream.app.ui.vm.LocalContainer
import com.novastream.app.ui.vm.collectAsStateSafe
import com.novastream.app.ui.vm.novaViewModel
import androidx.compose.runtime.LaunchedEffect

/**
 * Home feed row reordering (Phase 12).
 *
 * Rows are produced dynamically by add-ons, so instead of drag-and-drop on a fixed list this shows
 * the current rows with up/down priority controls and persists the resulting title order. Unknown
 * rows keep their natural position after any explicitly ordered ones.
 */
@Composable
fun HomeOrderScreen(nav: NavHostController) {
    val nova = LocalNovaColors.current
    val container = LocalContainer.current
    val vm = novaViewModel { HomeViewModel(it) }
    val rows by vm.rows.collectAsStateSafe()
    val saved by container.settings.homeOrder.collectAsStateSafe()
    val scope = rememberCoroutineScope()

    // Local working copy so moves are instant; persisted on every change. rememberSaveable so the
    // in-progress order survives rotation (it's a list of Strings, which is directly saveable).
    var order by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(rows, saved) {
        // Never let a bad row snapshot take the screen down: an unreadable order simply leaves the
        // list empty (which renders the empty state) instead of crashing on entry.
        runCatching {
            val titles = rows.map { it.title }
            if (titles.isNotEmpty()) {
                // Seed with the saved order, then append any titles not yet known. Guarded on `order`
                // being empty so a rotation-restored working copy isn't clobbered by the reseed.
                if (order.isEmpty()) {
                    order = saved.filter { it in titles } + titles.filterNot { it in saved }
                }
            }
        }
    }

    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target !in order.indices) return
        val mutable = order.toMutableList()
        val tmp = mutable[index]
        mutable[index] = mutable[target]
        mutable[target] = tmp
        order = mutable
        scope.launch { runCatching { container.settings.setHomeOrder(mutable) } }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = AppSpacing.screen, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = nova.textPrimary)
            }
            Text("Home rows", style = MaterialTheme.typography.headlineSmall, color = nova.textPrimary)
        }

        if (order.isEmpty()) {
            EmptyState("No rows to reorder", "Home sections appear once your add-ons load.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = AppSpacing.screen, end = AppSpacing.screen, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(order, key = { _, t -> t }) { index, title ->
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${index + 1}. $title",
                                style = MaterialTheme.typography.titleMedium,
                                color = nova.textPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { move(index, -1) }, enabled = index > 0) {
                                Icon(Icons.Filled.KeyboardArrowUp, "Move up", tint = nova.textSecondary)
                            }
                            IconButton(onClick = { move(index, 1) }, enabled = index < order.size - 1) {
                                Icon(Icons.Filled.KeyboardArrowDown, "Move down", tint = nova.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Stable sort of home rows by a saved title order; unknown titles keep their input order. */
fun applyRowOrder(rows: List<CatalogRow>, order: List<String>): List<CatalogRow> {
    if (order.isEmpty()) return rows
    val indexOf = order.withIndex().associate { it.value to it.index }
    return rows.sortedBy { indexOf[it.title] ?: Int.MAX_VALUE }
}
