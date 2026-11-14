/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.MusicArtist
import com.ignitrondev.auralis.domain.model.MusicPlaylist
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.repository.CatalogException

val CatalogItem.title: String
    get() = when (this) {
        is MusicTrack -> title
        is MusicAlbum -> title
        is MusicArtist -> name
        is MusicPlaylist -> title
    }

val CatalogItem.artworkUri: String?
    get() = when (this) {
        is MusicTrack -> artworkUri
        is MusicAlbum -> artworkUri
        is MusicArtist -> artworkUri
        is MusicPlaylist -> artworkUri
    }

/** Secondary line for an item: type and the most useful detail, for example "Album • M83 • 2011". */
@Composable
fun CatalogItem.subtitle(): String {
    val type = when (this) {
        is MusicTrack -> stringResource(if (isVideo) R.string.core_ui_type_video else R.string.core_ui_type_song)
        is MusicAlbum -> stringResource(R.string.core_ui_type_album)
        is MusicArtist -> stringResource(R.string.core_ui_type_artist)
        is MusicPlaylist -> stringResource(R.string.core_ui_type_playlist)
    }
    val details = when (this) {
        is MusicTrack -> listOfNotNull(artistNames.ifBlank { null }, durationMs?.let(::formatDuration))
        is MusicAlbum -> listOfNotNull(artistNames.ifBlank { null }, year)
        is MusicArtist -> listOfNotNull(subtitle)
        is MusicPlaylist -> listOfNotNull(author)
    }
    return (listOf(type) + details).joinToString(" • ")
}

/** 244000 to "4:04", 3723000 to "1:02:03". */
fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

@Composable
fun CatalogException.Reason.message(): String = stringResource(
    when (this) {
        CatalogException.Reason.Network -> R.string.core_ui_error_network
        CatalogException.Reason.NotFound -> R.string.core_ui_error_not_found
        CatalogException.Reason.UnexpectedResponse -> R.string.core_ui_error_unexpected
        CatalogException.Reason.SignInRequired -> R.string.core_ui_error_sign_in
    },
)
