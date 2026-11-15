/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.api

import com.ignitrondev.auralis.domain.model.Lyrics

/**
 * Lyrics from a provider. Null when it has none for the track. Calls fail with
 * [com.ignitrondev.auralis.domain.repository.CatalogException].
 */
interface LyricsProvider {
    suspend fun lyrics(trackId: String): Lyrics?
}
