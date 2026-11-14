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
 * History needs a signed-in session, so there is no recorded response in the repo: that would
 * be someone's real listening history. The fixture is synthetic, built on the documented
 * `FEmusic_history` shape (one `musicShelfRenderer` per period), with made-up IDs.
 */
class HistoryParserTest {

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

    @Test(expected = UnexpectedResponseException::class)
    fun aPageWithoutTheExpectedLayoutIsReported() {
        HistoryParser.parse(InnerTubeSession.json.parseToJsonElement("""{"contents":{}}""").jsonObject)
    }
}
