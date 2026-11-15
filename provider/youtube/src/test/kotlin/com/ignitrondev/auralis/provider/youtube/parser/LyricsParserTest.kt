/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube.parser

import com.ignitrondev.auralis.provider.youtube.InnerTubeSession
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shapes from the live spike (LiveSpikeTest.lyrics), with made-up lines: real lyrics are
 * copyrighted, so none are stored in the repository.
 */
class LyricsParserTest {
    private fun json(text: String) = InnerTubeSession.json.parseToJsonElement(text).jsonObject

    @Test
    fun findsTheLyricsPageInTheNextResponse(): Unit {
        val next = json(
            """{"contents": {"tabs": [
                 {"tabRenderer": {"endpoint": {"browseEndpoint": {"browseId": "FEmusic_up_next"}}}},
                 {"tabRenderer": {"endpoint": {"browseEndpoint": {"browseId": "MPLYt_example"}}}}]}}""",
        )
        assertEquals("MPLYt_example", LyricsParser.lyricsBrowseId(next))
        assertNull(LyricsParser.lyricsBrowseId(json("""{"contents": {}}""")))
    }

    @Test
    fun timedLinesKeepTheirStartTimes(): Unit {
        val lyrics = LyricsParser.parse(
            json(
                """{"contents": {"elementRenderer": {"newElement": {"type": {"componentType": {"model": {"timedLyricsModel": {
                     "lyricsData": {"sourceMessage": "Source: Example", "timedLyricsData": [
                       {"lyricLine": "First line", "cueRange": {"startTimeMilliseconds": "0", "endTimeMilliseconds": "4000"}},
                       {"lyricLine": "", "cueRange": {"startTimeMilliseconds": "4000", "endTimeMilliseconds": "5000"}},
                       {"lyricLine": "Second line", "cueRange": {"startTimeMilliseconds": "16130", "endTimeMilliseconds": "20000"}}]}}}}}}}}}""",
            ),
        )!!
        assertTrue(lyrics.isSynced)
        assertEquals(listOf(0L, 4000L, 16130L), lyrics.lines.map { it.startMs })
        assertEquals("Source: Example", lyrics.source)
    }

    @Test
    fun staticLinesHaveNoTimes(): Unit {
        val lyrics = LyricsParser.parse(
            json(
                """{"contents": {"timedLyricsModel": {"lyricsData": {"staticLayout": true, "sourceMessage": "Source: Example",
                     "timedLyricsData": [{"lyricLine": "First line"}, {"lyricLine": "Second line"}]}}}}""",
            ),
        )!!
        assertFalse(lyrics.isSynced)
        assertEquals(listOf("First line", "Second line"), lyrics.lines.map { it.text })
    }

    @Test
    fun theWebShapeGivesPlainLines(): Unit {
        val lyrics = LyricsParser.parse(
            json(
                """{"contents": {"sectionListRenderer": {"contents": [{"musicDescriptionShelfRenderer": {
                     "description": {"runs": [{"text": "First line\nSecond line\n\nThird line"}]},
                     "footer": {"runs": [{"text": "Source: Example"}]}}}]}}}""",
            ),
        )!!
        assertFalse(lyrics.isSynced)
        assertEquals(listOf("First line", "Second line", "", "Third line"), lyrics.lines.map { it.text })
        assertEquals("Source: Example", lyrics.source)
    }

    @Test
    fun aPageWithoutLyricsIsNull(): Unit {
        assertNull(LyricsParser.parse(json("""{"contents": {"messageRenderer": {"text": {"runs": [{"text": "Lyrics not available"}]}}}}""")))
    }
}
