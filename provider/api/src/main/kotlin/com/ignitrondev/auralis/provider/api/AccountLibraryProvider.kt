/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.api

import com.ignitrondev.auralis.domain.model.ListeningHistory

/**
 * The signed-in account's library on a provider (spec section 12.2). Read only. Calls fail with
 * [com.ignitrondev.auralis.domain.repository.CatalogException], with reason `SignInRequired`
 * when there is no working session.
 */
interface AccountLibraryProvider {
    suspend fun history(): ListeningHistory
}
