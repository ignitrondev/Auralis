/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.repository

import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.model.SearchResults

/**
 * Read access to the music catalog: discovery, search and detail pages. Works signed out.
 * Every call can fail with [CatalogException].
 */
interface CatalogRepository {
    suspend fun home(): HomeFeed

    suspend fun search(query: String): SearchResults

    suspend fun album(id: String): AlbumDetail

    suspend fun artist(id: String): ArtistDetail

    suspend fun playlist(id: String): PlaylistDetail
}

/** A catalog request that failed. [reason] lets the UI show the right state without parsing messages. */
class CatalogException(
    val reason: Reason,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Reason {
        /** No connection, timeout or server error. Retrying may help. */
        Network,

        /** The item does not exist or is not available in this region. */
        NotFound,

        /** The response could not be understood, usually after a change on the service side. */
        UnexpectedResponse,

        /** The data belongs to an account: signed out, or the session no longer works. */
        SignInRequired,
    }
}
