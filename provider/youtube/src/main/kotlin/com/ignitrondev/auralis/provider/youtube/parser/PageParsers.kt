/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube.parser

import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.AlbumRef
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.CatalogSection
import com.ignitrondev.auralis.domain.model.HistorySection
import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.MusicArtist
import com.ignitrondev.auralis.domain.model.MusicPlaylist
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.model.SearchResults
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Thrown when a response does not have the shape a parser needs. */
internal class UnexpectedResponseException(message: String) : IllegalStateException(message)

private fun JsonObject.sectionListContents(vararg prefix: String): List<JsonObject> =
    path(*prefix).arr("tabs").objects().firstOrNull()
        .path("tabRenderer", "content", "sectionListRenderer").arr("contents").objects()

/** One shelf of a browse page to a titled section. Null for shelves with no usable items. */
private fun JsonObject.toSection(): CatalogSection? {
    renderer("musicCarouselShelfRenderer")?.let { shelf ->
        val title = shelf.path("header", "musicCarouselShelfBasicHeaderRenderer").obj("title").text()
        val items = shelf.arr("contents").objects().mapNotNull { it.shelfItem() }
        return items.takeIf { it.isNotEmpty() }?.let { CatalogSection(title, it) }
    }
    renderer("musicShelfRenderer")?.let { shelf ->
        val items = shelf.arr("contents").objects().mapNotNull { it.shelfItem() }
        return items.takeIf { it.isNotEmpty() }?.let { CatalogSection(shelf.obj("title").text(), it) }
    }
    return null
}

internal object SearchParser {
    fun parse(query: String, root: JsonObject): SearchResults {
        val contents = root.sectionListContents("contents", "tabbedSearchResultsRenderer")
        if (root.path("contents", "tabbedSearchResultsRenderer") == null) {
            throw UnexpectedResponseException("search: no tabbedSearchResultsRenderer")
        }
        var top: CatalogItem? = null
        val items = mutableListOf<CatalogItem>()
        for (section in contents) {
            section.renderer("musicCardShelfRenderer")?.let { card ->
                top = card.topResult()
                card.arr("contents").objects().mapNotNullTo(items) { it.shelfItem() }
            }
            section.renderer("musicShelfRenderer")?.arr("contents").objects().mapNotNullTo(items) { it.shelfItem() }
            section.renderer("itemSectionRenderer")?.arr("contents").objects().mapNotNullTo(items) { it.shelfItem() }
        }
        val best = top
        val unique = items.distinctBy { it::class to it.id }.filterNot { best != null && it::class == best::class && it.id == best.id }
        val tracks = unique.filterIsInstance<MusicTrack>()
        return SearchResults(
            query = query,
            topResult = best,
            tracks = tracks.filterNot { it.isVideo } + tracks.filter { it.isVideo },
            albums = unique.filterIsInstance<MusicAlbum>(),
            artists = unique.filterIsInstance<MusicArtist>(),
            playlists = unique.filterIsInstance<MusicPlaylist>(),
        )
    }

    /** The highlighted best match card at the top of search results. */
    private fun JsonObject.topResult(): CatalogItem? {
        val titleRun = obj("title").runs().firstOrNull() ?: return null
        val title = titleRun.str("text") ?: return null
        val subtitleRuns = obj("subtitle").runs()
        val artwork = obj("thumbnail").largestThumbnail()
        val endpoint = titleRun.obj("navigationEndpoint")
        endpoint.watchTarget()?.let { watch ->
            if (watch.musicVideoType == null) return null
            return MusicTrack(
                id = watch.videoId,
                sourceId = SOURCE_ID,
                title = title,
                artists = subtitleRuns.groupsBySeparator().getOrNull(1).orEmpty().artistRefs(),
                album = subtitleRuns.firstOrNull { it.obj("navigationEndpoint").browseTarget()?.pageType == PageType.ALBUM }
                    ?.let { AlbumRef(it.obj("navigationEndpoint").browseTarget()?.id, it.str("text").orEmpty()) },
                durationMs = subtitleRuns.lastOrNull()?.str("text")?.let(::parseDuration),
                artworkUri = artwork,
                isVideo = watch.musicVideoType != "MUSIC_VIDEO_TYPE_ATV",
            )
        }
        val target = endpoint.browseTarget() ?: return null
        // The card subtitle starts with a localized type label ("Artist • …"); the UI adds its own.
        val details = subtitleRuns.groupsBySeparator().drop(1)
            .joinToString(" • ") { group -> group.joinToString("") { it.str("text").orEmpty() } }
            .ifBlank { null }
        return when (target.pageType) {
            PageType.ARTIST -> MusicArtist(target.id, SOURCE_ID, title, artwork, details)
            PageType.ALBUM -> MusicAlbum(target.id, SOURCE_ID, title, subtitleRuns.groupsBySeparator().getOrNull(1).orEmpty().artistRefs(), null, artwork)
            PageType.PLAYLIST -> MusicPlaylist(target.id, SOURCE_ID, title, null, artwork, details)
            else -> null
        }
    }
}

/**
 * Parses a single-column browse page made of shelves: the home feed (`FEmusic_home`) and the
 * explore page (`FEmusic_explore`) share this shape.
 */
internal object FeedParser {
    fun parse(root: JsonObject): List<CatalogSection> {
        if (root.path("contents", "singleColumnBrowseResultsRenderer") == null) {
            throw UnexpectedResponseException("feed: no singleColumnBrowseResultsRenderer")
        }
        return root.sectionListContents("contents", "singleColumnBrowseResultsRenderer").mapNotNull { it.toSection() }
    }
}

internal object AlbumParser {
    fun parse(id: String, root: JsonObject): AlbumDetail {
        val columns = root.path("contents", "twoColumnBrowseResultsRenderer")
            ?: throw UnexpectedResponseException("album: no twoColumnBrowseResultsRenderer")
        val header = columns.path("tabs").let { (it as? JsonArray).objects().firstOrNull() }
            .path("tabRenderer", "content", "sectionListRenderer").arr("contents").objects()
            .firstNotNullOfOrNull { it.renderer("musicResponsiveHeaderRenderer") }
            ?: throw UnexpectedResponseException("album: no header")
        val title = header.obj("title").text() ?: throw UnexpectedResponseException("album: no title")
        val artists = header.obj("straplineTextOne").runs().artistRefs()
        val subtitleGroups = header.obj("subtitle").runs().groupsBySeparator()
        val year = subtitleGroups.lastOrNull()?.singleOrNull()?.str("text")?.takeIf { it.length == 4 && it.all(Char::isDigit) }
        val artwork = header.obj("thumbnail").largestThumbnail()
        val album = MusicAlbum(id, SOURCE_ID, title, artists, year, artwork)

        val shelf = columns.path("secondaryContents", "sectionListRenderer").arr("contents").objects()
            .firstNotNullOfOrNull { it.renderer("musicShelfRenderer") }
        val tracks = shelf?.arr("contents").objects().mapNotNull { item ->
            item.renderer("musicResponsiveListItemRenderer")?.responsiveListItem(
                defaultArtwork = artwork,
                defaultAlbum = AlbumRef(id, title),
                defaultArtists = artists,
            ) as? MusicTrack
        }.orEmpty()

        return AlbumDetail(
            album = album,
            tracks = tracks,
            description = header.path("description", "musicDescriptionShelfRenderer").obj("description").text(),
            trackCountText = header.obj("secondSubtitle").text(),
        )
    }
}

internal object ArtistParser {
    fun parse(id: String, root: JsonObject): ArtistDetail {
        val header = root.path("header", "musicImmersiveHeaderRenderer")
            ?: root.path("header", "musicVisualHeaderRenderer")
            ?: throw UnexpectedResponseException("artist: no header")
        val name = header.obj("title").text() ?: throw UnexpectedResponseException("artist: no name")
        val artist = MusicArtist(
            id = id,
            sourceId = SOURCE_ID,
            name = name,
            artworkUri = header.obj("thumbnail").largestThumbnail(),
            subtitle = header.obj("monthlyListenerCount").text(),
        )
        val shelves = root.sectionListContents("contents", "singleColumnBrowseResultsRenderer")
        val topTracks = shelves.firstNotNullOfOrNull { it.renderer("musicShelfRenderer") }
            ?.arr("contents").objects()
            .mapNotNull { it.renderer("musicResponsiveListItemRenderer")?.responsiveListItem() as? MusicTrack }
            .orEmpty()
        // Untitled carousels on artist pages have nothing to label them with in the UI.
        val sections = shelves.filter { it.renderer("musicCarouselShelfRenderer") != null }
            .mapNotNull { it.toSection() }
            .filter { it.title != null }
        return ArtistDetail(
            artist = artist,
            description = header.obj("description").text()
                ?: shelves.firstNotNullOfOrNull { it.renderer("musicDescriptionShelfRenderer") }?.obj("description").text(),
            topTracks = topTracks,
            sections = sections,
        )
    }
}

internal object PlaylistParser {
    fun parse(id: String, root: JsonObject): PlaylistDetail {
        val columns = root.path("contents", "twoColumnBrowseResultsRenderer")
            ?: throw UnexpectedResponseException("playlist: no twoColumnBrowseResultsRenderer")
        val header = (columns.path("tabs") as? JsonArray).objects().firstOrNull()
            .path("tabRenderer", "content", "sectionListRenderer").arr("contents").objects()
            .firstNotNullOfOrNull { it.renderer("musicResponsiveHeaderRenderer") ?: it.renderer("musicEditablePlaylistDetailHeaderRenderer")?.path("header", "musicResponsiveHeaderRenderer") as? JsonObject }
            ?: throw UnexpectedResponseException("playlist: no header")
        val title = header.obj("title").text() ?: throw UnexpectedResponseException("playlist: no title")
        val shelf = columns.path("secondaryContents", "sectionListRenderer").arr("contents").objects()
            .firstNotNullOfOrNull { it.renderer("musicPlaylistShelfRenderer") }
        val items = shelf?.arr("contents").objects()
        val tracks = items.mapNotNull { it.renderer("musicResponsiveListItemRenderer")?.responsiveListItem() as? MusicTrack }
        val playlist = MusicPlaylist(
            id = id,
            sourceId = SOURCE_ID,
            title = title,
            author = header.obj("straplineTextOne").text(),
            artworkUri = header.obj("thumbnail").largestThumbnail(),
            subtitle = header.obj("subtitle").text(),
        )
        return PlaylistDetail(
            playlist = playlist,
            tracks = tracks,
            description = header.path("description", "musicDescriptionShelfRenderer").obj("description").text(),
            trackCountText = header.obj("secondSubtitle").text(),
            hasMore = items.any { it.renderer("continuationItemRenderer") != null },
        )
    }
}

/**
 * The account's listening history (`FEmusic_history`): one `musicShelfRenderer` per period, each
 * with track rows. Rows that are not tracks (podcast episodes) are left out.
 */
internal object HistoryParser {
    fun parse(root: JsonObject): ListeningHistory {
        if (root.path("contents", "singleColumnBrowseResultsRenderer") == null) {
            throw UnexpectedResponseException("history: no singleColumnBrowseResultsRenderer")
        }
        val shelves = root.sectionListContents("contents", "singleColumnBrowseResultsRenderer")
            .mapNotNull { it.renderer("musicShelfRenderer") }
        val sections = shelves.mapNotNull { shelf ->
            val tracks = shelf.arr("contents").objects()
                .mapNotNull { it.renderer("musicResponsiveListItemRenderer")?.responsiveListItem(unlinkedArtistGroup = 0) as? MusicTrack }
            tracks.takeIf { it.isNotEmpty() }?.let { HistorySection(shelf.obj("title").text(), it) }
        }
        return ListeningHistory(sections)
    }
}
