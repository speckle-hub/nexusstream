package com.moovio.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Icons
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
fun DetailScreen(
    mediaId: String,
    title: String,
    container: AppContainer,
    nav: NavController,
) {
    val isFavorite = remember { mutableStateOf(container.library.isFavorite(mediaId)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(AppSpacing.xxl),
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.back() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MoovioTextPrimary)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.h3,
                color = MoovioTextPrimary,
                modifier = Modifier.padding(start = AppSpacing.m),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().height(300.dp).padding(top = AppSpacing.xxl),
            color = MoovioSurface,
        ) {
            RemoteImage(
                url = null,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "$title backdrop",
            )
        }

        Text(text = title, style = MaterialTheme.typography.h2, color = MoovioTextPrimary,
            modifier = Modifier.padding(top = AppSpacing.xxl, bottom = AppSpacing.s))
        Text(text = "2024 · Sci-Fi · Adventure", style = MaterialTheme.typography.body1, color = MoovioTextSecondary)

        Row(modifier = Modifier.padding(vertical = AppSpacing.m)) {
            IconButton(onClick = { /* quality sheet */ }) {
                Icon(Icons.Default.Hd, contentDescription = "Quality", tint = moovioColors.accent)
            }
            Text("Best available", style = MaterialTheme.typography.subtitle1, color = MoovioTextSecondary,
                modifier = Modifier.padding(start = AppSpacing.s))
        }

        Button(
            onClick = { nav.navigate(com.moovio.desktop.domain.model.NavigationDestination.Player) },
            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Play", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}


