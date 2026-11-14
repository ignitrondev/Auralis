/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.home

import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.CatalogSection
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.model.SearchResults
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeCatalog : CatalogRepository {
        var failNext = true
        override suspend fun home(): HomeFeed {
            if (failNext) {
                failNext = false
                throw CatalogException(CatalogException.Reason.Network, "offline")
            }
            return HomeFeed(listOf(CatalogSection("Quick picks", emptyList())))
        }
        override suspend fun search(query: String): SearchResults = error("unused")
        override suspend fun album(id: String): AlbumDetail = error("unused")
        override suspend fun artist(id: String): ArtistDetail = error("unused")
        override suspend fun playlist(id: String): PlaylistDetail = error("unused")
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsLoadingShowsErrorThenContentAfterRetry() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeCatalog())
        assertEquals(CatalogUiState.Loading, viewModel.state.value)

        advanceUntilIdle()
        assertEquals(CatalogUiState.Error(CatalogException.Reason.Network), viewModel.state.value)

        viewModel.retry()
        assertEquals(CatalogUiState.Loading, viewModel.state.value)
        advanceUntilIdle()
        val content = viewModel.state.value as CatalogUiState.Content
        assertEquals("Quick picks", content.data.sections.single().title)
    }
}
