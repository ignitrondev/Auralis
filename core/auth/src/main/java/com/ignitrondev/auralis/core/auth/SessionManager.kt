/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.auth

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.provider.api.AccountSessionProvider
import com.ignitrondev.auralis.provider.api.ProviderSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the YouTube Music session (spec section 12.1). Restores a saved session at startup,
 * accepts a new one from the sign-in screen after checking that it works, and signs out
 * completely. The session only ever lives in [SessionVault] and in the provider's memory.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context,
    private val provider: AccountSessionProvider,
) : AuthRepository {

    private val vault = SessionVault(context)
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<AuthState>(AuthState.Restoring)
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        scope.launch { restore() }
    }

    private suspend fun restore() = lock.withLock {
        val saved = vault.read()
        if (saved == null) {
            _state.value = AuthState.SignedOut
            return@withLock
        }
        provider.applySession(saved)
        val account = provider.currentAccount()
        _state.value = if (account != null) {
            AuthState.SignedIn(account.name, account.handle, account.avatarUrl)
        } else {
            // Keep it applied: the failure may be offline. The UI offers to sign in again.
            AuthState.SessionExpired
        }
    }

    /**
     * Uses [session] if it belongs to a real account, then stores it encrypted.
     * @return false when the session does not work (nothing is stored).
     */
    suspend fun completeSignIn(session: ProviderSession): Boolean = lock.withLock {
        withContext(Dispatchers.IO) {
            provider.applySession(session)
            val account = provider.currentAccount()
            if (account == null) {
                provider.applySession(vault.read())
                return@withContext false
            }
            vault.write(session)
            _state.value = AuthState.SignedIn(account.name, account.handle, account.avatarUrl)
            true
        }
    }

    override suspend fun signOut() = lock.withLock {
        withContext(Dispatchers.IO) { vault.clear() }
        provider.applySession(null)
        withContext(Dispatchers.Main) { clearWebSession() }
        _state.value = AuthState.SignedOut
    }
}

/** Removes every cookie and web storage the sign-in WebView kept. Main thread only. */
internal fun clearWebSession() {
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
    WebStorage.getInstance().deleteAllData()
}
