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

    @Test
    fun aDistanceBetweenTwoLinesBlendsTheirLooks() {
        val sung = lineLook(0, 0f, true)
        val next = lineLook(1, 0f, true)
        val half = lineLook(0.5f, 0f, true)
        assertEquals((sung.alpha + next.alpha) / 2f, half.alpha, 0.001f)
        assertEquals((sung.sizeSp + next.sizeSp) / 2f, half.sizeSp, 0.001f)
        assertEquals(lineLook(-2, 1f, true), lineLook(-2f, 1f, true))
        assertEquals(lineLook(3, 0f, false), lineLook(2.4f, 0f, false))
    }

    @Test
    fun theFocusedLineIsMeasuredFromTheTopOfTheViewportNotFromThePadding() {
        // A 1000 px viewport with 500 px of padding above the first line: the viewport starts at -500.
        // A 60 px line at the top of the content has its middle at 530 px from the top of the screen.
        assertEquals(30f, focusDelta(itemOffset = 0, itemSize = 60, viewportStart = -500, viewportEnd = 500, fraction = 0.5f), 0.001f)
        // Already centred: nothing to scroll.
        assertEquals(0f, focusDelta(itemOffset = -30, itemSize = 60, viewportStart = -500, viewportEnd = 500, fraction = 0.5f), 0.001f)
        // A line below the focus must scroll forward, one above it backward.
        assertTrue(focusDelta(200, 60, -500, 500, 0.38f) > 0f)
        assertTrue(focusDelta(-400, 60, -500, 500, 0.38f) < 0f)
    }
}
