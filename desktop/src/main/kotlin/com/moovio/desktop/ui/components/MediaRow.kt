package com.moovio.desktop.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.MoovioTextSecondary

@Composable
fun MediaRow(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = androidx.compose.material.MaterialTheme.typography.h4,
            color = androidx.compose.material.MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = AppSpacing.m),
        )
        if (items.isEmpty()) {
            Text(
                text = "No results",
                style = androidx.compose.material.MaterialTheme.typography.body1,
                color = MoovioTextSecondary,
                modifier = Modifier.padding(bottom = AppSpacing.l),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
            ) {
                items.forEach { item ->
                    PosterCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.padding(end = AppSpacing.m),
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.l))
            }
        }
    }
}


