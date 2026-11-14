/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube

import com.metrolist.innertubex.extraction.PoTokenResult
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.strategy.PoTokenProviderKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Mints YouTube BotGuard proof-of-origin tokens for an identifier (a visitor id or a video id).
 * Signed-out playback needs them (docs/spikes/innertubex.md). On Android it is implemented with
 * a hidden WebView that runs Google's own BotGuard script.
 *
 * Tokens are sensitive: implementations never log or persist them.
 */
interface AttestationProvider {
    /** A base64url token bound to [identifier], or null when attestation is unavailable. */
    suspend fun mint(identifier: String): String?

    /** Drops the current attestation, for example after the service rejected a token. */
    suspend fun reset()
}

/**
 * Adapts an [AttestationProvider] to InnerTubeX. The session token (bound to the visitor id) is
 * minted once per visitor and reused; the content token is minted per video.
 */
internal class AttestationTokenProvider(
    private val attestation: AttestationProvider,
) : TokenProvider {

    private val lock = Mutex()
    private var sessionVisitor: String? = null
    private var sessionToken: String? = null

    override val capabilities = TokenProviderCapabilities(
        providers = setOf(PoTokenProviderKind.WEB_BOTGUARD),
        usesWebView = true,
    )

    override suspend fun getPoToken(videoId: String, visitorData: String, cookie: String?): PoTokenResult? =
        try {
            val session = lock.withLock {
                if (sessionVisitor != visitorData || sessionToken == null) {
                    sessionToken = attestation.mint(visitorData)
                    sessionVisitor = visitorData
                }
                sessionToken
            } ?: return null
            val content = attestation.mint(videoId) ?: return null
            PoTokenResult(playerRequestToken = session, streamingDataToken = content, visitorData = visitorData)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Attestation is best-effort: without a token the extractor tries tokenless clients.
            null
        }

    override suspend fun invalidateAttestation() {
        lock.withLock {
            sessionVisitor = null
            sessionToken = null
        }
        attestation.reset()
    }
}
