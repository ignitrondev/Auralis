/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/**
 * A playable track, independent of the provider it comes from.
 *
 * @property id Stable Auralis-wide identifier.
 * @property sourceId Identifier of the provider that owns this track (for example "youtube").
 * @property mediaUri Direct media location when already known. Usually null for streaming
 * sources, where the playable stream is resolved just before playback.
 */
data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val artworkUri: String?,
    val sourceId: String,
    val mediaUri: String?,
    val isFavorite: Boolean,
)
