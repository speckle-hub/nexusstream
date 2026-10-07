package com.novastream.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.novastream.app.data.model.MediaItem
import com.novastream.app.ui.theme.AccentViolet
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.Motion
import kotlinx.coroutines.delay

/** Unified corner radius for every poster-shaped card and its placeholder across the app. */
val PosterShape = RoundedCornerShape(12.dp)

/**
 * Fade + rise entrance for a single row item, delayed by its index.
 *
 * Rows used to pop in all at once, which reads as a single flat "flash of content" rather than a
 * feed filling up. Staggering by a few frames makes the page feel composed. The delay is capped so
 * the last visible card never waits more than a few hundred milliseconds — an unbounded stagger
 * makes long rows feel sluggish.
 */
@Composable
private fun StaggeredEntrance(index: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    // Keyed on Unit, not `index`: a LazyRow recycles slots, so re-keying on the index would replay
    // the fade from 0 whenever a different item lands in the same slot — a visible flash mid-scroll.
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(MAX_STAGGER_INDEX) * STAGGER_STEP_MS)
        progress.animateTo(1f, tween(durationMillis = Motion.SLOW, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = progress.value
            // Rises the last 12dp into place; a tiny overshoot-free settle reads cleaner than a bounce.
            translationY = (1f - progress.value) * STAGGER_RISE_DP.dp.toPx()
        }
    ) {
        content()
    }
}

/** Delay between successive row items, in ms. */
private const val STAGGER_STEP_MS = 40L

/** Items past this index all share the final delay, so long rows never feel sluggish. */
private const val MAX_STAGGER_INDEX = 8

/** How far (dp) an item travels as it fades in. */
private const val STAGGER_RISE_DP = 12f

/**
 * Slight shrink while a surface is held down.
 *
 * Cards used to have no press state at all, so the whole app felt inert to tap. The transform is
 * applied in the draw phase (no re-layout), and it eases back out over [Motion.PRESS] ms.
 */
@Composable
private fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.PRESS_SCALE else 1f,
        animationSpec = tween(Motion.PRESS),
        label = "pressScale",
    )
    return this.scale(scale)
}

/** Shared press feedback: an [interactionSource]-driven scale plus a normal ripple [clickable]. */
@Composable
private fun Modifier.pressableClickable(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = pressScale(interactionSource).clickable(
    interactionSource = interactionSource,
    // The interactionSource-taking overload of `clickable` has no default for `indication`, so it
    // has to be passed explicitly; LocalIndication keeps the platform ripple.
    indication = LocalIndication.current,
    onClick = onClick,
)

/** A translucent, elevated "glass" surface used for cards and sheets. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val nova = LocalNovaColors.current
    val interaction = remember { MutableInteractionSource() }
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
    // Tappable glass also shrinks slightly while held, so rows feel like physical controls.
    val clickable = if (onClick != null) {
        base.pressableClickable(interaction) { onClick() }
    } else {
        base
    }
    Box(modifier = clickable.then(modifier)) { content() }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = AppSpacing.screen, vertical = 8.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 132,
    showTitle: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val nova = LocalNovaColors.current
    val sizeMod = if (width > 0) Modifier.width(width.dp) else Modifier.fillMaxWidth()
    // Press feedback: the whole card shrinks slightly while held, so posters feel tappable rather
    // than inert. The scale happens in the draw phase, so no layout shifts on press.
    val interaction = remember { MutableInteractionSource() }
    val clickMod = if (onLongClick != null) {
        Modifier
            .pressScale(interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
    } else {
        Modifier.pressableClickable(interaction) { onClick() }
    }
    Column(
        modifier = modifier.then(sizeMod).then(clickMod),
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
            // "×" affordance for removing the card from a curated list (favourites, continue
            // watching). TopStart so it never collides with the rating badge.
            //
            // The *visible* chip stays small (24dp) to avoid covering the artwork, but the touch
            // target is the 48dp minimum via `minimumInteractiveComponentSize` — the previous 24dp
            // target was well below the accessibility floor for a destructive action.
            onRemove?.let { remove ->
                // The visible chip stays 24dp so it doesn't cover the artwork, but the *touch* target
                // is a full 48dp (the accessibility minimum) centred on it — the previous 24dp
                // target was well below the floor for a destructive action.
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { remove() },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.62f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
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
                Text(
                    String.format("%.1f", r),
                    color = Color.White,
                    // Tabular figures so the badge doesn't re-flow as the number changes width.
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                )
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
    val rowState = rememberLazyListState()
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title, trailing = trailing)
        LazyRow(
            state = rowState,
            modifier = Modifier.fillMaxWidth().clipToBounds(),
            contentPadding = PaddingValues(horizontal = AppSpacing.screen),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.item),
        ) {
            itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                StaggeredEntrance(index = index) {
                    PosterCard(item, { onClick(item) }, width = cardWidth)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Loading placeholder with a light sweep travelling across it.
 *
 * This used to pulse a flat block's alpha, which read as "nothing is happening" rather than
 * "something is loading". A translating gradient is what the eye recognises as a skeleton.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(12.dp)) {
    val nova = LocalNovaColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    // -1f..2f so the band fully enters and exits the box.
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "shimmerProgress",
    )
    val base = if (nova.isDark) nova.surfaceElevated else nova.surfaceElevated.copy(alpha = 0.9f)
    val highlight = if (nova.isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.85f)
    Box(
        modifier
            .clip(shape)
            .background(base)
            // Sweep the highlight across the box as the animation advances.
            .drawWithContent {
                drawContent()
                val band = size.width * 0.6f
                val start = progress * size.width - band
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        start = Offset(start, 0f),
                        end = Offset(start + band, size.height),
                    ),
                )
            }
    )
}

/** A skeleton shaped exactly like a [PosterCard] (artwork + title + meta), for grid loading. */
@Composable
fun PosterCardSkeleton(modifier: Modifier = Modifier, width: Int = 132) {
    val sizeMod = if (width > 0) Modifier.width(width.dp) else Modifier.fillMaxWidth()
    Column(modifier.then(sizeMod)) {
        ShimmerBox(Modifier.fillMaxWidth().aspectRatio(2f / 3f), PosterShape)
        Spacer(Modifier.height(6.dp))
        ShimmerBox(Modifier.fillMaxWidth(0.9f).height(12.dp), RoundedCornerShape(4.dp))
        Spacer(Modifier.height(5.dp))
        ShimmerBox(Modifier.fillMaxWidth(0.45f).height(10.dp), RoundedCornerShape(4.dp))
    }
}

@Composable
fun LoadingRow() {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        ShimmerBox(Modifier.padding(horizontal = AppSpacing.screen).width(160.dp).height(20.dp))
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.padding(horizontal = AppSpacing.screen),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.item),
        ) {
            // Matches MediaRow's default cardWidth so the feed doesn't jump when real rows land.
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

/** How a stream's resolution ranks, used to colour the badge and sort the list. */
enum class ResolutionTier { HDR, UHD, FHD, HD, SD, UNKNOWN }

/**
 * Classifies a free-form quality string ("1080p", "4k", "2160p HDR", "720", "480p", "HD").
 *
 * Add-ons are inconsistent — some say "4k", some "2160p", some "UHD" — and some pass through
 * junk. Anything unrecognised becomes [ResolutionTier.UNKNOWN] rather than guessing, so the badge
 * never claims a resolution the stream doesn't actually have.
 */
fun resolutionTierOf(quality: String?): ResolutionTier {
    val q = quality?.trim()?.lowercase().orEmpty()
    if (q.isEmpty()) return ResolutionTier.UNKNOWN
    return when {
        // HDR first: "2160p HDR" must not be classified as plain UHD.
        q.contains("hdr") || q.contains("dolby") || q.contains("10bit") || q.contains("10-bit") ->
            ResolutionTier.HDR
        // Match on the number so "2160p", "4k" and "uhd" all land as UHD.
        q.contains("2160") || q.contains("4k") || q.contains("uhd") || q.contains("8k") ->
            ResolutionTier.UHD
        q.contains("1080") || q.contains("fhd") || q.contains("fullhd") || q.contains("full-hd") ->
            ResolutionTier.FHD
        q.contains("720") || q.contains("hd") -> ResolutionTier.HD
        q.contains("480") || q.contains("360") || q.contains("240") || q.contains("sd") ->
            ResolutionTier.SD
        else -> ResolutionTier.UNKNOWN
    }
}

/**
 * A resolution badge for a stream.
 *
 * The streams list used to render quality through the generic [RatingBadge], which is accent-tinted
 * and carries no meaning — every quality looked identical. This one encodes the tier in its colour
 * (HDR gold → UHD violet → FHD accent → HD neutral → SD muted) so the best stream in a list is
 * identifiable at a glance, and stays plain text when the quality is unrecognised.
 */
@Composable
fun ResolutionBadge(quality: String?, modifier: Modifier = Modifier) {
    val tier = resolutionTierOf(quality)
    if (tier == ResolutionTier.UNKNOWN) {
        // Don't invent a resolution; fall back to whatever the add-on actually said.
        if (!quality.isNullOrBlank()) {
            RatingBadge(quality)
        }
        return
    }
    val nova = LocalNovaColors.current
    val tint = when (tier) {
        ResolutionTier.HDR -> Color(0xFFFFC94D)
        ResolutionTier.UHD -> AccentViolet
        ResolutionTier.FHD -> nova.accent
        ResolutionTier.HD -> Color(0xFF9AA0B5)
        ResolutionTier.SD -> nova.textTertiary
        ResolutionTier.UNKNOWN -> nova.textTertiary
    }
    val label = when (tier) {
        ResolutionTier.HDR -> "HDR"
        ResolutionTier.UHD -> "4K"
        ResolutionTier.FHD -> "1080p"
        ResolutionTier.HD -> "720p"
        ResolutionTier.SD -> "SD"
        ResolutionTier.UNKNOWN -> quality.orEmpty()
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        color = tint,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = if (tier == ResolutionTier.SD) 0.10f else 0.16f))
            .border(BorderStroke(1.dp, tint.copy(alpha = 0.35f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
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

/**
 * Confirmation dialog for removing an item from a curated list (favourites / continue watching).
 *
 * Both remove paths — the card's "×" and long-press — go through this so the wording is
 * identical and removal is always a deliberate second tap rather than an accidental one.
 */
/**
 * The app's search input.
 *
 * This replaces `OutlinedTextField`, which was the single most "default Android sample" element
 * on screen: a hard 1dp outline, no fill, no clear affordance. This version is a filled, elevated
 * pill with a search glyph, an inline progress dot while a query is in flight and a circular clear
 * button that appears only when there is something to clear.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    searching: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    val nova = LocalNovaColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(nova.surfaceElevated)
            .border(BorderStroke(1.dp, nova.outline.copy(alpha = 0.7f)), RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = if (value.isBlank()) nova.textTertiary else nova.accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.weight(1f).height(52.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = nova.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = nova.textPrimary),
                cursorBrush = SolidColor(nova.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // A small pulsing dot while the debounced query is in flight — a replacement for the
        // full-screen "Searching…" text state.
        if (searching && value.isNotBlank()) {
            val transition = rememberInfiniteTransition(label = "searchDot")
            val a by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600), androidx.compose.animation.core.RepeatMode.Reverse),
                label = "searchDotAlpha",
            )
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(nova.accent.copy(alpha = a))
            )
            Spacer(Modifier.width(8.dp))
        }
        if (value.isNotEmpty()) {
            // Full 48dp accessibility target (the glyph stays 18dp inside it).
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onClear?.invoke() ?: onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "Clear search", tint = nova.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Confirmation dialog for removing an item from a curated list (favourites / continue watching).
 *
 * Both remove paths — the card's "×" and long-press — go through this so the wording is
 * identical and removal is always a deliberate second tap rather than an accidental one.
 */
@Composable
fun ConfirmRemoveDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Remove") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
