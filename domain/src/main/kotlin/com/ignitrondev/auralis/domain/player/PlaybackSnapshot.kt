/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.player

import com.ignitrondev.auralis.domain.model.MusicTrack

/**
 * The queue and position saved on the device so playback can continue after the app process ends
 * (spec sections 7.1 and 11). It holds track metadata only, never stream URLs.
 */
data class PlaybackSnapshot(
    val queue: List<MusicTrack>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
)
