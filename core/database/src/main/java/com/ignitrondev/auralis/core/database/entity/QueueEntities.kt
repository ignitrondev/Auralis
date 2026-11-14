/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One track of the saved playback queue, at [position]. Artists are stored as two lists joined
 * with [LIST_SEPARATOR] so a track needs only one row.
 */
@Entity(tableName = "queue_item")
data class QueueItemEntity(
    @PrimaryKey val position: Int,
    val trackId: String,
    val sourceId: String,
    val title: String,
    val artistNames: String,
    val artistIds: String,
    val albumId: String?,
    val albumTitle: String?,
    val durationMs: Long?,
    val artworkUri: String?,
    val isVideo: Boolean,
    val isExplicit: Boolean,
) {
    companion object {
        /** ASCII unit separator: never part of a title or an ID. */
        const val LIST_SEPARATOR = '\u001F'
    }
}

/** Where playback was in the saved queue. There is only ever one row. */
@Entity(tableName = "queue_state")
data class QueueStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: String,
) {
    companion object {
        const val SINGLE_ROW = 0
    }
}
