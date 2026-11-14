/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ignitrondev.auralis.domain.model.AlbumRef
import com.ignitrondev.auralis.domain.model.ArtistRef
import com.ignitrondev.auralis.domain.model.MusicTrack

/**
 * Converts between domain tracks and Media3 items. The item URI is resolved by the service.
 * Fields Media3 has no slot for travel in the metadata extras, so a track survives the round trip.
 */
internal object MediaItems {
    const val SCHEME = "auralis"

    private const val EXTRA_ARTIST_NAMES = "auralis.artistNames"
    private const val EXTRA_ARTIST_IDS = "auralis.artistIds"
    private const val EXTRA_ALBUM_ID = "auralis.albumId"
    private const val EXTRA_IS_VIDEO = "auralis.isVideo"
    private const val EXTRA_IS_EXPLICIT = "auralis.isExplicit"

    fun toMediaItem(track: MusicTrack): MediaItem {
        val extras = Bundle().apply {
            putStringArrayList(EXTRA_ARTIST_NAMES, ArrayList(track.artists.map { it.name }))
            putStringArrayList(EXTRA_ARTIST_IDS, ArrayList(track.artists.map { it.id.orEmpty() }))
            putString(EXTRA_ALBUM_ID, track.album?.id)
            putBoolean(EXTRA_IS_VIDEO, track.isVideo)
            putBoolean(EXTRA_IS_EXPLICIT, track.isExplicit)
        }
        return MediaItem.Builder()
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
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    /** Rebuilds a track from an item when it was not started from this process (for example after restore). */
    fun toTrack(item: MediaItem): MusicTrack {
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        val names = extras?.getStringArrayList(EXTRA_ARTIST_NAMES)
        val ids = extras?.getStringArrayList(EXTRA_ARTIST_IDS).orEmpty()
        val artists = names?.mapIndexed { index, name -> ArtistRef(ids.getOrNull(index)?.ifEmpty { null }, name) }
            ?: metadata.artist?.toString()?.let { listOf(ArtistRef(null, it)) }.orEmpty()
        return MusicTrack(
            id = item.mediaId,
            sourceId = item.localConfiguration?.uri?.authority ?: item.requestMetadata.mediaUri?.authority ?: "youtube",
            title = metadata.title?.toString().orEmpty(),
            artists = artists,
            album = metadata.albumTitle?.toString()?.let { AlbumRef(extras?.getString(EXTRA_ALBUM_ID), it) },
            durationMs = metadata.durationMs,
            artworkUri = metadata.artworkUri?.toString(),
            isVideo = extras?.getBoolean(EXTRA_IS_VIDEO) ?: false,
            isExplicit = extras?.getBoolean(EXTRA_IS_EXPLICIT) ?: false,
        )
    }
}
