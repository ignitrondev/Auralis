/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {
    private val synced = Lyrics(
        lines = listOf(LyricLine("a", 1_000), LyricLine("", 5_000), LyricLine("b", 9_000)),
        source = null,
    )

    @Test
    fun theActiveLineIsTheLastOneThatStarted(): Unit {
        assertEquals("before the first line", -1, synced.activeLineIndex(500))
        assertEquals(0, synced.activeLineIndex(1_000))
        assertEquals(0, synced.activeLineIndex(4_999))
        assertEquals("a pause is a line too", 1, synced.activeLineIndex(6_000))
        assertEquals(2, synced.activeLineIndex(60_000))
    }

    @Test
    fun unsyncedLyricsHaveNoActiveLine(): Unit {
        val plain = Lyrics(listOf(LyricLine("a", null), LyricLine("b", null)), null)
        assertEquals(-1, plain.activeLineIndex(5_000))
        assertEquals(false, Lyrics(emptyList(), null).isSynced)
    }
}
