/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.CatalogSection
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.model.SearchResults
import com.ignitrondev.auralis.provider.api.CatalogProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultCatalogRepositoryTest {
    private class CountingProvider : CatalogProvider {
        var homeCalls = 0
        override val sourceId = "fake"
        override suspend fun home(): HomeFeed {
            homeCalls++
            return HomeFeed(listOf(CatalogSection("call $homeCalls", emptyList())))
        }
        override suspend fun search(query: String): SearchResults = error("unused")
        override suspend fun album(id: String): AlbumDetail = error("unused")
        override suspend fun artist(id: String): ArtistDetail = error("unused")
        override suspend fun playlist(id: String): PlaylistDetail = error("unused")
    }

    @Test
    fun homeIsCachedUntilItExpiresOrIsRefreshed() = runTest {
        val provider = CountingProvider()
        var now = 0L
        val repository = DefaultCatalogRepository(provider) { now }

        repository.home()
        now += 60_000
        repository.home()
        assertEquals("second call within 10 minutes uses the cache", 1, provider.homeCalls)

        now += 10 * 60_000
        repository.home()
        assertEquals("expired cache refetches", 2, provider.homeCalls)

        repository.refreshHome()
        assertEquals("refresh always refetches", 3, provider.homeCalls)
    }
}
