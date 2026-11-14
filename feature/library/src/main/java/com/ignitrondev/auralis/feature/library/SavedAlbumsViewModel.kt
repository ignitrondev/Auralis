/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Every album saved to the account, loaded a page at a time as the list scrolls.
 *
 * @property canLoadMore There is a next page and nothing is loading.
 * @property loadMoreFailed The last next-page request failed; the albums loaded so far stay.
 */
data class SavedAlbums(
    val albums: List<MusicAlbum>,
    val nextPageToken: String?,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
) {
    val canLoadMore: Boolean get() = nextPageToken != null && !loadingMore
}

@HiltViewModel
class SavedAlbumsViewModel @Inject constructor(
    private val library: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<CatalogUiState<SavedAlbums>>(CatalogUiState.Loading)
    val state: StateFlow<CatalogUiState<SavedAlbums>> = _state.asStateFlow()

    init {
        retry()
    }

    /** Loads the first page again, after an error. */
    fun retry() {
        _state.value = CatalogUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val page = library.savedAlbums()
                CatalogUiState.Content(SavedAlbums(page.items, page.nextPageToken))
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
                library.savedAlbums(token)
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
                        SavedAlbums((data.albums + next.items).distinctBy { it.id }, next.nextPageToken)
                    },
                )
            }
        }
    }
}
