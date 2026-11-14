/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.core.database.dao.RecentSearchDao
import com.ignitrondev.auralis.core.database.entity.RecentSearchEntity
import com.ignitrondev.auralis.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomSearchHistoryRepository @Inject constructor(
    private val dao: RecentSearchDao,
) : SearchHistoryRepository {

    override fun recentSearches(): Flow<List<String>> =
        dao.observeRecent(SHOWN).map { rows -> rows.map { it.query } }

    override suspend fun add(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dao.upsert(RecentSearchEntity(trimmed, System.currentTimeMillis()))
        dao.trim(keep = KEPT)
    }

    override suspend fun remove(query: String) = dao.delete(query)

    override suspend fun clear() = dao.clear()

    private companion object {
        const val SHOWN = 10
        const val KEPT = 50
    }
}
