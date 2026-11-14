/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
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
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        homeScreen()
        searchScreen()
        libraryScreen()
        settingsScreen()
    }
}
