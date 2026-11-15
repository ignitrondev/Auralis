/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.domain.model.Lyrics
import com.ignitrondev.auralis.domain.repository.LyricsRepository
import com.ignitrondev.auralis.provider.api.LyricsProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lyrics from the provider, kept in memory for the last few tracks: reopening the lyrics of a
 * track, or going back to the previous one, does not download them again (the response is large).
 */
@Singleton
class DefaultLyricsRepository @Inject constructor(
    private val provider: LyricsProvider,
) : LyricsRepository {

    private val lock = Mutex()
    private val cache = object : LinkedHashMap<String, Lyrics?>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Lyrics?>): Boolean = size > CACHE_SIZE
    }

    override suspend fun lyrics(trackId: String): Lyrics? {
        lock.withLock { if (cache.containsKey(trackId)) return cache[trackId] }
        val lyrics = provider.lyrics(trackId)
        lock.withLock { cache[trackId] = lyrics }
        return lyrics
    }

    private companion object {
        const val CACHE_SIZE = 8
    }
}
