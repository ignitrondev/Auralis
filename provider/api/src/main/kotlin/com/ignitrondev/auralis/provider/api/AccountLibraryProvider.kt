/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.api

import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.Page

/**
 * The signed-in account's library on a provider (spec section 12.2). The `set` calls change the
 * user's real account. Calls fail with
 * [com.ignitrondev.auralis.domain.repository.CatalogException], with reason `SignInRequired`
 * when there is no working session.
 */
interface AccountLibraryProvider {
    suspend fun history(): ListeningHistory

    suspend fun savedAlbums(pageToken: String? = null): Page<MusicAlbum>

    suspend fun isTrackLiked(trackId: String): Boolean

    suspend fun setTrackLiked(trackId: String, liked: Boolean)

    suspend fun setAlbumSaved(libraryPlaylistId: String, saved: Boolean)
}
