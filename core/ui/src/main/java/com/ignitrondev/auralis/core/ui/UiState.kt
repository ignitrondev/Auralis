/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.ui

import com.ignitrondev.auralis.domain.repository.CatalogException

/** Loading, error and content states shared by catalog screens (spec section 15 UI rules). */
sealed interface CatalogUiState<out T> {
    data object Loading : CatalogUiState<Nothing>

    data class Error(val reason: CatalogException.Reason) : CatalogUiState<Nothing>

    data class Content<T>(val data: T) : CatalogUiState<T>
}

/** Runs a catalog call and turns its outcome into a [CatalogUiState]. */
suspend fun <T> catalogState(block: suspend () -> T): CatalogUiState<T> =
    try {
        CatalogUiState.Content(block())
    } catch (error: CatalogException) {
        CatalogUiState.Error(error.reason)
    }
