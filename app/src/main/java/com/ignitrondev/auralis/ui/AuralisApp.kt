/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.R
import com.ignitrondev.auralis.core.designsystem.component.Artwork
import com.ignitrondev.auralis.core.designsystem.component.GlassMiniPlayer
import com.ignitrondev.auralis.domain.player.PlaybackError
import com.ignitrondev.auralis.feature.settings.navigation.navigateToSignIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.ignitrondev.auralis.core.designsystem.component.GlassLayers
import com.ignitrondev.auralis.core.designsystem.component.GlassNavigationBar
import com.ignitrondev.auralis.core.designsystem.component.GlassNavigationBarItem
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.component.glassNavigationBarPadding
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.navigation.AuralisNavHost
import com.ignitrondev.auralis.navigation.TopLevelDestination

/**
 * App shell: screens fill the window and the floating glass navigation bar sits over their
 * bottom edge, blurring whatever scrolls underneath.
 */
@Composable
fun AuralisApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val player = appViewModel.player
    val playerState by player.state.collectAsStateWithLifecycle()
    val track = playerState.currentTrack
    val spacing = AuralisTheme.spacing
    val barPadding = glassNavigationBarPadding()
    val chromePadding = if (track != null) barPadding + MiniPlayerHeight + spacing.sm else barPadding
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    // Detail and sign-in pages are not tabs: keep the tab they were opened from selected.
    var selectedTab by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    TopLevelDestination.entries.firstOrNull { currentDestination.isInHierarchy(it) }?.let { selectedTab = it }

    GlassLayers(
        modifier = modifier
            .fillMaxSize()
            .background(AuralisTheme.colors.background),
        overlay = {
            if (track != null) {
                val error = playerState.error
                GlassMiniPlayer(
                    title = track.title,
                    artist = error?.let { playbackErrorText(it) } ?: track.artistNames.ifBlank { null },
                    isPlaying = playerState.isPlaying,
                    progress = {
                        val duration = playerState.durationMs
                        if (duration > 0) playerState.positionMs.toFloat() / duration else 0f
                    },
                    onPlayPauseClick = player::togglePlayPause,
                    onClick = when (error) {
                        PlaybackError.SignInRequired -> { { navController.navigateToSignIn() } }
                        PlaybackError.Network, PlaybackError.Unknown -> player::retry
                        // Now Playing arrives in Phase 5; until then the player has no tap action.
                        else -> null
                    },
                    onNextClick = if (playerState.currentIndex < playerState.queue.lastIndex) player::next else null,
                    artwork = { Artwork(uri = track.artworkUri, contentDescription = null, shape = CircleShape) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = spacing.lg)
                        .padding(bottom = barPadding + spacing.xs),
                )
            }
            GlassNavigationBar(modifier = Modifier.align(Alignment.BottomCenter)) {
                TopLevelDestination.entries.forEach { destination ->
                    GlassNavigationBarItem(
                        selected = destination == selectedTab,
                        onClick = { navController.navigateToTopLevel(destination) },
                        icon = destination.icon,
                        label = stringResource(destination.labelRes),
                    )
                }
            }
        },
    ) {
        CompositionLocalProvider(LocalBottomChromePadding provides chromePadding) {
            AuralisNavHost(
                navController = navController,
                player = player,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val MiniPlayerHeight = 64.dp

@Composable
private fun playbackErrorText(error: PlaybackError): String = stringResource(
    when (error) {
        PlaybackError.SignInRequired -> R.string.playback_error_sign_in
        PlaybackError.Network -> R.string.playback_error_network
        PlaybackError.Unavailable -> R.string.playback_error_unavailable
        PlaybackError.Unknown -> R.string.playback_error_unknown
    },
)

private fun NavDestination?.isInHierarchy(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

/**
 * Switches tabs without growing the back stack: pops to the start destination, keeps one copy
 * of each tab, and saves/restores each tab's state.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    val options = navOptions {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    navigate(destination.route, options)
}
