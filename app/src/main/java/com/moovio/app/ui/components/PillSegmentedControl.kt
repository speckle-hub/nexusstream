package com.moovio.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moovio.app.ui.theme.AppRadius
import com.moovio.app.ui.theme.LocalMoovioColors
import com.moovio.app.ui.theme.Motion
import com.moovio.app.ui.theme.rememberMoovioHaptics

/**
 * A segmented control with a spring-animated selection indicator.
 *
 * Shared by the Browse and Library sub-navigation rows so the two read as the same control.
 *
 * Deliberately not a pill any more. A 48dp-tall capsule whose selected segment was an accent wash
 * made the control the loudest thing on Browse, above the posters it was filtering; the geometry is
 * now a panel radius (16) holding a 12-radius indicator, which is the nested step the radius scale
 * prescribes, and selection is a neutral surface wash rather than colour. The label weight no longer
 * changes with selection either: size and colour carry the hierarchy, so bumping to Bold as well was
 * shouting twice.
 */
@Composable
fun PillSegmentedControl(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return
    val moovio = LocalMoovioColors.current
    val haptics = rememberMoovioHaptics()
    val shape = RoundedCornerShape(AppRadius.panel.dp)
    val itemShape = RoundedCornerShape(AppRadius.card.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = moovio.surface,
        border = BorderStroke(1.dp, moovio.hairline),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(4.dp),
        ) {
            val itemWidth = maxWidth / labels.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selected,
                // The app's settling spring at a mid stiffness: an indicator that tracks a tap
                // should arrive sooner than a page change, and still never overshoot.
                animationSpec = Motion.settle(stiffness = Spring.StiffnessMediumLow),
                label = "segIndicatorOffset",
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
                labels.forEachIndexed { index, label ->
                    val active = index == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(itemShape)
                            .clickable {
                                if (!active) {
                                    haptics.select()
                                    onSelect(index)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) moovio.textPrimary else moovio.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
