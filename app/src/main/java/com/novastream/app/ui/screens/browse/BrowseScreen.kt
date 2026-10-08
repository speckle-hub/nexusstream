package com.novastream.app.ui.screens.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.novastream.app.ui.components.PillSegmentedControl
import com.novastream.app.ui.screens.anime.AnimeScreen
import com.novastream.app.ui.screens.manga.MangaScreen
import com.novastream.app.ui.nav.Routes
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.Motion

/**
 * One Browse destination covering both Manga and Anime.
 *
 * Manga and Anime were permanent bottom-bar tabs at one point, which made the bar six items wide —
 * crowded enough that labels truncated. They're now one destination behind a segmented control,
 * leaving the bar at five comfortable items.
 *
 * The NSFW segment that used to live here has been removed: adult content now lives in the separate,
 * route-only `NSFW Hub` (`ui/screens/nsfw/NsfwHubScreen.kt`), reachable from Settings → Content
 * Restrictions, so it can never sit alongside the public Manga/Anime surfaces.
 *
 * Each section is embedded (no duplicate title row) and keeps its own ViewModel, so switching
 * segments preserves that section's rows and search query.
 */
@Composable
fun BrowseScreen(nav: NavHostController) {
    val nova = LocalNovaColors.current
    // rememberSaveable: survives the Browse destination being disposed (e.g. opening a detail page
    // and coming back) so the user returns to the section they were on.
    var section by rememberSaveable { mutableIntStateOf(0) }
    val labels = listOf("Manga", "Anime")

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
                else -> AnimeScreen(nav, embedded = true)
            }
        }
    }
}
