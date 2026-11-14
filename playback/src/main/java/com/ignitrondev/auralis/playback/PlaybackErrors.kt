/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import com.ignitrondev.auralis.domain.player.PlaybackError
import com.ignitrondev.auralis.provider.api.StreamException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries the reason for the last playback failure from the service to the controller. Media3
 * sends errors to controllers as bundles, which drops the cause chain, so the service publishes
 * the mapped reason here (same process).
 */
@Singleton
class PlaybackErrors @Inject constructor() {
    private val _current = MutableStateFlow<PlaybackError?>(null)
    val current: StateFlow<PlaybackError?> = _current.asStateFlow()

    fun report(error: PlaybackError?) {
        _current.value = error
    }

    companion object {
        /** Maps a player failure by walking its causes. */
        fun classify(error: Throwable): PlaybackError {
            var cause: Throwable? = error
            while (cause != null) {
                if (cause is StreamException) {
                    return when (cause.reason) {
                        StreamException.Reason.Network -> PlaybackError.Network
                        StreamException.Reason.Blocked -> PlaybackError.SignInRequired
                        StreamException.Reason.Unavailable -> PlaybackError.Unavailable
                        // Without a session YouTube answers "no playable stream" to everything.
                        StreamException.Reason.NoStream -> PlaybackError.Unavailable
                    }
                }
                if (cause is java.net.UnknownHostException || cause is java.net.SocketTimeoutException) return PlaybackError.Network
                cause = cause.cause
            }
            return PlaybackError.Unknown
        }
    }
}
