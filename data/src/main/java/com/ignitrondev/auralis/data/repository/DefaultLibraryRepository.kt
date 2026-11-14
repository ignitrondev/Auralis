/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.Page
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import com.ignitrondev.auralis.provider.api.AccountLibraryProvider
import javax.inject.Inject

/**
 * Account library read straight from the provider. Nothing is cached: history changes with every
 * listen, and the account stays the source of truth (spec section 12.2).
 */
class DefaultLibraryRepository @Inject constructor(
    private val provider: AccountLibraryProvider,
) : LibraryRepository {
    override suspend fun history(): ListeningHistory = provider.history()

    override suspend fun savedAlbums(pageToken: String?): Page<MusicAlbum> = provider.savedAlbums(pageToken)

    override suspend fun isTrackLiked(trackId: String): Boolean = provider.isTrackLiked(trackId)

    override suspend fun setTrackLiked(trackId: String, liked: Boolean) = provider.setTrackLiked(trackId, liked)

    override suspend fun setAlbumSaved(libraryPlaylistId: String, saved: Boolean) = provider.setAlbumSaved(libraryPlaylistId, saved)
}
