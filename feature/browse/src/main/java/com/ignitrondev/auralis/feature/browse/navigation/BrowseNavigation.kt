/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.browse.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ignitrondev.auralis.core.ui.CatalogActions
import com.ignitrondev.auralis.feature.browse.AlbumDestination
import com.ignitrondev.auralis.feature.browse.ArtistDestination
import com.ignitrondev.auralis.feature.browse.PlaylistDestination
import kotlinx.serialization.Serializable

@Serializable
data class AlbumRoute(val id: String)

@Serializable
data class ArtistRoute(val id: String)

@Serializable
data class PlaylistRoute(val id: String)

fun NavController.navigateToAlbum(id: String) = navigate(AlbumRoute(id))

fun NavController.navigateToArtist(id: String) = navigate(ArtistRoute(id))

fun NavController.navigateToPlaylist(id: String) = navigate(PlaylistRoute(id))

/** Album, artist and playlist detail pages, reachable from any tab. */
fun NavGraphBuilder.browseScreens(actions: CatalogActions, onBack: () -> Unit) {
    composable<AlbumRoute> { AlbumDestination(actions = actions, onBack = onBack) }
    composable<ArtistRoute> { ArtistDestination(actions = actions, onBack = onBack) }
    composable<PlaylistRoute> { PlaylistDestination(actions = actions, onBack = onBack) }
}
