/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One past search query. The query is the key, so repeating a search only moves it up. */
@Entity(tableName = "recent_search")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAtMs: Long,
)
