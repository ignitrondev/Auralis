/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.player

import com.ignitrondev.auralis.domain.model.MusicTrack
import kotlinx.coroutines.flow.StateFlow

/**
 * Controls the single playback engine (spec section 7). Screens call this and observe [state];
 * they never create a player themselves.
 */
interface PlayerController {
    val state: StateFlow<PlayerState>

    /** Replaces the queue with [queue] and starts playing [queue][startIndex]. */
    fun play(queue: List<MusicTrack>, startIndex: Int = 0)

    fun togglePlayPause()

    fun next()

    fun previous()

    fun seekTo(positionMs: Long)

    fun skipTo(queueIndex: Int)

    fun setShuffle(enabled: Boolean)

    fun setRepeatMode(mode: RepeatMode)

    /** Retries after an error, keeping the position. */
    fun retry()

    /**
     * Stops playback and clears the queue, including the saved one: nothing is left to resume and
     * the media notification goes away.
     */
    fun stop()
}
