/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/** A titled row of items, such as "Quick picks" on the home feed or "Albums" on an artist page. */
data class CatalogSection(
    val title: String?,
    val items: List<CatalogItem>,
)

/**
 * The discovery feed shown on Home.
 *
 * @property moods The mood filters the source offers (for example "Relax"), in its order. Empty
 * when the source offers none, and in a feed that is already filtered.
 */
data class HomeFeed(
    val sections: List<CatalogSection>,
    val moods: List<Mood> = emptyList(),
)

/**
 * A mood filter for the home feed. [id] is opaque: only the source that issued it understands it,
 * and it is passed back to [com.ignitrondev.auralis.domain.repository.CatalogRepository.home].
 */
data class Mood(val id: String, val label: String)

/**
 * Search results grouped by type (spec section 9).
 *
 * @property topResult The single best match, if the source highlights one.
 * @property tracks Songs first, then videos.
 */
data class SearchResults(
    val query: String,
    val topResult: CatalogItem?,
    val tracks: List<MusicTrack>,
    val albums: List<MusicAlbum>,
    val artists: List<MusicArtist>,
    val playlists: List<MusicPlaylist>,
) {
    val isEmpty: Boolean
        get() = topResult == null && tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

/**
 * @property trackCountText Track count and length as shown by the source, for example "22 songs".
 */
/**
 * @property libraryPlaylistId The album's playlist, which is what the account saves when the album
 * is added to the library. Null when the service does not give one.
 * @property isSavedToLibrary Whether the signed-in account has saved the album. Always false signed out.
 */
data class AlbumDetail(
    val album: MusicAlbum,
    val tracks: List<MusicTrack>,
    val description: String?,
    val trackCountText: String?,
    val libraryPlaylistId: String? = null,
    val isSavedToLibrary: Boolean = false,
)

data class ArtistDetail(
    val artist: MusicArtist,
    val description: String?,
    val topTracks: List<MusicTrack>,
    val sections: List<CatalogSection>,
)

/**
 * @property hasMore True when the source has more tracks than were loaded.
 */
data class PlaylistDetail(
    val playlist: MusicPlaylist,
    val tracks: List<MusicTrack>,
    val description: String?,
    val trackCountText: String?,
    val hasMore: Boolean,
)
