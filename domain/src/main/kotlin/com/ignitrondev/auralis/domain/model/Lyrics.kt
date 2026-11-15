/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/**
 * Lyrics of a track.
 *
 * @property source Who provides them, as the service credits it (for example "Source: Musixmatch").
 */
data class Lyrics(val lines: List<LyricLine>, val source: String?) {
    /** Every line has a start time, so the current line can follow playback. */
    val isSynced: Boolean get() = lines.isNotEmpty() && lines.all { it.startMs != null }

    /**
     * The line being sung at [positionMs]: the last line that has started. -1 before the first
     * line, or when the lyrics are not synced.
     */
    fun activeLineIndex(positionMs: Long): Int {
        if (!isSynced) return -1
        return lines.indexOfLast { (it.startMs ?: Long.MAX_VALUE) <= positionMs }
    }
}

/** One line of lyrics. [startMs] is null when the lyrics have no timing. Blank lines are pauses. */
data class LyricLine(val text: String, val startMs: Long?)
