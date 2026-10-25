package com.moovio.desktop.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.moovio.desktop.domain.model.Episode
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.ui.theme.AccentColor
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.bottomScrim
import com.moovio.desktop.ui.theme.glass
import com.moovio.desktop.ui.theme.horizontalScrim
import com.moovio.desktop.ui.theme.reduceMotion
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIcon: ImageVector = Icons.Rounded.PlayArrow,
) {
    if (url.isNullOrBlank()) {
        ArtworkFallback(fallbackIcon, modifier)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        },
        error = {
            ArtworkFallback(fallbackIcon, Modifier.fillMaxSize())
        },
        success = {
            SubcomposeAsyncImageContent()
        },
    )
}

@Composable
private fun ArtworkFallback(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(
                Brush.linearGradient(
                    listOf(Palette.surfaceHigh, Palette.surfaceHighest, Palette.surfaceHigh),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(26.dp), tint = Palette.textTertiary.copy(alpha = 0.5f))
    }
}

@Composable
fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 168.dp,
    onPlay: (() -> Unit)? = null,
    progress: Float? = null,
    showTitle: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered && !reduceMotion) 1.045f else 1f,
        animationSpec = tween(220),
        label = "posterScale",
    )
    val focusInteraction = remember { MutableInteractionSource() }
    val focused by focusInteraction.collectIsFocusedAsState()

    Column(
        modifier = modifier
            .width(width)
            .scale(scale)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                    onClick(); true
                } else false
            }
            .focusable(interactionSource = focusInteraction),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(13.dp))
                .background(Palette.surfaceHigh)
                .hoverable(interaction)
                .clickable(onClick = onClick),
        ) {
            RemoteImage(
                url = item.poster ?: item.background,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
            )
            if (item.rating != null) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(Icons.Rounded.Star, null, Modifier.size(11.dp), tint = Color(0xFFFFC857))
                    Text(
                        "%.1f".format(item.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
            if (progress != null && progress > 0f) {
                ProgressLine(
                    progress = progress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    height = 3.dp,
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = hovered || focused,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
            ) {
                if (onPlay != null) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.92f))
                            .clickable(onClick = onPlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            "Play",
                            Modifier.size(26.dp),
                            tint = Color(0xFF0A0A12),
                        )
                    }
                }
            }
            if (focused) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(13.dp))
                        .background(androidx.compose.ui.graphics.SolidColor(Color.Transparent)),
                )
            }
        }
        if (showTitle) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (hovered) Color.White else Palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
fun MediaRow(
    title: String,
    items: List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    onPlay: ((MediaItem) -> Unit)? = null,
    progressFor: (MediaItem) -> Float? = { null },
    cardWidth: Dp = 168.dp,
    trailing: (@Composable () -> Unit)? = null,
    indexOffset: Int = 0,
) {
    if (items.isEmpty()) return
    val motionReduced = reduceMotion
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.padding(horizontal = 48.dp)) {
            SectionHeader(
                title = title,
                trailing = { trailing?.invoke() },
            )
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.key + indexOffset }) { index, item ->
                val appear = remember { mutableStateOf(false) }
                LaunchedEffect(item.key) {
                    if (motionReduced) {
                        appear.value = true
                    } else {
                        delay((index.coerceAtMost(12) * 35L))
                        appear.value = true
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = appear.value,
                    enter = fadeIn(tween(320)) + androidx.compose.animation.slideInVertically(
                        animationSpec = tween(320),
                        initialOffsetY = { it / 6 },
                    ),
                    exit = fadeOut(),
                ) {
                    PosterCard(
                        item = item,
                        width = cardWidth,
                        onClick = { onOpen(item) },
                        onPlay = onPlay?.let { play -> { play(item) } },
                        progress = progressFor(item),
                    )
                }
            }
        }
    }
}

@Composable
fun HeroCarousel(
    items: List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    onPlay: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    isInLibrary: (MediaItem) -> Boolean = { false },
    onToggleLibrary: (MediaItem) -> Unit = {},
) {
    if (items.isEmpty()) {
        Box(
            modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(Palette.surface),
        ) {
            ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        }
        return
    }
    var index by remember(items) { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    val safeIndex = index.coerceIn(0, items.lastIndex)
    val item = items[safeIndex]
    val motionReduced = reduceMotion

    LaunchedEffect(items, paused) {
        if (paused || items.size <= 1 || motionReduced) return@LaunchedEffect
        while (true) {
            delay(7600)
            index = (index + 1) % items.size
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(452.dp)
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(Palette.backgroundElevated)
            .hoverableOnEnterExit { hovered -> paused = hovered },
    ) {
        Crossfade(targetState = item, animationSpec = tween(900), label = "heroArt") { current ->
            RemoteImage(
                url = current.background ?: current.poster,
                contentDescription = current.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Palette.background,
                            Palette.background.copy(alpha = 0.86f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Palette.background.copy(alpha = 0.35f),
                            Palette.background,
                        ),
                    ),
                ),
        )

        AnimatedContent(
            targetState = item,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
            label = "heroCopy",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 56.dp, end = 56.dp, bottom = 46.dp)
                .fillMaxWidth(0.62f),
        ) { current ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        current.type.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("·", color = Palette.textTertiary)
                    if (current.rating != null) {
                        Icon(Icons.Rounded.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFC857))
                        Text(
                            "%.1f".format(current.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.textSecondary,
                        )
                    }
                    current.year?.let {
                        Text("·", color = Palette.textTertiary)
                        Text(it.toString(), style = MaterialTheme.typography.labelSmall, color = Palette.textSecondary)
                    }
                    if (current.genres.isNotEmpty()) {
                        Text("·", color = Palette.textTertiary)
                        Text(
                            current.genres.take(3).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    current.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    current.description ?: "No synopsis available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.92f),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Play", Icons.Rounded.PlayArrow, onClick = { onPlay(current) })
                    GhostButton("Details", onClick = { onOpen(current) })
                    CircleIconButton(
                        icon = if (isInLibrary(current)) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = "Library",
                        onClick = { onToggleLibrary(current) },
                        size = 44.dp,
                    )
                }
            }
        }

        // Slide indicators
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 52.dp, bottom = 54.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { dotIndex, _ ->
                val selected = dotIndex == safeIndex
                val width by animateFloatAsState(
                    if (selected) 26f else 7f,
                    animationSpec = tween(260),
                    label = "dot",
                )
                Box(
                    Modifier
                        .height(7.dp)
                        .width(width.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.32f))
                        .clickable { index = dotIndex },
                )
            }
        }
    }
}

@Composable
private fun Modifier.hoverableOnEnterExit(onChange: (Boolean) -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    LaunchedEffect(hovered) { onChange(hovered) }
    return this.hoverable(source)
}

@Composable
fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    isNextUp: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val background = if (hovered) Palette.surfaceHigh else Palette.surface.copy(alpha = 0.55f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .hoverable(source)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(148.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(9.dp))
                .background(Palette.surfaceHighest),
        ) {
            RemoteImage(
                url = episode.thumbnail,
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (hovered) 0.12f else 0.28f)),
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp), tint = Color.White)
            }
            if (progress > 0f) {
                ProgressLine(
                    progress,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    height = 3.dp,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "S${episode.season} · E${episode.episode}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentColor,
                )
                if (isNextUp) {
                    Text(
                        "UP NEXT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(AccentColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                episode.title,
                style = MaterialTheme.typography.titleMedium,
                color = Palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            episode.description?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.textTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ContinueCard(
    name: String,
    subtitle: String,
    imageUrl: String?,
    progress: Float,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (hovered && !reduceMotion) 1.02f else 1f, tween(200), label = "cw")
    Column(modifier.width(270.dp).scale(scale)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Palette.surfaceHigh)
                .hoverable(source)
                .clickable(onClick = onClick),
        ) {
            RemoteImage(imageUrl, name, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(bottomScrim()))
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .glass(CircleShape, alpha = 0.02f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(24.dp), tint = Color.White)
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            ) {
                ProgressLine(progress, Modifier.fillMaxWidth(), height = 3.dp)
            }
            if (hovered) {
                CircleIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Remove",
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
                    size = 27.dp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = Palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Palette.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Horizontal scrim used on landscape cards. */
@Composable
fun LandscapeScrim(modifier: Modifier = Modifier) {
    Box(modifier.background(horizontalScrim()))
}

fun initials(name: String): String = name.split(' ')
    .filter { it.isNotBlank() }
    .take(2)
    .map { it.first().uppercaseChar() }
    .joinToString("")

fun ratioLabel(position: Long, duration: Long): String {
    if (duration <= 0) return ""
    val remaining = ((duration - position) / 60_000).coerceAtLeast(0)
    return "${remaining}m left"
}

fun almostEquals(a: Float, b: Float): Boolean = abs(a - b) < 0.001f

@Composable
fun IconBadge(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .glass(RoundedCornerShape(8.dp), alpha = 0.07f)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, Modifier.size(12.dp), tint = Palette.textSecondary)
        Text(text, style = MaterialTheme.typography.labelSmall, color = Palette.textSecondary)
    }
}
