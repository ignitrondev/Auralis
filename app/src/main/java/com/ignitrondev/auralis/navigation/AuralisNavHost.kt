/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.feature.browse.navigation.browseScreens
import com.ignitrondev.auralis.feature.browse.navigation.navigateToAlbum
import com.ignitrondev.auralis.feature.browse.navigation.navigateToArtist
import com.ignitrondev.auralis.feature.browse.navigation.navigateToPlaylist
import com.ignitrondev.auralis.feature.home.navigation.HomeRoute
import com.ignitrondev.auralis.feature.home.navigation.homeScreen
import com.ignitrondev.auralis.feature.library.navigation.libraryScreen
import com.ignitrondev.auralis.feature.search.navigation.searchScreen
import com.ignitrondev.auralis.feature.settings.navigation.settingsScreen

@Composable
fun AuralisNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // Feature modules never depend on each other: the app tells them where items lead.
    val actions = remember(navController) {
        CatalogActions(
            openAlbum = navController::navigateToAlbum,
            openArtist = navController::navigateToArtist,
            openPlaylist = navController::navigateToPlaylist,
        )
    }
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        homeScreen(actions)
        searchScreen(actions)
        libraryScreen()
        settingsScreen()
        browseScreens(actions, onBack = { navController.popBackStack() })
    }
}
