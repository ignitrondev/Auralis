/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/**
 * Anything the catalog can show in a list or carousel. Provider-independent: providers map their
 * own responses into these types (spec sections 5 and 6).
 */
sealed interface CatalogItem {
    /** Stable identifier within [sourceId]. */
    val id: String

    /** Provider that owns the item, for example "youtube". Together with [id] it is unique. */
    val sourceId: String
}

/** A link to an artist as shown next to a track or album. [id] is null when not browsable. */
data class ArtistRef(val id: String?, val name: String)

/** A link to the album a track belongs to. [id] is null when not browsable. */
data class AlbumRef(val id: String?, val title: String)
