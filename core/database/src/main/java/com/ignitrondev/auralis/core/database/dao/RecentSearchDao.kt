/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ignitrondev.auralis.core.database.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_search ORDER BY searchedAtMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentSearchEntity>>

    @Upsert
    suspend fun upsert(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_search WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM recent_search")
    suspend fun clear()

    /** Keeps only the newest [keep] rows, so the table cannot grow without bound. */
    @Query("DELETE FROM recent_search WHERE query NOT IN (SELECT query FROM recent_search ORDER BY searchedAtMs DESC LIMIT :keep)")
    suspend fun trim(keep: Int)
}
