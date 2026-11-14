/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.repository

import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.LibraryCollection
import com.ignitrondev.auralis.domain.model.Page

/**
 * The signed-in account's library (spec section 12.2). The account is the source of truth. Every
 * call can fail with [CatalogException], with [CatalogException.Reason.SignInRequired] when there
 * is no working session. The `set` calls change the user's real account.
 */
interface LibraryRepository {
    suspend fun history(): ListeningHistory

    /**
     * One kind of item in the account's library, as the account orders it, one page at a time.
     * Pass the previous page's [Page.nextPageToken] to get the next one. Items are of the kind
     * asked for: albums, playlists or artists.
     */
    suspend fun collection(collection: LibraryCollection, pageToken: String? = null): Page<CatalogItem>

    /** Whether the account liked [trackId]. */
    suspend fun isTrackLiked(trackId: String): Boolean

    /** Likes [trackId] (it joins Liked music) or removes the like. */
    suspend fun setTrackLiked(trackId: String, liked: Boolean)

    /** Saves an album to the library, or removes it, by its [com.ignitrondev.auralis.domain.model.AlbumDetail.libraryPlaylistId]. */
    suspend fun setAlbumSaved(libraryPlaylistId: String, saved: Boolean)

    companion object {
        /** The account's "Liked music" auto playlist; open it like any playlist. */
        const val LIKED_MUSIC_PLAYLIST_ID = "LM"
    }
}
