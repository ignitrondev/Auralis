/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/**
 * An artist.
 *
 * @property subtitle Short secondary line from the source, such as an audience size.
 */
data class MusicArtist(
    override val id: String,
    override val sourceId: String,
    val name: String,
    val artworkUri: String?,
    val subtitle: String? = null,
) : CatalogItem
