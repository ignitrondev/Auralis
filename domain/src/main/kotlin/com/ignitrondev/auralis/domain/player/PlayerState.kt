/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.player

import com.ignitrondev.auralis.domain.model.MusicTrack

/**
 * Single observable snapshot of playback (spec section 7.3).
 *
 * The playback service owns the real player. UI layers only observe this state. [positionMs]
 * is refreshed at a controlled interval while playing, not every frame.
 *
 * @property currentIndex Index of [currentTrack] in [queue], or -1 when nothing is loaded.
 * @property isBuffering True while the current track is loading (resolving the stream or
 * filling the buffer).
 * @property error The last playback failure, cleared when playback resumes.
 */
data class PlayerState(
    val currentTrack: MusicTrack?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val queue: List<MusicTrack>,
    val currentIndex: Int,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
    val isBuffering: Boolean = false,
    val error: PlaybackError? = null,
) {
    companion object {
        /** Nothing loaded: empty queue, not playing. */
        val Idle = PlayerState(
            currentTrack = null,
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L,
            queue = emptyList(),
            currentIndex = -1,
            shuffleEnabled = false,
            repeatMode = RepeatMode.OFF,
        )
    }
}

/** Why playback stopped, so the UI can show the right message and action. */
enum class PlaybackError {
    /** No connection or a server error. Retrying may help. */
    Network,

    /** The service wants a signed-in account before it plays anything (offer sign-in). */
    SignInRequired,

    /** This track cannot be played (removed, region or age restricted). */
    Unavailable,

    /** Anything else. */
    Unknown,
}
