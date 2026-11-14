/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.api

import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.model.SearchResults

/**
 * Contract every catalog source implements (spec section 6). Providers return Auralis domain
 * models only; their own response types never leave the provider module.
 * Failures are reported as [com.ignitrondev.auralis.domain.repository.CatalogException].
 */
interface CatalogProvider {
    /** Matches the sourceId of every item this provider returns, for example "youtube". */
    val sourceId: String

    suspend fun home(): HomeFeed

    suspend fun search(query: String): SearchResults

    suspend fun album(id: String): AlbumDetail

    suspend fun artist(id: String): ArtistDetail

    suspend fun playlist(id: String): PlaylistDetail
}
