/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ignitrondev.auralis.core.database.dao.RecentSearchDao
import com.ignitrondev.auralis.core.database.entity.RecentSearchEntity

/**
 * Local data that belongs to the user (spec section 11). Catalog content is not stored here yet;
 * the account stays the source of truth for account data. Session material never goes here.
 */
@Database(
    entities = [RecentSearchEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AuralisDatabase : RoomDatabase() {
    abstract fun recentSearchDao(): RecentSearchDao
}
