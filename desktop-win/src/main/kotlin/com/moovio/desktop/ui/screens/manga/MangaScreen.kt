package com.moovio.desktop.ui.screens.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.moovio.desktop.ui.components.PrimaryButton
import com.moovio.desktop.ui.components.SectionHeader
import com.moovio.desktop.ui.components.ShimmerBox
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.theme.AccentColor
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.glass

@Composable
fun MangaScreen(app: AppState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 34.dp),
    ) {
        Text("Manga", style = MaterialTheme.typography.headlineLarge, color = Palette.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            "A desktop-native reader is part of the v1.1 release.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.textTertiary,
        )
        Spacer(Modifier.height(28.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            AccentColor.copy(alpha = 0.20f),
                            Palette.surface,
                            Palette.surfaceHigh.copy(alpha = 0.7f),
                        ),
                    ),
                    RoundedCornerShape(22.dp),
                )
                .glass(RoundedCornerShape(22.dp), alpha = 0.04f),
        ) {
            Row(
                Modifier.fillMaxSize().padding(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoStories, null, Modifier.size(18.dp), tint = AccentColor)
                        Text(
                            "COMING IN V1.1",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentColor,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Read wide, not pinched.",
                        style = MaterialTheme.typography.displayMedium,
                        color = Palette.textPrimary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "A page-turning reader designed for 1440p and ultrawide displays: " +
                            "webtoon strips, double-page spreads, RTL/LTR, zoom lock and offline chapters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.textSecondary,
                        modifier = Modifier.fillMaxWidth(0.8f),
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton("Open Movies instead", onClick = { app.navigateTab(com.moovio.desktop.ui.nav.Screen.Movies) })
                        Box(
                            Modifier
                                .height(44.dp)
                                .glass(RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Reader runtime is intentionally out of scope for v1",
                                style = MaterialTheme.typography.labelSmall,
                                color = Palette.textTertiary,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(3) { index ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShimmerBox(Modifier.width(132.dp).height(198.dp), RoundedCornerShape(12.dp))
                            Box(
                                Modifier
                                    .width(132.dp)
                                    .height(8.dp)
                                    .background(Palette.surfaceHighest, RoundedCornerShape(4.dp)),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(38.dp))
        SectionHeader("What lands with the reader", "Planned for the next milestone")
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            ReaderFeature(
                Icons.Rounded.BookmarkBorder,
                "Progress that follows you",
                "Per-chapter positions sync with your library.",
                Modifier.weight(1f),
            )
            ReaderFeature(
                Icons.Rounded.Download,
                "Offline chapters",
                "Queue downloads and read on a plane.",
                Modifier.weight(1f),
            )
            ReaderFeature(
                Icons.Rounded.Lock,
                "Private shelf",
                "PIN-gated titles stay out of search and the home feed.",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReaderFeature(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .glass(RoundedCornerShape(16.dp), alpha = 0.05f)
            .padding(22.dp),
    ) {
        Column {
            Box(
                Modifier
                    .size(38.dp)
                    .background(AccentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(19.dp), tint = AccentColor)
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = Palette.textTertiary)
        }
    }
}
