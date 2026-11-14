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
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * The account's listening history. It loads only when signed in, reloads when the account changes
 * and each time the screen is shown again, because every listen changes it. Signed out or with an
 * expired session the state is an error with [CatalogException.Reason.SignInRequired].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    auth: AuthRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    private val reloads = MutableStateFlow(0)

    val state: StateFlow<CatalogUiState<ListeningHistory>> =
        combine(auth.state, reloads) { account, _ -> account }
            .flatMapLatest<AuthState, CatalogUiState<ListeningHistory>> { account ->
                when (account) {
                    AuthState.Restoring -> flowOf(CatalogUiState.Loading)
                    AuthState.SignedOut, AuthState.SessionExpired ->
                        flowOf(CatalogUiState.Error(CatalogException.Reason.SignInRequired))
                    is AuthState.SignedIn -> flow {
                        emit(CatalogUiState.Loading)
                        emit(catalogState { library.history() })
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CatalogUiState.Loading)

    fun retry() = reloads.update { it + 1 }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
