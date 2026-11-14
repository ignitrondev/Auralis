/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.ui

import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.MusicArtist
import com.ignitrondev.auralis.domain.model.MusicPlaylist
import com.ignitrondev.auralis.domain.model.MusicTrack

/**
 * Where tapping a catalog item goes. Screens receive this from the app, so feature modules never
 * depend on each other.
 *
 * @property playTrack Null until playback exists; track rows are then not tappable, so they never
 * look like controls that do nothing (spec section 15).
 */
data class CatalogActions(
    val openAlbum: (String) -> Unit,
    val openArtist: (String) -> Unit,
    val openPlaylist: (String) -> Unit,
    val playTrack: ((track: MusicTrack, queue: List<MusicTrack>) -> Unit)? = null,
) {
    /** The tap action for [item], or null when it cannot be opened. */
    fun onClick(item: CatalogItem, queue: List<MusicTrack> = emptyList()): (() -> Unit)? = when (item) {
        is MusicAlbum -> { { openAlbum(item.id) } }
        is MusicArtist -> { { openArtist(item.id) } }
        is MusicPlaylist -> { { openPlaylist(item.id) } }
        is MusicTrack -> playTrack?.let { play -> { play(item, queue.ifEmpty { listOf(item) }) } }
    }

    companion object {
        /** For previews and tests. */
        val None = CatalogActions({}, {}, {})
    }
}
