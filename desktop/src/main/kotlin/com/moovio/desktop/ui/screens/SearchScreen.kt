package com.moovio.desktop.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.ui.nav.NavController
import com.moovio.desktop.ui.components.MediaRow
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.MoovioTextSecondary

@Composable
fun SearchScreen(container: AppContainer, nav: NavController, initialQuery: String = "") {
    val settings = container.settings
    var query by remember { mutableStateOf(initialQuery) }
    val results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(AppSpacing.xxl),
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.Start,
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.h3,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = AppSpacing.xl),
        )
        TextField(
            value = query,
            onValueChange = { query = it },
            onValueCommit = {
                scope.launch {
                    runSearch()
                }
            },
            placeholder = { Text("Search movies, shows, anime…") },
            modifier = Modifier.width(420.dp),
        )
        if (query.isNotBlank()) {
            Text(
                text = "${results.size} results for “$query”",
                style = MaterialTheme.typography.body1,
                color = MoovioTextSecondary,
                modifier = Modifier.padding(top = AppSpacing.m, bottom = AppSpacing.m),
            )
            MediaRow(
                title = "Results",
                items = results,
                onItemClick = { nav.openDetail(it.id, it.title) },
            )
        }
    }

    private fun runSearch() = withContext(Dispatchers.Default) {
        val found = container.catalogRepository.search(query)
        results.value = found
    }
}


