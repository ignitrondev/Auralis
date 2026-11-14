/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.repository

import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.Page

/**
 * The signed-in account's library (spec section 12.2). The account is the source of truth. Every
 * call can fail with [CatalogException], with [CatalogException.Reason.SignInRequired] when there
 * is no working session. The `set` calls change the user's real account.
 */
interface LibraryRepository {
    suspend fun history(): ListeningHistory

    /**
     * Albums saved to the account's library, most recently saved first, one page at a time.
     * Pass the previous page's [Page.nextPageToken] to get the next one.
     */
    suspend fun savedAlbums(pageToken: String? = null): Page<MusicAlbum>

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
