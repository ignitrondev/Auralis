/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube.parser

import com.ignitrondev.auralis.provider.youtube.InnerTubeSession
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Account pages need a signed-in session, so there are no recorded responses in the repo: they
 * would be someone's real history and library. The fixtures are synthetic, built on the shapes of
 * `FEmusic_history` and `FEmusic_liked_albums`, with made-up IDs.
 */
class AccountLibraryParserTest {

    private fun fixture(name: String): JsonObject {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name.json")) { "missing fixture $name" }
        return InnerTubeSession.json.parseToJsonElement(stream.bufferedReader().readText()).jsonObject
    }

    @Test
    fun groupsTracksByPeriodAndDropsNonMusicRows(): Unit {
        val history = HistoryParser.parse(fixture("history_synthetic"))

        assertEquals("a period with only a podcast is left out", listOf("Today", "Yesterday"), history.sections.map { it.title })

        val (song, video) = history.sections[0].tracks
        assertEquals("song00000A1", song.id)
        assertEquals("Teh Hijau", song.title)
        assertEquals("Tulus", song.artistNames)
        assertEquals("UCartist0001", song.artists.single().id)
        assertEquals("Manusia", song.album?.title)
        assertEquals(212_000L, song.durationMs)
        assertFalse(song.isVideo)

        assertEquals("M83", video.artistNames)
        assertTrue(video.isVideo)
        assertEquals(244_000L, video.durationMs)

        val (uploaded, again) = history.sections[1].tracks
        assertEquals("an uploader without a link is the first group, not the views", "Some Uploader", uploaded.artistNames)
        assertEquals("the podcast episode is not a track", listOf("video0000C3", "song00000A1"), history.sections[1].tracks.map { it.id })
        assertEquals("Tulus", again.artistNames)
    }

    @Test
    fun savedAlbumsComeFromTheGridAndOnlyAlbumsAreKept(): Unit {
        val page = LibraryAlbumsParser.parse(fixture("saved_albums_synthetic"))
        val albums = page.items
        assertEquals("PAGE2TOKEN", page.nextPageToken)

        assertEquals(listOf("MPREalbum001", "MPREalbum002"), albums.map { it.id })
        val manusia = albums[0]
        assertEquals("Manusia", manusia.title)
        assertEquals("Tulus", manusia.artistNames)
        assertEquals("2022", manusia.year)
        assertEquals("https://lh3.googleusercontent.com/album=w226-h226-l90-rj".substringBefore("=w"), manusia.artworkUri?.substringBefore("=w"))
        assertTrue(albums[1].isExplicit)
    }

    @Test
    fun likeStatusIsReadFromTheButtonForThatTrack(): Unit {
        // Other tracks' buttons (for example in the queue) must not be mistaken for the current one.
        val root = InnerTubeSession.json.parseToJsonElement(
            """
            {"contents": {"queue": [{"likeButtonRenderer": {"target": {"videoId": "other"}, "likeStatus": "LIKE"}}]},
             "playerOverlays": {"playerOverlayRenderer": {"actions": [
               {"likeButtonRenderer": {"target": {"videoId": "song00000A1"}, "likeStatus": "INDIFFERENT"}}
             ]}}}
            """.trimIndent(),
        ).jsonObject
        assertFalse(LikeStatusParser.isLiked("song00000A1", root))
        assertTrue(LikeStatusParser.isLiked("other", root))
    }

    /** The shape YouTube Music web sends today, reduced to the fields the parser reads. */
    private fun viewModelResponse(trackId: String, overlayStatus: String, updatedStatus: String?) =
        InnerTubeSession.json.parseToJsonElement(
            """
            {"playerOverlays": {"playerOverlayRenderer": {
               "segmentedLikeDislikeButtonViewModel": {"likeButtonViewModel": {"likeButtonViewModel": {
                 "likeStatusEntityKey": "EgtzMDAwMDAwMDAwMSA%3D",
                 "likeStatusEntity": {"key": "EgtzMDAwMDAwMDAwMSA%3D", "likeStatus": "$overlayStatus"},
                 "toggleButtonViewModel": {"toggleButtonViewModel": {"defaultButtonViewModel": {"buttonViewModel": {
                   "onTap": {"serialCommand": {"commands": [{"innertubeCommand": {"likeEndpoint": {"status": "LIKE", "target": {"videoId": "$trackId"}}}}]}}
                 }}}}
               }}}}},
             "frameworkUpdates": {"entityBatchUpdate": {"mutations": [
               {"payload": {"likeStatusEntity": {"key": "otherKey", "likeStatus": "LIKE"}}}
               ${updatedStatus?.let { ", {\"payload\": {\"likeStatusEntity\": {\"key\": \"EgtzMDAwMDAwMDAwMSA%3D\", \"likeStatus\": \"$it\"}}}" }.orEmpty()}
             ]}}}
            """.trimIndent(),
        ).jsonObject

    @Test
    fun likeStatusIsReadFromThePlayerOverlayViewModel(): Unit {
        assertTrue(LikeStatusParser.isLiked("song00000A1", viewModelResponse("song00000A1", "LIKE", updatedStatus = null)))
        assertFalse(LikeStatusParser.isLiked("song00000A1", viewModelResponse("song00000A1", "INDIFFERENT", updatedStatus = null)))
        // The entity update for the same key is newer than the overlay's copy; another key is ignored.
        assertTrue(LikeStatusParser.isLiked("song00000A1", viewModelResponse("song00000A1", "INDIFFERENT", updatedStatus = "LIKE")))
    }

    @Test(expected = UnexpectedResponseException::class)
    fun aViewModelForAnotherTrackIsNotUsed() {
        LikeStatusParser.isLiked("song00000A1", viewModelResponse("otherTrack1", "LIKE", updatedStatus = null))
    }

    private fun albumCard(id: String) =
        """{"musicTwoRowItemRenderer": {"title": {"runs": [{"text": "Album $id"}]},
           "navigationEndpoint": {"browseEndpoint": {"browseId": "$id", "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {"pageType": "MUSIC_PAGE_TYPE_ALBUM"}}}}}}"""

    @Test
    fun savedAlbumsContinueInTheOlderGridContinuationShape(): Unit {
        val root = InnerTubeSession.json.parseToJsonElement(
            """{"continuationContents": {"gridContinuation": {
                 "items": [${albumCard("MPREalbum003")}, ${albumCard("MPREalbum004")}],
                 "continuations": [{"nextContinuationData": {"continuation": "PAGE3TOKEN"}}]}}}""",
        ).jsonObject
        val page = LibraryAlbumsParser.parse(root)
        assertEquals(listOf("MPREalbum003", "MPREalbum004"), page.items.map { it.id })
        assertEquals("PAGE3TOKEN", page.nextPageToken)
    }

    @Test
    fun savedAlbumsContinueInTheAppendActionShapeAndTheLastPageHasNoToken(): Unit {
        val withToken = InnerTubeSession.json.parseToJsonElement(
            """{"onResponseReceivedActions": [{"appendContinuationItemsAction": {"continuationItems": [
                 ${albumCard("MPREalbum005")},
                 {"continuationItemRenderer": {"continuationEndpoint": {"continuationCommand": {"token": "PAGE4TOKEN"}}}}]}}]}""",
        ).jsonObject
        assertEquals(listOf("MPREalbum005"), LibraryAlbumsParser.parse(withToken).items.map { it.id })
        assertEquals("PAGE4TOKEN", LibraryAlbumsParser.parse(withToken).nextPageToken)

        val last = InnerTubeSession.json.parseToJsonElement(
            """{"onResponseReceivedActions": [{"appendContinuationItemsAction": {"continuationItems": [${albumCard("MPREalbum006")}]}}]}""",
        ).jsonObject
        assertNull(LibraryAlbumsParser.parse(last).nextPageToken)
    }

    @Test(expected = UnexpectedResponseException::class)
    fun aPageWithoutTheExpectedLayoutIsReported() {
        HistoryParser.parse(InnerTubeSession.json.parseToJsonElement("""{"contents":{}}""").jsonObject)
    }
}
