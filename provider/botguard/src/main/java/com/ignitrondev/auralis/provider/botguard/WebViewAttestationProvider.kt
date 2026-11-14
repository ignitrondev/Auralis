/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.botguard

import android.content.Context
import com.ignitrondev.auralis.provider.youtube.AttestationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [AttestationProvider] backed by a hidden WebView running YouTube's BotGuard (the approach used
 * by NewPipe and Metrolist). One session is kept and reused until its integrity token nears
 * expiry; a failed mint recreates it once.
 */
class WebViewAttestationProvider(context: Context) : AttestationProvider {
    private val context = context.applicationContext
    private val lock = Mutex()
    private var session: BotGuardSession? = null

    override suspend fun mint(identifier: String): String? =
        // Builds without the key skip attestation; signed-in playback does not need it.
        if (!BotGuardProtocol.isConfigured) {
            null
        } else try {
            mintWithRetry(identifier)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }

    private suspend fun mintWithRetry(identifier: String): String {
        val first = currentSession()
        return try {
            first.mint(identifier)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // The page can be lost when the app was in the background: start over once.
            reset()
            currentSession().mint(identifier)
        }
    }

    private suspend fun currentSession(): BotGuardSession = lock.withLock {
        session?.takeUnless { it.isExpired } ?: run {
            session?.let { old -> withContext(Dispatchers.Main) { old.destroy() } }
            BotGuardSession.start(context).also { session = it }
        }
    }

    override suspend fun reset() {
        lock.withLock {
            session?.let { old -> withContext(Dispatchers.Main) { old.destroy() } }
            session = null
        }
    }
}
