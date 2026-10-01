package com.novastream.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.novastream.app.data.model.MediaItem
import com.novastream.app.ui.theme.LocalNovaColors

/** Unified corner radius for every poster-shaped card and its placeholder across the app. */
val PosterShape = RoundedCornerShape(12.dp)

/** A translucent, elevated "glass" surface used for cards and sheets. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val nova = LocalNovaColors.current
    val base = Modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    nova.surfaceElevated.copy(alpha = if (nova.isDark) 0.72f else 0.9f),
                    nova.surface.copy(alpha = if (nova.isDark) 0.55f else 0.8f),
                )
            )
        )
        .border(BorderStroke(1.dp, nova.outline.copy(alpha = 0.5f)), shape)
    val clickable = if (onClick != null) base.clickable { onClick() } else base
    Box(modifier = clickable.then(modifier)) { content() }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = LocalNovaColors.current.textPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 132,
    showTitle: Boolean = true,
) {
    val nova = LocalNovaColors.current
    val sizeMod = if (width > 0) Modifier.width(width.dp) else Modifier.fillMaxWidth()
    Column(
        modifier = modifier.then(sizeMod).clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(PosterShape)
                .background(nova.surfaceElevated),
        ) {
            if (!item.poster.isNullOrBlank()) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Image, null, tint = nova.textTertiary, modifier = Modifier.size(28.dp))
                }
            }
            // subtle bottom gradient
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                            startY = 260f,
                        )
                    )
            )
            item.rating?.let { r ->
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC94D), modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(String.format("%.1f", r), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (showTitle) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = nova.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            // Metadata on a single horizontal line, e.g. "Digger · 2026".
            val meta = listOfNotNull(
                item.year?.takeIf { it.isNotBlank() },
                item.rating?.let { String.format("%.1f", it) },
            ).joinToString(" \u00b7 ")
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = nova.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun MediaRow(
    title: String,
    items: List<MediaItem>,
    onClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Int = 132,
    trailing: @Composable (() -> Unit)? = null,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title, trailing = trailing)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.key }) { item ->
                PosterCard(item, { onClick(item) }, width = cardWidth)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(12.dp)) {
    val nova = LocalNovaColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier
            .clip(shape)
            .background(nova.surfaceElevated.copy(alpha = alpha))
    )
}

@Composable
fun LoadingRow() {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        ShimmerBox(Modifier.padding(horizontal = 16.dp).width(160.dp).height(20.dp))
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(4) {
                ShimmerBox(Modifier.width(132.dp).aspectRatio(2f / 3f), PosterShape)
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Outlined.Image,
) {
    val nova = LocalNovaColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(nova.surfaceElevated.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(it, null, tint = nova.textTertiary, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary)
        subtitle?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = nova.textSecondary)
        }
    }
}

/** Small outlined pill used to attribute a screen to its data source (e.g. "MangaDex"). */
@Composable
fun SourceBadge(text: String, modifier: Modifier = Modifier) {
    val nova = LocalNovaColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(nova.surfaceElevated.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, nova.outline.copy(alpha = 0.7f)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = nova.textSecondary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun RatingBadge(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LocalNovaColors.current.accent.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = LocalNovaColors.current.accent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TagChip(text: String, selected: Boolean = false, onClick: () -> Unit = {}) {
    val nova = LocalNovaColors.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) nova.accent else nova.surfaceElevated)
            // An explicit outline marks unselected chips as clearly clickable.
            .border(BorderStroke(1.dp, if (selected) Color.Transparent else nova.outline), shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else nova.textSecondary,
        )
    }
}
