/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import com.ignitrondev.auralis.provider.api.PlayableStream
import com.ignitrondev.auralis.provider.api.StreamProvider
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a track id into a playable stream just before playback, and keeps resolved streams until
 * shortly before they expire. Stream URLs are signed and short-lived: they stay in memory only
 * and are never logged (spec section 6.1).
 */
@Singleton
class StreamResolver @Inject constructor(
    private val provider: StreamProvider,
) {
    private val cache = ConcurrentHashMap<String, PlayableStream>()

    /** Called on ExoPlayer's loading thread, which may block. */
    fun resolveBlocking(trackId: String): PlayableStream =
        cached(trackId) ?: runBlocking { provider.resolveStream(trackId) }.also { cache[trackId] = it }

    /** Resolves ahead of time (for example the next track) so the transition does not wait. */
    suspend fun prefetch(trackId: String) {
        if (cached(trackId) != null) return
        runCatching { provider.resolveStream(trackId) }.onSuccess { cache[trackId] = it }
    }

    /** Drops a stream the server rejected, so the next attempt resolves a fresh one. */
    fun invalidate(trackId: String) {
        cache.remove(trackId)
    }

    private fun cached(trackId: String): PlayableStream? {
        val stream = cache[trackId] ?: return null
        val expiresAt = stream.expiresAtMs ?: (System.currentTimeMillis() + FALLBACK_LIFETIME_MS)
        return if (System.currentTimeMillis() < expiresAt - EXPIRY_MARGIN_MS) stream else null.also { cache.remove(trackId) }
    }

    private companion object {
        const val EXPIRY_MARGIN_MS = 2 * 60 * 1000L
        const val FALLBACK_LIFETIME_MS = 60 * 60 * 1000L
    }
}
