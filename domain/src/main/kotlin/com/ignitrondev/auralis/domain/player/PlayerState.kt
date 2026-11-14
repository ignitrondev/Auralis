/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.player

import com.ignitrondev.auralis.domain.model.MusicTrack

/**
 * Single observable snapshot of playback (spec section 7.3).
 *
 * The playback service owns the real player. UI layers only observe this state.
 *
 * @property currentIndex Index of [currentTrack] in [queue], or -1 when nothing is loaded.
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
