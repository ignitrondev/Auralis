/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.feature.browse.navigation.browseScreens
import com.ignitrondev.auralis.feature.browse.navigation.navigateToAlbum
import com.ignitrondev.auralis.feature.browse.navigation.navigateToArtist
import com.ignitrondev.auralis.feature.browse.navigation.navigateToPlaylist
import com.ignitrondev.auralis.feature.home.navigation.HomeRoute
import com.ignitrondev.auralis.feature.home.navigation.homeScreen
import com.ignitrondev.auralis.feature.library.navigation.libraryScreen
import com.ignitrondev.auralis.feature.player.navigation.NowPlayingRoute
import com.ignitrondev.auralis.feature.player.navigation.nowPlayingScreen
import com.ignitrondev.auralis.feature.search.navigation.searchScreen
import com.ignitrondev.auralis.feature.settings.navigation.navigateToSignIn
import com.ignitrondev.auralis.feature.settings.navigation.settingsScreen

@Composable
fun AuralisNavHost(
    navController: NavHostController,
    player: PlayerController,
    modifier: Modifier = Modifier,
) {
    // Feature modules never depend on each other: the app tells them where items lead.
    val actions = remember(navController, player) {
        CatalogActions(
            openAlbum = navController::navigateToAlbum,
            openArtist = navController::navigateToArtist,
            openPlaylist = navController::navigateToPlaylist,
            playTrack = { track, queue -> player.play(queueStartingAt(track, queue)) },
        )
    }
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        // Short fades between pages. Now Playing is a sheet over the current page: the page under
        // it stays still and fully visible while the sheet slides up or down.
        enterTransition = { fadeIn(tween(PAGE_FADE_MS)) },
        exitTransition = {
            if (targetState.destination.hasRoute(NowPlayingRoute::class)) ExitTransition.KeepUntilTransitionsFinished else fadeOut(tween(PAGE_FADE_MS))
        },
        popEnterTransition = {
            if (initialState.destination.hasRoute(NowPlayingRoute::class)) EnterTransition.None else fadeIn(tween(PAGE_FADE_MS))
        },
        popExitTransition = { fadeOut(tween(PAGE_FADE_MS)) },
    ) {
        homeScreen(actions)
        searchScreen(actions)
        libraryScreen()
        settingsScreen(onSignIn = navController::navigateToSignIn, onSignInDone = { navController.popBackStack() })
        browseScreens(actions, onBack = { navController.popBackStack() })
        nowPlayingScreen(onCollapse = { navController.popBackStack() })
    }
}

/**
 * A tapped track plays with the list it was in as the queue. A track that is not in that list
 * (the search top result is shown apart from the songs) goes first, so the tapped track is
 * always the one that plays.
 */
internal fun queueStartingAt(track: MusicTrack, queue: List<MusicTrack>): Pair<List<MusicTrack>, Int> {
    val index = queue.indexOfFirst { it.id == track.id && it.sourceId == track.sourceId }
    return if (index >= 0) queue to index else (listOf(track) + queue) to 0
}

private fun PlayerController.play(start: Pair<List<MusicTrack>, Int>) = play(start.first, start.second)

private const val PAGE_FADE_MS = 200
