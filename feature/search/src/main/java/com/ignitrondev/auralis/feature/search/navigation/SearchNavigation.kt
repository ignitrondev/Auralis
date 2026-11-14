/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.ignitrondev.auralis.feature.search.SearchScreen
import kotlinx.serialization.Serializable

@Serializable
data object SearchRoute

fun NavController.navigateToSearch(navOptions: NavOptions? = null) = navigate(SearchRoute, navOptions)

fun NavGraphBuilder.searchScreen() {
    composable<SearchRoute> {
        SearchScreen()
    }
}
