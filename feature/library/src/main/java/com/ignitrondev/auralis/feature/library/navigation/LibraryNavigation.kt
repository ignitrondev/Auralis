/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.library.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.feature.library.LibraryDestination
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

fun NavController.navigateToLibrary(navOptions: NavOptions? = null) = navigate(LibraryRoute, navOptions)

fun NavGraphBuilder.libraryScreen(actions: CatalogActions, onSignIn: () -> Unit) {
    composable<LibraryRoute> {
        LibraryDestination(actions = actions, onSignIn = onSignIn)
    }
}
