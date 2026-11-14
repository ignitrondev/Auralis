/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/**
 * A playlist in the catalog. Its tracks are loaded separately, as part of [PlaylistDetail].
 *
 * @property author Owner name as shown by the source, if any.
 * @property subtitle Secondary line, such as a track count or view count.
 */
data class MusicPlaylist(
    override val id: String,
    override val sourceId: String,
    val title: String,
    val author: String?,
    val artworkUri: String?,
    val subtitle: String? = null,
) : CatalogItem
