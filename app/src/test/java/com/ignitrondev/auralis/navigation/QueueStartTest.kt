/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.navigation

import com.ignitrondev.auralis.domain.model.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueStartTest {
    private fun track(id: String) = MusicTrack(id, "youtube", id, emptyList(), null, null, null)

    @Test
    fun trackInTheListPlaysFromItsPosition() {
        val songs = listOf(track("a"), track("b"), track("c"))
        assertEquals(songs to 1, queueStartingAt(track("b"), songs))
    }

    @Test
    fun trackOutsideTheListGoesFirst() {
        // The search top result is not part of the songs list.
        val songs = listOf(track("a"), track("b"))
        val (queue, index) = queueStartingAt(track("top"), songs)
        assertEquals(listOf("top", "a", "b"), queue.map { it.id })
        assertEquals(0, index)
    }
}
