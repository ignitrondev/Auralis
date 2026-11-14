/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/**
 * A playable track from a streaming catalog.
 *
 * The playable stream is never part of the model: it is resolved by the provider just before
 * playback, because stream URLs are short-lived (spec section 6.1).
 *
 * @property durationMs Null when the source does not say.
 * @property isVideo True for music videos and uploads; false for audio tracks ("songs").
 */
data class MusicTrack(
    override val id: String,
    override val sourceId: String,
    val title: String,
    val artists: List<ArtistRef>,
    val album: AlbumRef?,
    val durationMs: Long?,
    val artworkUri: String?,
    val isVideo: Boolean = false,
    val isExplicit: Boolean = false,
) : CatalogItem {
    /** Artist names joined for display, for example "Daft Punk, Pharrell Williams". */
    val artistNames: String get() = artists.joinToString(", ") { it.name }
}
