/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ignitrondev.auralis.feature.player.NowPlayingScreen
import kotlinx.serialization.Serializable

@Serializable
data object NowPlayingRoute

fun NavController.navigateToNowPlaying() = navigate(NowPlayingRoute) { launchSingleTop = true }

/** Now Playing slides up over the current screen and back down when collapsed. */
fun NavGraphBuilder.nowPlayingScreen(onCollapse: () -> Unit) {
    composable<NowPlayingRoute>(
        enterTransition = { slideInVertically(tween(320)) { it } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(160)) },
        popExitTransition = { slideOutVertically(tween(280)) { it } },
    ) {
        NowPlayingScreen(onCollapse = onCollapse)
    }
}
