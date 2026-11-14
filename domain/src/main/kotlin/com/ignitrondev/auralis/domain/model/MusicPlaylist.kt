/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

data class MusicPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
)
