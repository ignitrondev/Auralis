/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.share

/**
 * What a share card shows, already in display text.
 *
 * @property subtitle For example "Song • Tulus".
 * @property link The public link shared next to the image; null when the item has none.
 * @property lyrics Lines for a lyrics card; empty for an item card.
 */
data class ShareCard(
    val title: String,
    val subtitle: String?,
    val artworkUri: String?,
    val link: String?,
    val lyrics: List<String> = emptyList(),
) {
    val isLyrics: Boolean get() = lyrics.isNotEmpty()
}
