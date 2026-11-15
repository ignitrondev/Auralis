/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.repository

import com.ignitrondev.auralis.domain.model.Lyrics

/** Lyrics for tracks. Works signed out. Calls can fail with [CatalogException]. */
interface LyricsRepository {
    /** The lyrics of [trackId], or null when the source has none. */
    suspend fun lyrics(trackId: String): Lyrics?
}
