/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.ignitrondev.auralis.core.database.dao.PlaybackQueueDao
import com.ignitrondev.auralis.core.database.dao.RecentSearchDao
import com.ignitrondev.auralis.core.database.entity.QueueItemEntity
import com.ignitrondev.auralis.core.database.entity.QueueStateEntity
import com.ignitrondev.auralis.core.database.entity.RecentSearchEntity

/**
 * Local data that belongs to the user (spec section 11): recent searches and the last playback
 * queue. Catalog content is not stored here yet; the account stays the source of truth for account
 * data. Session material never goes here.
 */
@Database(
    entities = [RecentSearchEntity::class, QueueItemEntity::class, QueueStateEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AuralisDatabase : RoomDatabase() {
    abstract fun recentSearchDao(): RecentSearchDao

    abstract fun playbackQueueDao(): PlaybackQueueDao
}
