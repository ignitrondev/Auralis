/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

data class MusicAlbum(
    val id: String,
    val title: String,
    val artist: String?,
    val artworkUri: String?,
)
