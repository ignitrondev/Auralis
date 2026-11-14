/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.repository

import kotlinx.coroutines.flow.Flow

/** Queries the user searched for, newest first. Stored on the device only. */
interface SearchHistoryRepository {
    fun recentSearches(): Flow<List<String>>

    /** Adds [query], or moves it to the top if it is already there. Blank queries are ignored. */
    suspend fun add(query: String)

    suspend fun remove(query: String)

    suspend fun clear()
}
