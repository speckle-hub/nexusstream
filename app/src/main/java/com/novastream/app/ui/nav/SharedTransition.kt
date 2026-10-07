package com.novastream.app.ui.nav

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The [SharedTransitionScope] installed by `SharedTransitionLayout`.
 *
 * Compose 1.7.0 exposes that scope only as a *receiver* on the `SharedTransitionLayout` content
 * lambda — there is no `LocalSharedTransitionScope` to read (it only arrived in a later release).
 * Screens are many frames below that lambda, so we capture the receiver once at the top of the
 * NavHost and publish it to the tree here.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScopeProvider = staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * The [AnimatedVisibilityScope] of the destination currently being composed.
 *
 * `Modifier.sharedElement` needs one to know which navigation entry a shared element belongs to.
 * Navigation-compose already creates one per destination but doesn't hand it to the screen
 * composables, so each destination publishes it here via [ProvideSharedScope].
 */
val LocalAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Publishes both scopes that shared-element morphs would need.
 *
 * Called inside each `composable { }` body of the NavHost, where navigation-compose supplies the
 * destination's `AnimatedVisibilityScope` as the lambda receiver.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProvideSharedScope(
    sharedTransitionScope: SharedTransitionScope,
    destinationScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSharedTransitionScopeProvider provides sharedTransitionScope,
        LocalAnimatedVisibilityScope provides destinationScope,
    ) {
        content()
    }
}
