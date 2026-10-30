package com.moovio.app.ui.nav

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moovio.app.ui.theme.AppRadius
import com.moovio.app.ui.theme.LocalMoovioColors
import com.moovio.app.ui.theme.Motion
import com.moovio.app.ui.theme.rememberMoovioHaptics

/** One entry in the floating bar. */
data class FloatingNavItem(val label: String, val icon: ImageVector)

/** Visible height of the floating capsule itself (margins/insets are added around it). */
val FloatingNavBarHeight: Dp = 64.dp

/**
 * Bottom content padding a tab screen should add so its last row clears the floating bar.
 *
 * The bar is **always visible**: it used to hide itself on downward scroll through a shared
 * `NestedScrollConnection`, but the direction detection felt inverted and auto-hiding the primary
 * navigation was more annoying than it was worth, so the scroll plumbing is gone and only this
 * padding local remains (the bar still overlays the content, so lists need the clearance).
 */
val LocalFloatingNavBottomPadding = staticCompositionLocalOf { 0.dp }

/**
 * The floating bottom navigation bar.
 *
 * It stays a floating bar — it overlays scrolling content and needs the inset — but no longer a
 * glass pill: the 85%-alpha surface let posters shimmer through the labels, and a 66dp capsule was
 * the only 9999-radius shape left in the nav. It is now a 64dp panel radius (16) on a solid raised
 * surface with a hairline, a 12-radius selection wash in neutral text ink, and one icon and label
 * weight whether or not a tab is selected. Colour is reserved for content, so the bar stops
 * competing with the artwork behind it.
 */
@Composable
fun FloatingPillNavBar(
    items: List<FloatingNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val moovio = LocalMoovioColors.current
    // One tactile tick per tab switch (skipped for the already-active tab).
    val haptics = rememberMoovioHaptics()
    val shape = RoundedCornerShape(AppRadius.panel.dp)
    val itemShape = RoundedCornerShape(AppRadius.card.dp)
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = navInset + 16.dp)
            .height(FloatingNavBarHeight),
        shape = shape,
        color = moovio.surfaceElevated,
        contentColor = moovio.textPrimary,
        border = BorderStroke(1.dp, moovio.hairline),
        // Dark depth comes from the surface step plus the hairline; only the light theme needs a
        // shadow to lift a white bar off a white page.
        shadowElevation = if (moovio.isDark) 0.dp else 2.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
            val itemWidth = maxWidth / items.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = Motion.settle(stiffness = Spring.StiffnessMediumLow),
                label = "navIndicatorOffset",
            )
            Box(
                Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(itemShape)
                    .background(moovio.textPrimary.copy(alpha = 0.10f)),
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(itemShape)
                            .clickable {
                                if (index != selectedIndex) {
                                    haptics.select()
                                    onSelect(index)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) moovio.textPrimary else moovio.textTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) moovio.textPrimary else moovio.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
