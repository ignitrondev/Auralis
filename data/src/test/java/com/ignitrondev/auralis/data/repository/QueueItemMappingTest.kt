/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.domain.model.AlbumRef
import com.ignitrondev.auralis.domain.model.ArtistRef
import com.ignitrondev.auralis.domain.model.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueItemMappingTest {
    private fun track(artists: List<ArtistRef>, album: AlbumRef? = null) = MusicTrack(
        id = "abc",
        sourceId = "youtube",
        title = "Teh Hijau",
        artists = artists,
        album = album,
        durationMs = 212_000,
        artworkUri = "https://example.com/art.jpg",
        isVideo = true,
        isExplicit = true,
    )

    @Test
    fun aTrackSurvivesTheRoundTrip_includingArtistsWithoutIds(): Unit {
        val original = track(
            artists = listOf(ArtistRef("UC1", "Daft Punk"), ArtistRef(null, "Pharrell Williams"), ArtistRef("UC3", "Nile, Rodgers")),
            album = AlbumRef("MPRE1", "Random Access Memories"),
        )
        val entity = original.toEntity(position = 4)
        assertEquals(4, entity.position)
        assertEquals(original, entity.toTrack())
    }

    @Test
    fun noArtistsAndNoAlbumStayEmpty(): Unit {
        val original = track(artists = emptyList())
        assertEquals(original, original.toEntity(0).toTrack())
    }
}
