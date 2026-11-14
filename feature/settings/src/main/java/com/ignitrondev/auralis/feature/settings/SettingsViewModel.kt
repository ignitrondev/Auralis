/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.core.auth.SessionManager
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.provider.api.ProviderSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    val authState: StateFlow<AuthState> = auth.state

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}

/** Sign-in flow state: the WebView, then checking the captured session, then a result. */
sealed interface SignInState {
    data object Browsing : SignInState
    data object Checking : SignInState
    data object Failed : SignInState
    data object Done : SignInState
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val sessions: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow<SignInState>(SignInState.Browsing)
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun onSession(session: ProviderSession) {
        if (_state.value != SignInState.Browsing) return
        _state.value = SignInState.Checking
        viewModelScope.launch {
            _state.value = if (sessions.completeSignIn(session)) SignInState.Done else SignInState.Failed
        }
    }

    fun retry() {
        _state.value = SignInState.Browsing
    }
}
