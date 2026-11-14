/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ignitrondev.auralis.domain.model.AlbumRef
import com.ignitrondev.auralis.domain.model.ArtistRef
import com.ignitrondev.auralis.domain.model.MusicTrack

/** Converts between domain tracks and Media3 items. The item URI is resolved by the service. */
internal object MediaItems {
    const val SCHEME = "auralis"

    fun toMediaItem(track: MusicTrack): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.Builder().scheme(SCHEME).authority(track.sourceId).appendPath(track.id).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artistNames.ifBlank { null })
                    .setAlbumTitle(track.album?.title)
                    .setArtworkUri(track.artworkUri?.let(Uri::parse))
                    .setDurationMs(track.durationMs)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build(),
            )
            .build()

    /** Rebuilds a track from an item when it was not started from this process (for example after restore). */
    fun toTrack(item: MediaItem): MusicTrack {
        val metadata = item.mediaMetadata
        return MusicTrack(
            id = item.mediaId,
            sourceId = item.localConfiguration?.uri?.authority ?: "youtube",
            title = metadata.title?.toString().orEmpty(),
            artists = metadata.artist?.toString()?.let { listOf(ArtistRef(null, it)) }.orEmpty(),
            album = metadata.albumTitle?.toString()?.let { AlbumRef(null, it) },
            durationMs = metadata.durationMs,
            artworkUri = metadata.artworkUri?.toString(),
        )
    }
}
