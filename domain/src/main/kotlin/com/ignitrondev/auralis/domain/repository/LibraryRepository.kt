/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.repository

import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicAlbum

/**
 * Read access to the signed-in account's library (spec section 12.2). The account is the source
 * of truth. Every call can fail with [CatalogException], with [CatalogException.Reason.SignInRequired]
 * when there is no working session.
 */
interface LibraryRepository {
    suspend fun history(): ListeningHistory

    /** Albums saved to the account's library, most recently saved first. */
    suspend fun savedAlbums(): List<MusicAlbum>

    companion object {
        /** The account's "Liked music" auto playlist; open it like any playlist. */
        const val LIKED_MUSIC_PLAYLIST_ID = "LM"
    }
}
