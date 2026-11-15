/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.feature.home.HomeDestination
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavController.navigateToHome(navOptions: NavOptions? = null) = navigate(HomeRoute, navOptions)

fun NavGraphBuilder.homeScreen(actions: CatalogActions, onOpenSettings: () -> Unit) {
    composable<HomeRoute> {
        HomeDestination(actions = actions, onOpenSettings = onOpenSettings)
    }
}
