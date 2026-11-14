/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {
    @Test
    fun idleHasNothingLoaded() {
        val state = PlayerState.Idle

        assertNull(state.currentTrack)
        assertFalse(state.isPlaying)
        assertTrue(state.queue.isEmpty())
        assertEquals(-1, state.currentIndex)
        assertEquals(0L, state.positionMs)
        assertEquals(RepeatMode.OFF, state.repeatMode)
        assertFalse(state.shuffleEnabled)
    }
}
