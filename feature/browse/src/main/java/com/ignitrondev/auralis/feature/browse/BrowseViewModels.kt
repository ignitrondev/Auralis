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
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.CatalogRepository
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import com.ignitrondev.auralis.feature.browse.navigation.AlbumRoute
import com.ignitrondev.auralis.feature.browse.navigation.ArtistRoute
import com.ignitrondev.auralis.feature.browse.navigation.PlaylistRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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

/**
 * The album page's "save to library" button. Shown only when signed in and the album has a
 * library playlist, so it never looks like a control that does nothing.
 *
 * @property failed The last change was rejected; the button is back to the account's state.
 */
data class SaveUiState(
    val libraryPlaylistId: String? = null,
    val saved: Boolean = false,
    val pending: Boolean = false,
    val failed: Boolean = false,
) {
    val visible: Boolean get() = libraryPlaylistId != null
}

@HiltViewModel
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    catalog: CatalogRepository,
    auth: AuthRepository,
    private val library: LibraryRepository,
) : DetailViewModel<AlbumDetail>({ catalog.album(savedStateHandle.toRoute<AlbumRoute>().id) }) {

    private val _save = MutableStateFlow(SaveUiState())
    val save: StateFlow<SaveUiState> = _save.asStateFlow()

    init {
        start()
        viewModelScope.launch {
            combine(auth.state, state) { account, page ->
                val detail = (page as? CatalogUiState.Content)?.data
                if (account is AuthState.SignedIn && detail?.libraryPlaylistId != null) {
                    SaveUiState(libraryPlaylistId = detail.libraryPlaylistId, saved = detail.isSavedToLibrary)
                } else {
                    SaveUiState()
                }
            }.collect { fromPage ->
                // A freshly loaded page is the account's truth; keep local changes for the same album.
                _save.update { current -> if (current.libraryPlaylistId == fromPage.libraryPlaylistId && current.visible) current else fromPage }
            }
        }
    }

    /** Saves or removes the album right away and reverts if the account rejects the change. */
    fun toggleSaved() {
        val current = _save.value
        val playlistId = current.libraryPlaylistId ?: return
        if (current.pending) return
        val target = !current.saved
        _save.value = current.copy(saved = target, pending = true, failed = false)
        viewModelScope.launch {
            val failed = try {
                library.setAlbumSaved(playlistId, target)
                false
            } catch (error: CatalogException) {
                true
            }
            _save.update { state ->
                when {
                    state.libraryPlaylistId != playlistId -> state
                    failed -> state.copy(saved = !target, pending = false, failed = true)
                    else -> state.copy(pending = false)
                }
            }
        }
    }
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
