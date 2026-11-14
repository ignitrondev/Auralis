/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube.parser

import com.ignitrondev.auralis.domain.model.AlbumRef
import com.ignitrondev.auralis.domain.model.ArtistRef
import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.MusicArtist
import com.ignitrondev.auralis.domain.model.MusicPlaylist
import com.ignitrondev.auralis.domain.model.MusicTrack
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal const val SOURCE_ID = "youtube"

/** Page types InnerTube puts on browse endpoints. Only these become catalog items. */
internal object PageType {
    const val ALBUM = "MUSIC_PAGE_TYPE_ALBUM"
    const val ARTIST = "MUSIC_PAGE_TYPE_ARTIST"
    const val PLAYLIST = "MUSIC_PAGE_TYPE_PLAYLIST"
}

/** InnerTube music video types. ATV is an audio track ("song"); the rest are videos. */
private const val VIDEO_TYPE_SONG = "MUSIC_VIDEO_TYPE_ATV"

private const val SEPARATOR = " • "

internal data class BrowseTarget(val id: String, val pageType: String?)

internal fun JsonElement?.browseTarget(): BrowseTarget? {
    val endpoint = obj("browseEndpoint") ?: return null
    val id = endpoint.str("browseId") ?: return null
    val pageType = endpoint.path(
        "browseEndpointContextSupportedConfigs",
        "browseEndpointContextMusicConfig",
        "pageType",
    ).let { (it as? JsonPrimitive)?.content }
    return BrowseTarget(id, pageType)
}

internal data class WatchTarget(val videoId: String, val musicVideoType: String?)

internal fun JsonElement?.watchTarget(): WatchTarget? {
    val endpoint = obj("watchEndpoint") ?: return null
    val videoId = endpoint.str("videoId") ?: return null
    val type = endpoint.path("watchEndpointMusicSupportedConfigs", "watchEndpointMusicConfig", "musicVideoType")
    return WatchTarget(videoId, (type as? JsonPrimitive)?.content)
}

/** Largest thumbnail URL in a thumbnail list, wherever it sits in the renderer. */
internal fun JsonElement?.largestThumbnail(): String? {
    val list = this.path("musicThumbnailRenderer", "thumbnail").arr("thumbnails")
        ?: this.obj("thumbnail").arr("thumbnails")
        ?: this.arr("thumbnails")
    return list.objects()
        .maxByOrNull { (it.str("width")?.toIntOrNull() ?: 0) * (it.str("height")?.toIntOrNull() ?: 0) }
        ?.str("url")
        ?.let(::upscaleArtwork)
}

/**
 * Google image URLs carry their size in the path ("=w120-h120-…"). Ask for a larger square so
 * artwork stays sharp on large surfaces. Other URLs are returned unchanged.
 */
internal fun upscaleArtwork(url: String): String {
    val sized = Regex("=w\\d+-h\\d+")
    return if ("googleusercontent.com" in url || "ggpht.com" in url) {
        if (sized.containsMatchIn(url)) url.replace(sized, "=w544-h544") else url
    } else {
        url
    }
}

/** "4:04" or "1:02:03" to milliseconds. */
internal fun parseDuration(text: String?): Long? {
    if (text == null) return null
    val parts = text.trim().split(':').map { it.toLongOrNull() ?: return null }
    if (parts.isEmpty() || parts.size > 3) return null
    return parts.fold(0L) { total, part -> total * 60 + part } * 1000
}

private val durationPattern = Regex("^\\d{1,2}(:\\d{2}){1,2}$")

/** Splits subtitle runs into groups separated by " • ". */
internal fun List<JsonObject>.groupsBySeparator(): List<List<JsonObject>> {
    val groups = mutableListOf(mutableListOf<JsonObject>())
    for (run in this) {
        if (run.str("text") == SEPARATOR) groups += mutableListOf<JsonObject>() else groups.last() += run
    }
    return groups.filter { it.isNotEmpty() }
}

/** Artist links in runs; plain-text names (such as "Various Artists") are kept without an id. */
internal fun List<JsonObject>.artistRefs(): List<ArtistRef> =
    filter { it.str("text").let { text -> text != null && text != " & " && text != ", " && text != SEPARATOR } }
        .mapNotNull { run ->
            val name = run.str("text") ?: return@mapNotNull null
            val target = run.obj("navigationEndpoint").browseTarget()
            ArtistRef(id = target?.id?.takeIf { target.pageType == PageType.ARTIST }, name = name)
        }

private fun List<JsonObject>.artistRunsOnly(): List<JsonObject> =
    filter { it.obj("navigationEndpoint").browseTarget()?.pageType == PageType.ARTIST }

private fun JsonObject.hasExplicitBadge(): Boolean =
    arr("badges").objects().any { badge ->
        badge.path("musicInlineBadgeRenderer", "icon").str("iconType") == "MUSIC_EXPLICIT_BADGE"
    }

/** Converts a `musicResponsiveListItemRenderer` (search rows, album and playlist tracks). */
internal fun JsonObject.responsiveListItem(
    defaultArtwork: String? = null,
    defaultAlbum: AlbumRef? = null,
    defaultArtists: List<ArtistRef> = emptyList(),
): CatalogItem? {
    val columns = arr("flexColumns").objects().map { it.path("musicResponsiveListItemFlexColumnRenderer", "text") }
    val titleRun = columns.firstOrNull().runs().firstOrNull()
    val title = columns.firstOrNull().text() ?: return null
    val artwork = obj("thumbnail").largestThumbnail() ?: defaultArtwork
    val explicit = hasExplicitBadge()

    obj("navigationEndpoint").browseTarget()?.let { target ->
        val details = columns.getOrNull(1).runs()
        val groups = details.groupsBySeparator()
        return when (target.pageType) {
            PageType.ALBUM -> MusicAlbum(
                id = target.id,
                sourceId = SOURCE_ID,
                title = title,
                artists = details.artistRunsOnly().artistRefs().ifEmpty { groups.getOrNull(1).orEmpty().artistRefs() },
                year = groups.lastOrNull()?.singleOrNull()?.str("text")?.takeIf { it.length == 4 && it.all(Char::isDigit) },
                artworkUri = artwork,
                isExplicit = explicit,
            )
            PageType.ARTIST -> MusicArtist(
                id = target.id,
                sourceId = SOURCE_ID,
                name = title,
                artworkUri = artwork,
                subtitle = groups.drop(1).joinToString(SEPARATOR) { group -> group.joinToString("") { it.str("text").orEmpty() } }.ifBlank { null },
            )
            PageType.PLAYLIST -> MusicPlaylist(
                id = target.id,
                sourceId = SOURCE_ID,
                title = title,
                author = groups.getOrNull(1)?.joinToString("") { it.str("text").orEmpty() },
                artworkUri = artwork,
                subtitle = groups.drop(2).joinToString(SEPARATOR) { group -> group.joinToString("") { it.str("text").orEmpty() } }.ifBlank { null },
            )
            else -> null
        }
    }

    // A title that links to a page (podcast episodes, non-music audio) is not a track.
    if (titleRun.obj("navigationEndpoint").browseTarget() != null) return null
    val watch = titleRun.obj("navigationEndpoint").watchTarget()
    val videoId = path("playlistItemData").str("videoId") ?: watch?.videoId ?: return null
    // Rows without a music video type (podcast episodes) are outside the Auralis catalog.
    val videoType = watch?.musicVideoType
    if (watch != null && videoType == null) return null

    val detailRuns = columns.drop(1).flatMap { it.runs() }
    val linkedArtists = detailRuns.artistRunsOnly().artistRefs()
    val albumRun = detailRuns.firstOrNull { it.obj("navigationEndpoint").browseTarget()?.pageType == PageType.ALBUM }
    val duration = arr("fixedColumns").objects()
        .firstNotNullOfOrNull { parseDuration(it.path("musicResponsiveListItemFixedColumnRenderer", "text").text()) }
        ?: detailRuns.lastOrNull { run -> run.str("text")?.matches(durationPattern) == true }?.let { parseDuration(it.str("text")) }
    val artists = linkedArtists.ifEmpty {
        // Search rows put the artist in the second group: "Song • Artist • …".
        columns.getOrNull(1).runs().groupsBySeparator().getOrNull(1).orEmpty().artistRefs()
    }.ifEmpty { defaultArtists }

    return MusicTrack(
        id = videoId,
        sourceId = SOURCE_ID,
        title = title,
        artists = artists,
        album = albumRun?.let { AlbumRef(it.obj("navigationEndpoint").browseTarget()?.id, it.str("text").orEmpty()) } ?: defaultAlbum,
        durationMs = duration,
        artworkUri = artwork,
        isVideo = videoType != null && videoType != VIDEO_TYPE_SONG,
        isExplicit = explicit,
    )
}

/** Converts a `musicTwoRowItemRenderer` (carousel cards on home and artist pages). */
internal fun JsonObject.twoRowItem(): CatalogItem? {
    val title = obj("title").text() ?: return null
    val subtitleRuns = obj("subtitle").runs()
    val groups = subtitleRuns.groupsBySeparator()
    val artwork = obj("thumbnailRenderer").largestThumbnail()
    val explicit = arr("subtitleBadges").objects().any {
        it.path("musicInlineBadgeRenderer", "icon").str("iconType") == "MUSIC_EXPLICIT_BADGE"
    }

    obj("navigationEndpoint").browseTarget()?.let { target ->
        return when (target.pageType) {
            PageType.ALBUM -> MusicAlbum(
                id = target.id,
                sourceId = SOURCE_ID,
                title = title,
                artists = subtitleRuns.artistRunsOnly().artistRefs(),
                year = groups.lastOrNull()?.singleOrNull()?.str("text")?.takeIf { it.length == 4 && it.all(Char::isDigit) },
                artworkUri = artwork,
                isExplicit = explicit,
            )
            PageType.ARTIST -> MusicArtist(
                id = target.id,
                sourceId = SOURCE_ID,
                name = title,
                artworkUri = artwork,
                subtitle = obj("subtitle").text(),
            )
            PageType.PLAYLIST -> MusicPlaylist(
                id = target.id,
                sourceId = SOURCE_ID,
                title = title,
                author = subtitleRuns.artistRefs().firstOrNull()?.name,
                artworkUri = artwork,
                subtitle = obj("subtitle").text(),
            )
            else -> null
        }
    }

    val watch = obj("navigationEndpoint").watchTarget() ?: return null
    if (watch.musicVideoType == null) return null
    return MusicTrack(
        id = watch.videoId,
        sourceId = SOURCE_ID,
        title = title,
        artists = subtitleRuns.artistRunsOnly().artistRefs(),
        album = null,
        durationMs = null,
        artworkUri = artwork,
        isVideo = watch.musicVideoType != VIDEO_TYPE_SONG,
        isExplicit = explicit,
    )
}

/** Any list or card renderer inside a shelf. */
internal fun JsonObject.shelfItem(): CatalogItem? =
    renderer("musicResponsiveListItemRenderer")?.responsiveListItem()
        ?: renderer("musicTwoRowItemRenderer")?.twoRowItem()
