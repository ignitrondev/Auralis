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
import com.ignitrondev.auralis.feature.library.SavedAlbumsDestination
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

/** Every album saved to the account; opened from the Albums section in Library. */
@Serializable
data object SavedAlbumsRoute

fun NavController.navigateToLibrary(navOptions: NavOptions? = null) = navigate(LibraryRoute, navOptions)

fun NavGraphBuilder.libraryScreen(actions: CatalogActions, onSignIn: () -> Unit, onOpenSavedAlbums: () -> Unit, onBack: () -> Unit) {
    composable<LibraryRoute> {
        LibraryDestination(actions = actions, onSignIn = onSignIn, onOpenSavedAlbums = onOpenSavedAlbums)
    }
    composable<SavedAlbumsRoute> {
        SavedAlbumsDestination(actions = actions, onBack = onBack)
    }
}
