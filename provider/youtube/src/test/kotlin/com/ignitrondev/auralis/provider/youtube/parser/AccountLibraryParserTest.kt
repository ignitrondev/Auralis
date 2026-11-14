/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube.parser

import com.ignitrondev.auralis.provider.youtube.InnerTubeSession
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val albums = LibraryAlbumsParser.parse(fixture("saved_albums_synthetic"))

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

    @Test
    fun likeStatusIsReadFromTheEntityWhoseKeyNamesTheTrack(): Unit {
        // The key is a base64 protobuf: field 1 (length 11) holds the video ID.
        fun key(videoId: String) = java.util.Base64.getEncoder().encodeToString(byteArrayOf(0x0a, 0x0b) + videoId.toByteArray() + byteArrayOf(0x20, 0x01))
        val root = InnerTubeSession.json.parseToJsonElement(
            """
            {"playerOverlays": {"playerOverlayRenderer": {}},
             "frameworkUpdates": {"entityBatchUpdate": {"mutations": [
               {"payload": {"likeStatusEntity": {"key": "${key("otherTrack1")}", "likeStatus": "INDIFFERENT"}}},
               {"payload": {"likeStatusEntity": {"key": "${key("song00000A1")}", "likeStatus": "LIKE"}}}
             ]}}}
            """.trimIndent(),
        ).jsonObject
        assertTrue(LikeStatusParser.isLiked("song00000A1", root))
        assertFalse(LikeStatusParser.isLiked("otherTrack1", root))
    }

    @Test(expected = UnexpectedResponseException::class)
    fun aPageWithoutTheExpectedLayoutIsReported() {
        HistoryParser.parse(InnerTubeSession.json.parseToJsonElement("""{"contents":{}}""").jsonObject)
    }
}
