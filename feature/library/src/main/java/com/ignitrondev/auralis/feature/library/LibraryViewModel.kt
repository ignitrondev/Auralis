/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.catalogState
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** What Library shows for a signed-in account. */
data class LibraryContent(val albums: List<MusicAlbum>, val history: ListeningHistory)

/**
 * The account's saved albums and listening history. They load only when signed in, and reload
 * when the account changes and each time the screen is shown again, because every listen changes
 * the history. Signed out or with an expired session the state is an error with
 * [CatalogException.Reason.SignInRequired].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    auth: AuthRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    private val reloads = MutableStateFlow(0)

    val state: StateFlow<CatalogUiState<LibraryContent>> =
        combine(auth.state, reloads) { account, _ -> account }
            .flatMapLatest<AuthState, CatalogUiState<LibraryContent>> { account ->
                when (account) {
                    AuthState.Restoring -> flowOf(CatalogUiState.Loading)
                    AuthState.SignedOut, AuthState.SessionExpired ->
                        flowOf(CatalogUiState.Error(CatalogException.Reason.SignInRequired))
                    is AuthState.SignedIn -> flow {
                        emit(CatalogUiState.Loading)
                        emit(catalogState { loadContent() })
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CatalogUiState.Loading)

    fun retry() = reloads.update { it + 1 }

    /**
     * Albums and history load in parallel. The history is the main content, so only its failure
     * fails the screen; albums that cannot load are left out, unless the session was rejected.
     */
    private suspend fun loadContent(): LibraryContent = coroutineScope {
        val albums = async {
            try {
                library.savedAlbums()
            } catch (error: CatalogException) {
                if (error.reason == CatalogException.Reason.SignInRequired) throw error
                emptyList()
            }
        }
        val history = library.history()
        LibraryContent(albums.await(), history)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
