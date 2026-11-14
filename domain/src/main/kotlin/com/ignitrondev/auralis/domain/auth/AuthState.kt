/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.auth

import kotlinx.coroutines.flow.StateFlow

/** Account state seen by features (spec section 12.1). Features never see session material. */
sealed interface AuthState {
    /** Still restoring a saved session at startup. */
    data object Restoring : AuthState

    data object SignedOut : AuthState

    /** Only display data (spec 12.1: store the minimum profile data). */
    data class SignedIn(val name: String, val handle: String?, val avatarUrl: String?) : AuthState

    /** A saved session no longer works; the user needs to sign in again. */
    data object SessionExpired : AuthState
}

interface AuthRepository {
    val state: StateFlow<AuthState>

    /** Forgets the session everywhere: encrypted storage, the provider, and the sign-in WebView. */
    suspend fun signOut()
}
