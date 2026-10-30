package com.moovio.app.ui.components

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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.moovio.app.data.model.MediaItem
import com.moovio.app.ui.theme.AppRadius
import com.moovio.app.ui.theme.AppSpacing
import com.moovio.app.ui.theme.LocalMoovioColors
import com.moovio.app.ui.theme.Motion
import com.moovio.app.ui.theme.rememberMoovioHaptics
import com.moovio.app.ui.theme.tnum
import com.moovio.app.ui.theme.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val reducedMotion = rememberReducedMotion()
    // Keyed on Unit, not `index`: a LazyRow recycles slots, so re-keying on the index would replay
    // the fade from 0 whenever a different item lands in the same slot — a visible flash mid-scroll.
    LaunchedEffect(Unit) {
        // Honour the system "remove animations" preference — no fade, no delay, just present.
        if (reducedMotion) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
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

/**
 * The app's raised surface: a solid surface step with a hairline edge.
 *
 * This replaced a glass treatment — translucent gradient fill, 10dp drop shadow and a white rim
 * highlight along the top edge. On a near-black theme those three cues together read as frosted
 * plastic, and the rim light competed with the artwork sitting behind the panel. Depth on dark now
 * comes from the surface step plus the hairline, which is all this UI needs; light mode keeps a
 * soft neutral shadow because a white surface on white needs one to separate at all.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(AppRadius.panel.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val moovio = LocalMoovioColors.current
    val interaction = remember { MutableInteractionSource() }
    val base = Modifier
        .then(
            if (moovio.isDark) {
                Modifier
            } else {
                Modifier.shadow(
                    elevation = 2.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.06f),
                )
            }
        )
        .clip(shape)
        .background(moovio.surfaceElevated)
        .border(BorderStroke(1.dp, moovio.hairline), shape)
    // Tappable surfaces shrink slightly while held, so rows feel like physical controls.
    val clickable = if (onClick != null) {
        base.pressableClickable(interaction) { onClick() }
    } else {
        base
    }
    Box(modifier = clickable.then(modifier)) { content() }
}

/**
 * The app's switch.
 *
 * Material3's defaults paint the **unchecked** track with `surfaceVariant` and the thumb with
 * `outline` — on this theme those sit within a hair of the surrounding card, so a switch that is
 * *off* looked invisible (and worse under True AMOLED black). These explicit colours give the off
 * state a visible pill + thumb and the on state a solid accent track, in every theme.
 */
@Composable
fun MoovioSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = moovioSwitchColors(),
    )
}

/** Explicit [Switch] colours with a legible unchecked state (see [MoovioSwitch]). */
@Composable
fun moovioSwitchColors(): SwitchColors {
    val moovio = LocalMoovioColors.current
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = moovio.accent,
        checkedBorderColor = Color.Transparent,
        checkedIconColor = Color.White,
        // The off state must stay visible against the surface it sits on.
        uncheckedThumbColor = moovio.textSecondary,
        uncheckedTrackColor = moovio.outline.copy(alpha = 0.45f),
        uncheckedBorderColor = moovio.outline,
        uncheckedIconColor = moovio.textSecondary,
        disabledCheckedThumbColor = Color.White.copy(alpha = 0.6f),
        disabledCheckedTrackColor = moovio.accent.copy(alpha = 0.5f),
        disabledUncheckedThumbColor = moovio.textTertiary,
        disabledUncheckedTrackColor = moovio.outline.copy(alpha = 0.25f),
        disabledUncheckedBorderColor = moovio.outline.copy(alpha = 0.4f),
    )
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
            color = LocalMoovioColors.current.textPrimary,
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
    /** Row this card belongs to; scopes its shared-element key (see [sharedPoster]). */
    rail: String? = null,
) {
    val moovio = LocalMoovioColors.current
    // Long press here opens a remove confirmation on Library cards, so the press that commits to it
    // gets the heavier cue rather than the light tick a tap gets.
    val haptics = rememberMoovioHaptics()
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
                onLongClick = {
                    haptics.commit()
                    onLongClick()
                },
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
                .background(moovio.surfaceElevated)
                // Hairline: without it a dark cover dissolves into the page background and two
                // adjacent posters read as one shape rather than two works.
                .border(BorderStroke(1.dp, moovio.hairline), PosterShape)
                // Shared element: this artwork is the source half of the poster → detail-header
                // morph. A plain modifier outside a NavHost (previews, tests).
                .sharedPoster(item.key, rail),
        ) {
            // SubcomposeAsyncImage so a loading poster shows the shimmer instead of a blank dark
            // rectangle (the old behaviour while scrolling a grid), and a failed load degrades to
            // the placeholder glyph instead of an empty box. Crossfade comes from the app-wide
            // ImageLoader (MoovioApp sets `.crossfade(true)`).
            if (!item.poster.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = item.poster,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ShimmerBox(Modifier.fillMaxSize()) },
                    error = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Image, null, tint = moovio.textTertiary, modifier = Modifier.size(28.dp))
                        }
                    },
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Image, null, tint = moovio.textTertiary, modifier = Modifier.size(28.dp))
                }
            }
            // "×" affordance for removing the card from a curated list (favourites, continue
            // watching). TopStart so it never covers the artwork's focal point.
            //
            // The *visible* chip stays small (24dp) to avoid covering the artwork, but the touch
            // target is the 48dp minimum via `minimumInteractiveComponentSize` — the previous 24dp
            // target was well below the accessibility floor for a destructive action.
            onRemove?.let { remove ->
                // Compact corner hit area: the old 48dp box at 6dp inset covered most of the poster's
                // top-left corner and swallowed taps meant for opening the title. The touch target is
                // now a 40dp box tucked into the corner (2dp inset) with the glyph top-start aligned,
                // so the tappable region hugs the corner instead of spreading over the artwork. The
                // visible chip stays small (22dp) to keep the art uncovered.
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { remove() },
                    contentAlignment = Alignment.TopStart,
                ) {
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.62f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        if (showTitle) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = moovio.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = AppSpacing.tight),
            )
            // Year and rating on one quiet line. The rating used to also sit in an amber-star pill
            // over the artwork, so every poster said the same number twice while the pill covered
            // the poster art that the user actually chooses by.
            val meta = listOfNotNull(
                item.year?.takeIf { it.isNotBlank() },
                item.rating?.let { String.format("%.1f", it) },
            ).joinToString(" \u00b7 ")
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelMedium.tnum,
                    color = moovio.textTertiary,
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
    /** Screen-qualified id for this row, so its posters' shared keys never collide across rails. */
    rail: String? = null,
) {
    if (items.isEmpty()) return
    val rowState = rememberLazyListState()
    var actionsFor by remember { mutableStateOf<MediaItem?>(null) }
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title, trailing = trailing)
        LazyRow(
            state = rowState,
            modifier = Modifier.fillMaxWidth().clipToBounds(),
            contentPadding = PaddingValues(horizontal = AppSpacing.screen),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.item),
        ) {
            val uniqueItems = items.distinctBy { it.key }
            itemsIndexed(uniqueItems, key = { _, item -> item.key }) { index, item ->
                StaggeredEntrance(index = index) {
                    PosterCard(
                        item,
                        { onClick(item) },
                        width = cardWidth,
                        onLongClick = { actionsFor = item },
                        rail = rail,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    actionsFor?.let { target ->
        PosterQuickActionsSheet(
            item = target,
            onDismiss = { actionsFor = null },
            onOpen = { onClick(target); actionsFor = null },
        )
    }
}

/**
 * Long-press actions for a poster: open, favourite/unfavourite, and share. Kept deliberately
 * small — it's a shortcut, not a second detail page.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PosterQuickActionsSheet(
    item: MediaItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    val container = com.moovio.app.ui.vm.LocalContainer.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val moovio = LocalMoovioColors.current
    val favorites by container.libraryStore.favorites.collectAsStateWithLifecycle(emptyList())
    val isFavorite = favorites.any { it.key == item.key }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                color = moovio.textPrimary,
                modifier = Modifier.padding(horizontal = AppSpacing.screen, vertical = 4.dp),
            )
            ActionRow("Open") { onOpen() }
            ActionRow(if (isFavorite) "Remove from Favourites" else "Add to Favourites") {
                scope.launch { container.libraryStore.toggleFavorite(item) }
                onDismiss()
            }
            ActionRow("Share") {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, item.title)
                }
                context.startActivity(android.content.Intent.createChooser(send, "Share"))
                onDismiss()
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    val moovio = LocalMoovioColors.current
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = moovio.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = AppSpacing.screen, vertical = 14.dp),
    )
}

/**
 * Loading placeholder with a light sweep travelling across it.
 *
 * This used to pulse a flat block's alpha, which read as "nothing is happening" rather than
 * "something is loading". A translating gradient is what the eye recognises as a skeleton.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(12.dp)) {
    val moovio = LocalMoovioColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    // -1f..2f so the band fully enters and exits the box.
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "shimmerProgress",
    )
    val base = if (moovio.isDark) moovio.surfaceElevated else moovio.surfaceElevated.copy(alpha = 0.9f)
    val highlight = if (moovio.isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.85f)
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
                // Match the rail card width so the feed doesn't jump when real rows land.
                ShimmerBox(Modifier.width(AppSpacing.railCard.dp).aspectRatio(2f / 3f), PosterShape)
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
    /** Optional call to action, e.g. "Retry". Only shown when [onAction] is also provided. */
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val moovio = LocalMoovioColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(AppSpacing.page),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            // One quiet tile. This used to paint a 140dp accent radial gradient behind the icon —
            // an empty state is a resting place, not a moment, and a coloured wash behind a glyph
            // is the single clearest sign that a screen was generated rather than designed.
            val tileShape = RoundedCornerShape(AppRadius.panel.dp)
            Box(
                Modifier
                    .size(64.dp)
                    .clip(tileShape)
                    .background(moovio.surfaceElevated)
                    .border(BorderStroke(1.dp, moovio.hairline), tileShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(it, null, tint = moovio.textTertiary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(AppSpacing.screen))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = moovio.textPrimary)
        subtitle?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = moovio.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        // The button is what turns an empty state from a dead end into a next step.
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(AppRadius.control.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            ) { Text(actionLabel, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** Small outlined pill used to attribute a screen to its data source (e.g. "MangaDex"). */
@Composable
fun SourceBadge(text: String, modifier: Modifier = Modifier) {
    val moovio = LocalMoovioColors.current
    val shape = RoundedCornerShape(AppRadius.chip.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(moovio.surfaceElevated)
            .border(BorderStroke(1.dp, moovio.hairline), shape)
            .padding(horizontal = AppSpacing.tight, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = moovio.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun RatingBadge(text: String) {
    val moovio = LocalMoovioColors.current
    val shape = RoundedCornerShape(AppRadius.chip.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(moovio.surfaceElevated)
            .border(BorderStroke(1.dp, moovio.hairline), shape)
            .padding(horizontal = AppSpacing.tight, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.tnum,
            color = moovio.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    val moovio = LocalMoovioColors.current
    // Two roles, not five hues. A stream list has one best option and everything else, so the
    // high tiers take the accent and the rest stay muted; the old gold/violet/accent/slate/muted
    // ladder turned the sheet into a colour chart the eye has to learn.
    val label = when (tier) {
        ResolutionTier.HDR -> "HDR"
        ResolutionTier.UHD -> "4K"
        ResolutionTier.FHD -> "1080p"
        ResolutionTier.HD -> "720p"
        ResolutionTier.SD -> "SD"
        ResolutionTier.UNKNOWN -> quality.orEmpty()
    }
    val leads = tier == ResolutionTier.HDR || tier == ResolutionTier.UHD || tier == ResolutionTier.FHD
    val foreground = if (leads) moovio.accent else moovio.textTertiary
    val shape = RoundedCornerShape(AppRadius.chip.dp)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.tnum,
        color = foreground,
        modifier = modifier
            .clip(shape)
            .background(foreground.copy(alpha = if (leads) 0.12f else 0.08f))
            .border(BorderStroke(1.dp, foreground.copy(alpha = 0.28f)), shape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

@Composable
fun TagChip(text: String, selected: Boolean = false, onClick: () -> Unit = {}) {
    val moovio = LocalMoovioColors.current
    // A control radius, not a capsule: chips sit next to cards and posters, and when every surface
    // on screen is a pill the shapes stop carrying any meaning.
    val shape = RoundedCornerShape(AppRadius.control.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) moovio.actionFill else moovio.surfaceElevated)
            // An explicit outline marks unselected chips as clearly clickable.
            .border(BorderStroke(1.dp, if (selected) Color.Transparent else moovio.hairline), shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = AppSpacing.tight),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) moovio.onAction else moovio.textSecondary,
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
    val moovio = LocalMoovioColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.card.dp))
            .background(moovio.surfaceElevated)
            .border(BorderStroke(1.dp, moovio.hairline), RoundedCornerShape(AppRadius.card.dp))
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            // One colour: the search glyph marks the field, it is not a state readout.
            tint = moovio.textTertiary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.weight(1f).height(48.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = moovio.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = moovio.textPrimary),
                cursorBrush = SolidColor(moovio.accent),
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
                    .background(moovio.accent.copy(alpha = a))
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
                Icon(Icons.Filled.Close, "Clear search", tint = moovio.textSecondary, modifier = Modifier.size(18.dp))
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
