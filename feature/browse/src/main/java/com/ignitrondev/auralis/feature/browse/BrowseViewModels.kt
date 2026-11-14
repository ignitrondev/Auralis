/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.catalogState
import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.repository.CatalogRepository
import com.ignitrondev.auralis.feature.browse.navigation.AlbumRoute
import com.ignitrondev.auralis.feature.browse.navigation.ArtistRoute
import com.ignitrondev.auralis.feature.browse.navigation.PlaylistRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Loads one detail page and exposes it as a [CatalogUiState], with retry. */
abstract class DetailViewModel<T>(private val load: suspend () -> T) : ViewModel() {
    private val _state = MutableStateFlow<CatalogUiState<T>>(CatalogUiState.Loading)
    val state: StateFlow<CatalogUiState<T>> = _state.asStateFlow()

    protected fun start() = retry()

    fun retry() {
        _state.value = CatalogUiState.Loading
        viewModelScope.launch { _state.value = catalogState { load() } }
    }
}

@HiltViewModel
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    catalog: CatalogRepository,
) : DetailViewModel<AlbumDetail>({ catalog.album(savedStateHandle.toRoute<AlbumRoute>().id) }) {
    init { start() }
}

@HiltViewModel
class ArtistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    catalog: CatalogRepository,
) : DetailViewModel<ArtistDetail>({ catalog.artist(savedStateHandle.toRoute<ArtistRoute>().id) }) {
    init { start() }
}

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    catalog: CatalogRepository,
) : DetailViewModel<PlaylistDetail>({ catalog.playlist(savedStateHandle.toRoute<PlaylistRoute>().id) }) {
    init { start() }
}
