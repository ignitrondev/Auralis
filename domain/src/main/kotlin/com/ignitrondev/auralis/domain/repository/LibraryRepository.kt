/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.repository

import com.ignitrondev.auralis.domain.model.ListeningHistory

/**
 * Read access to the signed-in account's library (spec section 12.2). The account is the source
 * of truth. Every call can fail with [CatalogException], with [CatalogException.Reason.SignInRequired]
 * when there is no working session.
 */
interface LibraryRepository {
    suspend fun history(): ListeningHistory
}
