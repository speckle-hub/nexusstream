package com.novastream.app.ui.screens.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.novastream.app.ui.components.PillSegmentedControl
import com.novastream.app.ui.screens.manga.MangaScreen
import com.novastream.app.ui.screens.nsfw.NsfwScreen
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.Motion

/**
 * One Browse destination covering both Manga and NSFW.
 *
 * Manga and NSFW were permanent bottom-bar tabs, which made the bar six items wide — crowded
 * enough that labels truncated, and it promoted NSFW (a gated, adult section) to the same
 * prominence as Home. They're now one destination behind a segmented control, leaving the bar at
 * five comfortable items.
 *
 * Each section is embedded (no duplicate title row) and keeps its own ViewModel, so switching
 * segments preserves that section's rows, search queries and sub-tab — the same behaviour the
 * NSFW screen had when it was its own destination.
 */
@Composable
fun BrowseScreen(nav: NavHostController) {
    val nova = LocalNovaColors.current
    // rememberSaveable: survives the Browse destination being disposed (e.g. opening a detail page
    // and coming back) so the user returns to the section they were on.
    var section by rememberSaveable { mutableIntStateOf(0) }
    val labels = listOf("Manga", "NSFW")

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .zIndex(10f)
                .background(nova.background)
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.screen)
                .padding(top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Browse",
                style = MaterialTheme.typography.displaySmall,
                color = nova.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { nav.navigate(Routes.ADDONS) }) {
                Icon(Icons.Filled.Extension, "Extensions", tint = nova.accent)
            }
        }

        PillSegmentedControl(
            labels = labels,
            selected = section,
            onSelect = { section = it },
            modifier = Modifier.zIndex(10f).background(nova.background).padding(horizontal = AppSpacing.screen, vertical = 4.dp),
        )

        AnimatedContent(
            targetState = section,
            transitionSpec = {
                // A short cross-fade; the sections swap content wholesale so a slide would imply
                // spatial movement that isn't there.
                fadeIn(tween(Motion.FAST)) togetherWith fadeOut(tween(Motion.FAST))
            },
            label = "browseSection",
            // Clip the cross-fading sections to their box so a scrolling grid can never paint over
            // the "Browse" header or the segmented control above it.
            modifier = Modifier.fillMaxSize().clipToBounds(),
        ) { target ->
            when (target) {
                0 -> MangaScreen(nav, embedded = true)
                else -> NsfwScreen(nav, embedded = true)
            }
        }
    }
}

/**
 * A compact pill segmented control.
 *
 * Used instead of a full-width `TabRow` because the Browse destination already owns a large title
 * above it, and two tabs don't need to stretch across the screen.
 */
@Composable
private fun SegmentedControl(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nova = LocalNovaColors.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(nova.surfaceElevated)
            .border(BorderStroke(1.dp, nova.outline.copy(alpha = 0.6f)), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) nova.accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) nova.accent else nova.textSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}
