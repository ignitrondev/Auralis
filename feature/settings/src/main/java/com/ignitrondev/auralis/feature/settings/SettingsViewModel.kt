/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.core.auth.SessionManager
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.update.AppInfo
import com.ignitrondev.auralis.domain.update.AppUpdates
import com.ignitrondev.auralis.domain.update.ReleaseNote
import com.ignitrondev.auralis.provider.api.ProviderSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The update check in Settings. It runs only when the user asks. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState

    /** @property downloadUrl The APK; opening it in the browser downloads it. */
    data class Available(
        val version: String,
        val downloadUrl: String,
        val pageUrl: String,
        val notes: List<ReleaseNote> = emptyList(),
    ) : UpdateState

    data object Failed : UpdateState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val appInfo: AppInfo,
    private val updates: AppUpdates,
) : ViewModel() {
    val authState: StateFlow<AuthState> = auth.state

    val installedVersion: String get() = appInfo.versionName

    private val check = MutableStateFlow<UpdateState>(UpdateState.Idle)

    /**
     * An update found earlier (by the check on app start) shows right away; a check the user asks
     * for shows its own progress and result.
     */
    val update: StateFlow<UpdateState> = combine(check, updates.available) { check, available ->
        when {
            check == UpdateState.Checking -> check
            available != null -> UpdateState.Available(available.version.toString(), available.apkUrl ?: available.pageUrl, available.pageUrl, available.notes)
            else -> check
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UpdateState.Idle)

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    fun checkForUpdates() {
        if (check.value == UpdateState.Checking) return
        check.value = UpdateState.Checking
        viewModelScope.launch {
            check.value = try {
                // A found update shows through [AppUpdates.available].
                if (updates.checkNow() == null) UpdateState.UpToDate else UpdateState.Idle
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                UpdateState.Failed
            }
        }
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
