/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.library

import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.Page
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedAlbumsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun album(id: String) = MusicAlbum(id, "youtube", id, emptyList(), null, null)

    private class Pages : LibraryRepository {
        val requested = mutableListOf<String?>()
        var failNext = false
        val pages = mapOf<String?, Page<MusicAlbum>>(
            null to Page(listOf("a", "b").map { MusicAlbum(it, "youtube", it, emptyList(), null, null) }, "p2"),
            "p2" to Page(listOf("b", "c").map { MusicAlbum(it, "youtube", it, emptyList(), null, null) }, null),
        )
        override suspend fun savedAlbums(pageToken: String?): Page<MusicAlbum> {
            requested += pageToken
            if (failNext) {
                failNext = false
                throw CatalogException(CatalogException.Reason.Network, "offline")
            }
            return pages.getValue(pageToken)
        }
        override suspend fun history(): ListeningHistory = error("unused")
        override suspend fun isTrackLiked(trackId: String) = error("unused")
        override suspend fun setTrackLiked(trackId: String, liked: Boolean) = error("unused")
        override suspend fun setAlbumSaved(libraryPlaylistId: String, saved: Boolean) = error("unused")
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun SavedAlbumsViewModel.data() = (state.value as CatalogUiState.Content).data

    @Test
    fun loadsPagesUntilTheLastAndKeepsAlbumsWhenAPageFails() = runTest(dispatcher) {
        val repository = Pages()
        val viewModel = SavedAlbumsViewModel(repository)
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), viewModel.data().albums.map { it.id })
        assertTrue(viewModel.data().canLoadMore)

        repository.failNext = true
        viewModel.loadMore()
        advanceUntilIdle()
        assertTrue(viewModel.data().loadMoreFailed)
        assertEquals("albums loaded so far stay", listOf("a", "b"), viewModel.data().albums.map { it.id })

        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals("an album on both pages shows once", listOf("a", "b", "c"), viewModel.data().albums.map { it.id })
        assertNull(viewModel.data().nextPageToken)
        assertFalse(viewModel.data().canLoadMore)

        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals("nothing is requested after the last page", listOf(null, "p2", "p2"), repository.requested)
    }

    @Test
    fun aFailedFirstPageIsAnErrorThatRetryRecovers() = runTest(dispatcher) {
        val repository = Pages().apply { failNext = true }
        val viewModel = SavedAlbumsViewModel(repository)
        advanceUntilIdle()
        assertEquals(CatalogUiState.Error(CatalogException.Reason.Network), viewModel.state.value)
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(2, viewModel.data().albums.size)
    }
}
