/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.repository

import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.player.PlaybackSnapshot
import com.ignitrondev.auralis.domain.player.RepeatMode

/**
 * Stores the last playback queue on the device. The queue and the progress are saved separately,
 * because progress changes far more often than the queue.
 */
interface PlaybackSnapshotRepository {
    /** The saved snapshot, or null when there is no saved queue. */
    suspend fun load(): PlaybackSnapshot?

    /** Replaces the saved queue. An empty [queue] clears the snapshot. */
    suspend fun saveQueue(queue: List<MusicTrack>)

    suspend fun saveProgress(currentIndex: Int, positionMs: Long, shuffleEnabled: Boolean, repeatMode: RepeatMode)
}
