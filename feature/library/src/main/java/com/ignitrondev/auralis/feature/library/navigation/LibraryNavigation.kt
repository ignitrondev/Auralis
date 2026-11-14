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
import com.ignitrondev.auralis.domain.model.LibraryCollection
import com.ignitrondev.auralis.feature.library.LibraryCollectionDestination
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

/**
 * Everything of one kind in the account's library, opened from its section in Library.
 * [collection] is a [LibraryCollection] name.
 */
@Serializable
data class LibraryCollectionRoute(val collection: String)

fun NavController.navigateToLibraryCollection(collection: LibraryCollection) = navigate(LibraryCollectionRoute(collection.name))

fun NavController.navigateToLibrary(navOptions: NavOptions? = null) = navigate(LibraryRoute, navOptions)

fun NavGraphBuilder.libraryScreen(
    actions: CatalogActions,
    onSignIn: () -> Unit,
    onOpenCollection: (LibraryCollection) -> Unit,
    onBack: () -> Unit,
) {
    composable<LibraryRoute> {
        LibraryDestination(actions = actions, onSignIn = onSignIn, onOpenCollection = onOpenCollection)
    }
    composable<LibraryCollectionRoute> {
        LibraryCollectionDestination(actions = actions, onBack = onBack)
    }
}
