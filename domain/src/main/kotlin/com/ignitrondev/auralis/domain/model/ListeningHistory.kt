/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/**
 * What the signed-in account listened to, newest first, as the service groups it
 * (for example "Today", "Yesterday", "Last week").
 */
data class ListeningHistory(val sections: List<HistorySection>) {
    val isEmpty: Boolean get() = sections.all { it.tracks.isEmpty() }
}

/** @property title The service's label for the period, or null when it has none. */
data class HistorySection(val title: String?, val tracks: List<MusicTrack>)
