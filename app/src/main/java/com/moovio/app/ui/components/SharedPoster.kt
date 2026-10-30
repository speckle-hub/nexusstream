package com.moovio.app.ui.components

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.moovio.app.ui.nav.LocalAnimatedVisibilityScope
import com.moovio.app.ui.nav.LocalSharedTransitionScopeProvider
import com.moovio.app.ui.theme.Motion

/**
 * Marks a poster's artwork as a shared element, so tapping a card morphs that artwork into the
 * detail header instead of cross-fading one screen into another.
 *
 * The navigation layer already installs `SharedTransitionLayout` and publishes both scopes
 * (see `ui/nav/SharedTransition.kt`), but nothing was consuming them — so every navigation fell
 * back to the destination's fade and the morph existed only in the scaffolding. This is the
 * missing consumer.
 *
 * Both ends key off the media item's composite key ([key]), which is what makes the two ends of a
 * navigation match: the poster that was tapped and the header art of the destination.
 *
 * [rail] scopes that key to the row the poster was tapped in. One item can legitimately appear in
 * two rails of the same feed ("Trending" and "Popular" overlap constantly with Stremio add-ons),
 * and two shared elements sharing one key inside an [androidx.compose.animation.SharedTransitionLayout]
 * scope is undefined behaviour — both posters chase the same destination rect. Scoping by rail keeps
 * each card's key unique; the destination passes the same rail through its nav argument, so the pair
 * still matches. Cards outside a rail (library, search, detail's own related row) pass `null` and keep
 * the item-only key.
 *
 * The bounds transform is [Motion.settle] — the app's non-bouncy low-stiffness spring — so the frame
 * decelerates from card geometry into header geometry instead of snapping between the two. Falls
 * back to a plain modifier when there is no transition scope in the tree (previews, tests, any
 * screen composed outside the NavHost), so this never has to be null-checked by callers.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedPoster(key: String, rail: String? = null): Modifier {
    val transitionScope = LocalSharedTransitionScopeProvider.current ?: return this
    val destinationScope = LocalAnimatedVisibilityScope.current ?: return this
    val sharedKey = if (rail.isNullOrBlank()) "poster:$key" else "poster:$rail:$key"
    return with(transitionScope) {
        this@sharedPoster.sharedBounds(
            sharedContentState = rememberSharedContentState(key = sharedKey),
            animatedVisibilityScope = destinationScope,
            boundsTransform = BoundsTransform { _, _ -> Motion.settle<Rect>() },
        )
    }
}
