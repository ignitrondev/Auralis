/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.player

enum class RepeatMode {
    /** Stop after the last track in the queue. */
    OFF,

    /** Repeat the current track. */
    ONE,

    /** Restart the queue after the last track. */
    ALL,
}
