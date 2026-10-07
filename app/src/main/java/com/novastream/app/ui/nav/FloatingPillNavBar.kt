package com.novastream.app.ui.nav

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novastream.app.ui.theme.LocalNovaColors

/** One entry in the floating bar. */
data class FloatingNavItem(val label: String, val icon: ImageVector)

/** Visible height of the floating capsule itself (margins/insets are added around it). */
val FloatingNavBarHeight: Dp = 66.dp

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
 * A modern floating pill navigation bar.
 *
 * Replaces the edge-to-edge docked Material `NavigationBar` with a capsule that floats 16 dp above
 * the gesture inset: translucent glass surface, hairline outline, soft shadow, and a spring-driven
 * pill indicator that slides behind the active tab. It stays on screen permanently — scrolling no
 * longer hides it.
 */
@Composable
fun FloatingPillNavBar(
    items: List<FloatingNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val nova = LocalNovaColors.current
    val shape = RoundedCornerShape(50)
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = navInset + 16.dp)
            .height(FloatingNavBarHeight),
        shape = shape,
        // Glass backdrop: the surface tint at 85% so scrolled content reads faintly through it.
        color = nova.surface.copy(alpha = 0.85f),
        contentColor = nova.textPrimary,
        border = BorderStroke(1.dp, nova.outline.copy(alpha = 0.25f)),
        shadowElevation = 6.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val itemWidth = maxWidth / items.size
            // Sliding active indicator, offset by whole item widths with a low-bounce spring.
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "navIndicatorOffset",
            )
            Box(
                Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(nova.accent.copy(alpha = 0.16f)),
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelect(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) nova.accent else nova.textTertiary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) nova.accent else nova.textTertiary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
