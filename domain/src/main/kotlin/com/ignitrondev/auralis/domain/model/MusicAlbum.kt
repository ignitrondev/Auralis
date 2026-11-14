/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/**
 * An album, single or EP.
 *
 * @property year Release year as shown by the source, if any.
 */
data class MusicAlbum(
    override val id: String,
    override val sourceId: String,
    val title: String,
    val artists: List<ArtistRef>,
    val year: String?,
    val artworkUri: String?,
    val isExplicit: Boolean = false,
) : CatalogItem {
    val artistNames: String get() = artists.joinToString(", ") { it.name }
}
