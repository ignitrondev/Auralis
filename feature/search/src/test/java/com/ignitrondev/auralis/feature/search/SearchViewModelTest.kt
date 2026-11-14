/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.search

import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.model.SearchResults
import com.ignitrondev.auralis.domain.repository.CatalogRepository
import com.ignitrondev.auralis.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeCatalog : CatalogRepository {
        val searched = mutableListOf<String>()
        override suspend fun search(query: String): SearchResults {
            searched += query
            return SearchResults(query, null, emptyList(), emptyList(), emptyList(), emptyList())
        }
        override suspend fun home(): HomeFeed = error("unused")
        override suspend fun album(id: String): AlbumDetail = error("unused")
        override suspend fun artist(id: String): ArtistDetail = error("unused")
        override suspend fun playlist(id: String): PlaylistDetail = error("unused")
    }

    private class FakeHistory : SearchHistoryRepository {
        val items = MutableStateFlow<List<String>>(emptyList())
        override fun recentSearches() = items
        override suspend fun add(query: String) {
            items.value = listOf(query) + (items.value - query)
        }
        override suspend fun remove(query: String) {
            items.value = items.value - query
        }
        override suspend fun clear() {
            items.value = emptyList()
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun typingIsDebouncedAndOnlyTheLastQueryIsSearched() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val viewModel = SearchViewModel(catalog, FakeHistory())
        backgroundScope.launch { viewModel.state.collect() }

        listOf("m", "m8", "m83").forEach {
            viewModel.onQueryChange(it)
            advanceTimeBy(100)
        }
        advanceUntilIdle()

        assertEquals(listOf("m83"), catalog.searched)
        val results = viewModel.state.value.results as SearchResultsState.Query
        assertEquals("m83", results.query)
        assertTrue(results.state is CatalogUiState.Content)
    }

    @Test
    fun blankQueryShowsRecentSearchesAndSearchActionRemembersQuery() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val viewModel = SearchViewModel(catalog, FakeHistory())
        backgroundScope.launch { viewModel.state.collect() }

        viewModel.onSearch("daft punk")
        advanceUntilIdle()
        assertEquals(listOf("daft punk"), viewModel.state.value.recentSearches)

        viewModel.onQueryChange("   ")
        advanceUntilIdle()
        assertEquals(SearchResultsState.Idle, viewModel.state.value.results)
        assertEquals("blank query never hits the network", listOf("daft punk"), catalog.searched)
    }
}
