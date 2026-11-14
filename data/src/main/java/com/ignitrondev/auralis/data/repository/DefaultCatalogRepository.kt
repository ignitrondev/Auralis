/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.model.SearchResults
import com.ignitrondev.auralis.domain.repository.CatalogRepository
import com.ignitrondev.auralis.provider.api.CatalogProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Catalog backed by the music provider. Keeps the home feed in memory for a short time, so
 * switching tabs does not refetch it. Detail pages are always fresh.
 */
@Singleton
class DefaultCatalogRepository internal constructor(
    private val provider: CatalogProvider,
    private val clock: () -> Long,
) : CatalogRepository {

    @Inject
    constructor(provider: CatalogProvider) : this(provider, System::currentTimeMillis)

    private val homeLock = Mutex()
    private var cachedHome: HomeFeed? = null
    private var cachedHomeAtMs = 0L

    override suspend fun home(): HomeFeed = homeLock.withLock {
        val cached = cachedHome
        if (cached != null && clock() - cachedHomeAtMs < HOME_TTL_MS) return cached
        provider.home().also {
            cachedHome = it
            cachedHomeAtMs = clock()
        }
    }

    /** Drops the cached home feed, for pull-to-refresh. */
    suspend fun refreshHome(): HomeFeed {
        homeLock.withLock { cachedHome = null }
        return home()
    }

    override suspend fun search(query: String): SearchResults = provider.search(query)

    override suspend fun album(id: String): AlbumDetail = provider.album(id)

    override suspend fun artist(id: String): ArtistDetail = provider.artist(id)

    override suspend fun playlist(id: String): PlaylistDetail = provider.playlist(id)

    private companion object {
        const val HOME_TTL_MS = 10 * 60 * 1000L
    }
}
