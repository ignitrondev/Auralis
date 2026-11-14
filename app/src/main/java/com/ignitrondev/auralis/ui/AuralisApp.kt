/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.ignitrondev.auralis.R
import com.ignitrondev.auralis.core.designsystem.component.Artwork
import com.ignitrondev.auralis.core.designsystem.component.GlassLayers
import com.ignitrondev.auralis.core.designsystem.component.GlassMiniPlayer
import com.ignitrondev.auralis.core.designsystem.component.GlassNavigationBar
import com.ignitrondev.auralis.core.designsystem.component.GlassNavigationBarItem
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.component.glassNavigationBarPadding
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.domain.player.PlaybackError
import com.ignitrondev.auralis.feature.player.NowPlayingScreen
import com.ignitrondev.auralis.feature.settings.navigation.navigateToSignIn
import com.ignitrondev.auralis.navigation.AuralisNavHost
import com.ignitrondev.auralis.navigation.TopLevelDestination

/**
 * App shell: screens fill the window and the floating glass navigation bar sits over their
 * bottom edge, blurring whatever scrolls underneath.
 *
 * Now Playing is laid over everything, not navigated to. Opening it grows the mini player into
 * the full player (shared bounds for the container, a shared element for the artwork) while the
 * navigation bar slides away; collapsing it, with its button or Back, runs the same transition in
 * reverse. Swiping the mini player down stops playback.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
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
    val motion = AuralisTheme.motion
    val barPadding = glassNavigationBarPadding()
    val chromePadding = if (track != null) barPadding + MiniPlayerHeight + spacing.sm else barPadding
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    // Detail and sign-in pages are not tabs: keep the tab they were opened from selected.
    var selectedTab by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    TopLevelDestination.entries.firstOrNull { currentDestination.isInHierarchy(it) }?.let { selectedTab = it }

    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    val showPlayer = playerExpanded && track != null
    LaunchedEffect(track == null) { if (track == null) playerExpanded = false }
    BackHandler(enabled = showPlayer) { playerExpanded = false }
    // The container's corners: the mini player's capsule while small, square once full screen.
    val containerCorner by animateDpAsState(if (showPlayer) 0.dp else MiniPlayerHeight / 2, motion.fade(), label = "player corner")

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassLayers(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AuralisTheme.colors.background),
                overlay = {
                    AnimatedVisibility(
                        visible = track != null && !showPlayer,
                        enter = motion.enter,
                        exit = motion.exit,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = spacing.lg)
                            .padding(bottom = barPadding + spacing.xs),
                    ) {
                        val current = track ?: return@AnimatedVisibility
                        val error = playerState.error
                        GlassMiniPlayer(
                            title = current.title,
                            artist = error?.let { playbackErrorText(it) } ?: current.artistNames.ifBlank { null },
                            isPlaying = playerState.isPlaying,
                            progress = {
                                val duration = playerState.durationMs
                                if (duration > 0) playerState.positionMs.toFloat() / duration else 0f
                            },
                            onPlayPauseClick = player::togglePlayPause,
                            onClick = when (error) {
                                PlaybackError.SignInRequired -> { { navController.navigateToSignIn() } }
                                PlaybackError.Network, PlaybackError.Unknown -> player::retry
                                else -> { { playerExpanded = true } }
                            },
                            onNextClick = if (playerState.currentIndex < playerState.queue.lastIndex) player::next else null,
                            artwork = {
                                Artwork(
                                    uri = current.artworkUri,
                                    contentDescription = null,
                                    shape = AuralisTheme.shapes.small,
                                    modifier = Modifier.sharedArtwork(this@SharedTransitionLayout, this@AnimatedVisibility),
                                )
                            },
                            modifier = Modifier
                                .sharedContainer(this@SharedTransitionLayout, this@AnimatedVisibility, RoundedCornerShape(containerCorner))
                                .swipeToDismiss(
                                    onDismiss = player::stop,
                                    onOpen = { if (error == null) playerExpanded = true },
                                    dismissLabel = stringResource(R.string.mini_player_stop),
                                ),
                        )
                    }
                    AnimatedVisibility(
                        visible = !showPlayer,
                        enter = motion.chromeEnter,
                        exit = motion.chromeExit,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        GlassNavigationBar {
                            TopLevelDestination.entries.forEach { destination ->
                                GlassNavigationBarItem(
                                    selected = destination == selectedTab,
                                    onClick = { navController.navigateToTopLevel(destination) },
                                    icon = destination.icon,
                                    label = stringResource(destination.labelRes),
                                )
                            }
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
            AnimatedVisibility(visible = showPlayer, enter = motion.enter, exit = motion.exit) {
                NowPlayingScreen(
                    onCollapse = { playerExpanded = false },
                    modifier = Modifier.sharedContainer(this@SharedTransitionLayout, this@AnimatedVisibility, RoundedCornerShape(containerCorner)),
                    artworkModifier = Modifier.sharedArtwork(this@SharedTransitionLayout, this@AnimatedVisibility),
                    revealModifier = Modifier.animateEnterExit(enter = motion.reveal, exit = motion.conceal),
                )
            }
        }
    }
}

/** The player container: the mini player capsule and the Now Playing screen are the same surface. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sharedContainer(
    shared: SharedTransitionScope,
    visibility: AnimatedVisibilityScope,
    clip: RoundedCornerShape,
): Modifier = with(shared) {
    this@sharedContainer.sharedBounds(
        sharedContentState = rememberSharedContentState(SHARED_PLAYER),
        animatedVisibilityScope = visibility,
        boundsTransform = AuralisTheme.motion.sharedBounds,
        clipInOverlayDuringTransition = OverlayClip(clip),
    )
}

/**
 * The artwork moves and scales from the mini player into its place in Now Playing. It is not
 * clipped by the growing container: it travels ahead of it, and a clip would show only a slice.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sharedArtwork(shared: SharedTransitionScope, visibility: AnimatedVisibilityScope): Modifier =
    with(shared) {
        this@sharedArtwork.sharedElement(
            sharedContentState = rememberSharedContentState(SHARED_ARTWORK),
            animatedVisibilityScope = visibility,
            boundsTransform = AuralisTheme.motion.sharedBounds,
            clipInOverlayDuringTransition = NoOverlayClip,
        )
    }

@OptIn(ExperimentalSharedTransitionApi::class)
private object NoOverlayClip : SharedTransitionScope.OverlayClip {
    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path? = null
}

private const val SHARED_PLAYER = "player"
private const val SHARED_ARTWORK = "player-artwork"

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
