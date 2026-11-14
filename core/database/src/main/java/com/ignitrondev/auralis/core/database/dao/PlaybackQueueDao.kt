/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ignitrondev.auralis.core.database.entity.QueueItemEntity
import com.ignitrondev.auralis.core.database.entity.QueueStateEntity

@Dao
interface PlaybackQueueDao {
    @Query("SELECT * FROM queue_item ORDER BY position")
    suspend fun items(): List<QueueItemEntity>

    @Query("SELECT * FROM queue_state WHERE id = ${QueueStateEntity.SINGLE_ROW}")
    suspend fun state(): QueueStateEntity?

    /** Swaps the whole saved queue in one transaction, so a crash never leaves half a queue. */
    @Transaction
    suspend fun replaceItems(items: List<QueueItemEntity>) {
        clearItems()
        insertItems(items)
    }

    @Insert
    suspend fun insertItems(items: List<QueueItemEntity>)

    @Query("DELETE FROM queue_item")
    suspend fun clearItems()

    @Upsert
    suspend fun upsertState(state: QueueStateEntity)
}
