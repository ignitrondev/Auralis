/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.LibraryCollection
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import com.ignitrondev.auralis.feature.library.navigation.LibraryCollectionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything of one kind in the library, loaded a page at a time as the list scrolls.
 *
 * @property canLoadMore There is a next page and nothing is loading.
 * @property loadMoreFailed The last next-page request failed; the items loaded so far stay.
 */
data class CollectionItems(
    val items: List<CatalogItem>,
    val nextPageToken: String?,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
) {
    val canLoadMore: Boolean get() = nextPageToken != null && !loadingMore
}

@HiltViewModel
class LibraryCollectionViewModel internal constructor(
    val collection: LibraryCollection,
    private val library: LibraryRepository,
) : ViewModel() {

    @Inject
    constructor(savedStateHandle: SavedStateHandle, library: LibraryRepository) : this(
        LibraryCollection.valueOf(savedStateHandle.toRoute<LibraryCollectionRoute>().collection),
        library,
    )

    private val _state = MutableStateFlow<CatalogUiState<CollectionItems>>(CatalogUiState.Loading)
    val state: StateFlow<CatalogUiState<CollectionItems>> = _state.asStateFlow()

    init {
        retry()
    }

    /** Loads the first page again, after an error. */
    fun retry() {
        _state.value = CatalogUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val page = library.collection(collection)
                CatalogUiState.Content(CollectionItems(page.items, page.nextPageToken))
            } catch (error: CatalogException) {
                CatalogUiState.Error(error.reason)
            }
        }
    }

    /** Appends the next page. Does nothing while a page is loading or after the last one. */
    fun loadMore() {
        val current = (_state.value as? CatalogUiState.Content)?.data ?: return
        val token = current.nextPageToken ?: return
        if (current.loadingMore) return
        _state.value = CatalogUiState.Content(current.copy(loadingMore = true, loadMoreFailed = false))
        viewModelScope.launch {
            val next = try {
                library.collection(collection, token)
            } catch (error: CatalogException) {
                null
            }
            _state.update { state ->
                val data = (state as? CatalogUiState.Content)?.data ?: return@update state
                CatalogUiState.Content(
                    if (next == null) {
                        data.copy(loadingMore = false, loadMoreFailed = true)
                    } else {
                        // Pages can overlap when the library changes between requests.
                        CollectionItems((data.items + next.items).distinctBy { it.id }, next.nextPageToken)
                    },
                )
            }
        }
    }
}
