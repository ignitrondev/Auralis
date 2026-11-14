/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.library

import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.model.HistorySection
import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.LibraryCollection
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.Page
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeAuth : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.Restoring)
        override suspend fun signOut() = Unit
    }

    private class FakeLibrary : LibraryRepository {
        var calls = 0
        var failNext = false
        var albumsFailure: CatalogException.Reason? = null
        override suspend fun collection(collection: LibraryCollection, pageToken: String?): Page<CatalogItem> {
            if (collection == LibraryCollection.ALBUMS) albumsFailure?.let { throw CatalogException(it, "albums") }
            return Page(listOf(MusicAlbum("MPRE1", "youtube", "Manusia", emptyList(), "2022", null)), nextPageToken = null)
        }
        override suspend fun isTrackLiked(trackId: String) = error("unused")
        override suspend fun setTrackLiked(trackId: String, liked: Boolean) = error("unused")
        override suspend fun setAlbumSaved(libraryPlaylistId: String, saved: Boolean) = error("unused")
        override suspend fun history(): ListeningHistory {
            calls++
            if (failNext) {
                failNext = false
                throw CatalogException(CatalogException.Reason.Network, "offline")
            }
            return ListeningHistory(listOf(HistorySection("Today", emptyList())))
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsOnlyWhenSignedInAndAsksToSignInOtherwise() = runTest(dispatcher) {
        val auth = FakeAuth()
        val library = FakeLibrary()
        val viewModel = LibraryViewModel(auth, library)
        backgroundScope.launch { viewModel.state.collect {} }

        advanceUntilIdle()
        assertEquals(CatalogUiState.Loading, viewModel.state.value)
        assertEquals(0, library.calls)

        auth.state.value = AuthState.SignedOut
        advanceUntilIdle()
        assertEquals(CatalogUiState.Error(CatalogException.Reason.SignInRequired), viewModel.state.value)
        assertEquals(0, library.calls)

        auth.state.value = AuthState.SignedIn("Listener", null, null)
        advanceUntilIdle()
        assertEquals("Today", (viewModel.state.value as CatalogUiState.Content).data.history.sections.single().title)
        assertEquals(1, library.calls)

        auth.state.value = AuthState.SessionExpired
        advanceUntilIdle()
        assertEquals(CatalogUiState.Error(CatalogException.Reason.SignInRequired), viewModel.state.value)
    }

    @Test
    fun retryLoadsAgainAfterAnError() = runTest(dispatcher) {
        val auth = FakeAuth().apply { state.value = AuthState.SignedIn("Listener", null, null) }
        val library = FakeLibrary().apply { failNext = true }
        val viewModel = LibraryViewModel(auth, library)
        backgroundScope.launch { viewModel.state.collect {} }

        advanceUntilIdle()
        assertEquals(CatalogUiState.Error(CatalogException.Reason.Network), viewModel.state.value)

        viewModel.retry()
        advanceUntilIdle()
        assertEquals(2, library.calls)
        assertEquals(CatalogUiState.Content::class, viewModel.state.value::class)
    }

    @Test
    fun albumsThatFailToLoadAreLeftOutButARejectedSessionIsNot() = runTest(dispatcher) {
        val auth = FakeAuth().apply { state.value = AuthState.SignedIn("Listener", null, null) }
        val library = FakeLibrary().apply { albumsFailure = CatalogException.Reason.Network }
        val viewModel = LibraryViewModel(auth, library)
        backgroundScope.launch { viewModel.state.collect {} }

        advanceUntilIdle()
        val content = (viewModel.state.value as CatalogUiState.Content).data
        assertEquals("only the failed collection is left out", emptyList<CatalogItem>(), content.items(LibraryCollection.ALBUMS))
        assertEquals(1, content.items(LibraryCollection.PLAYLISTS).size)
        assertEquals("Today", content.history.sections.single().title)

        library.albumsFailure = CatalogException.Reason.SignInRequired
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(CatalogUiState.Error(CatalogException.Reason.SignInRequired), viewModel.state.value)
    }
}
