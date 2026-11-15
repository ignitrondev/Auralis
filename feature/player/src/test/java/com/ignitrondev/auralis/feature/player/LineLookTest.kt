/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineLookTest {
    @Test
    fun theLineBeingSungIsTheBrightestAndLargest() {
        for (expand in listOf(0f, 0.5f, 1f)) {
            val now = lineLook(0, expand, synced = true)
            for (d in listOf(-3, -2, -1, 1, 2, 3)) {
                val other = lineLook(d, expand, synced = true)
                assertTrue("alpha at $d/$expand", now.alpha >= other.alpha)
                assertTrue("size at $d/$expand", now.sizeSp >= other.sizeSp)
            }
        }
    }

    @Test
    fun liveFadesWithDistanceAndFullScreenKeepsUpcomingLinesBrighter() {
        assertTrue(lineLook(1, 0f, true).alpha > lineLook(2, 0f, true).alpha)
        assertTrue(lineLook(2, 0f, true).alpha > lineLook(3, 0f, true).alpha)
        assertTrue(lineLook(1, 1f, true).alpha > lineLook(-1, 1f, true).alpha)
    }

    @Test
    fun theLookBlendsContinuouslyBetweenLiveAndFullScreen() {
        val live = lineLook(2, 0f, true)
        val mid = lineLook(2, 0.5f, true)
        val full = lineLook(2, 1f, true)
        assertTrue(mid.alpha in minOf(live.alpha, full.alpha)..maxOf(live.alpha, full.alpha))
        assertTrue(mid.sizeSp in minOf(live.sizeSp, full.sizeSp)..maxOf(live.sizeSp, full.sizeSp))
        assertEquals(full.sizeSp, lineLook(2, 1f, true).sizeSp, 0f)
    }

    @Test
    fun unsyncedLyricsAreAllTheSame() {
        assertEquals(lineLook(0, 0f, false), lineLook(5, 0f, false))
        assertEquals(lineLook(-4, 1f, false), lineLook(3, 1f, false))
    }
}
