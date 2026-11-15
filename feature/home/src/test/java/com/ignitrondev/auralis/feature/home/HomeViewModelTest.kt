/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.home

import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.model.AlbumDetail
import com.ignitrondev.auralis.domain.model.ArtistDetail
import com.ignitrondev.auralis.domain.model.CatalogSection
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.Mood
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.model.PlaylistDetail
import com.ignitrondev.auralis.domain.model.SearchResults
import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.domain.player.PlayerState
import com.ignitrondev.auralis.domain.player.RepeatMode
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.CatalogRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val relax = Mood("relax-params", "Relax")
    private val focus = Mood("focus-params", "Focus")

    private class FakeCatalog(private val moods: List<Mood>) : CatalogRepository {
        var failNext = true
        val requested = mutableListOf<String?>()

        /** While set, a request waits here, so a test can look at the loading state. */
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun home(mood: String?): HomeFeed {
            requested += mood
            gate?.await()
            if (failNext) {
                failNext = false
                throw CatalogException(CatalogException.Reason.Network, "offline")
            }
            // A filtered feed has no chips of its own, like the real one.
            return HomeFeed(listOf(CatalogSection(mood ?: "Quick picks", emptyList())), if (mood == null) moods else emptyList())
        }
        override suspend fun search(query: String): SearchResults = error("unused")
        override suspend fun album(id: String): AlbumDetail = error("unused")
        override suspend fun artist(id: String): ArtistDetail = error("unused")
        override suspend fun playlist(id: String): PlaylistDetail = error("unused")
    }

    private class FakePlayer : PlayerController {
        override val state = MutableStateFlow(PlayerState.Idle)
        var toggles = 0
        override fun play(queue: List<MusicTrack>, startIndex: Int) = Unit
        override fun togglePlayPause() { toggles++ }
        override fun next() = Unit
        override fun previous() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun skipTo(queueIndex: Int) = Unit
        override fun setShuffle(enabled: Boolean) = Unit
        override fun setRepeatMode(mode: RepeatMode) = Unit
        override fun retry() = Unit
        override fun stop() = Unit
    }

    private class FakeAuth(initial: AuthState = AuthState.SignedOut) : AuthRepository {
        override val state = MutableStateFlow(initial)
        override suspend fun signOut() = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsLoadingShowsErrorThenContentAfterRetry() = runTest(dispatcher) {
        val catalog = FakeCatalog(listOf(relax, focus)).apply { gate = CompletableDeferred() }
        val viewModel = HomeViewModel(catalog, FakePlayer(), FakeAuth())
        runCurrent()
        assertEquals(CatalogUiState.Loading, viewModel.state.value.feed)

        catalog.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(CatalogUiState.Error(CatalogException.Reason.Network), viewModel.state.value.feed)

        catalog.gate = CompletableDeferred()
        viewModel.retry()
        runCurrent()
        assertEquals(CatalogUiState.Loading, viewModel.state.value.feed)
        catalog.gate!!.complete(Unit)
        advanceUntilIdle()
        val content = viewModel.state.value.feed as CatalogUiState.Content
        assertEquals("Quick picks", content.data.sections.single().title)
        assertEquals(listOf("Relax", "Focus"), viewModel.state.value.moods.map { it.label })
    }

    @Test
    fun aMoodFiltersTheFeedKeepsTheChipsAndTogglesOff() = runTest(dispatcher) {
        val catalog = FakeCatalog(listOf(relax, focus)).apply { failNext = false }
        val viewModel = HomeViewModel(catalog, FakePlayer(), FakeAuth())
        advanceUntilIdle()

        catalog.gate = CompletableDeferred()
        viewModel.selectMood(relax)
        runCurrent()
        assertEquals("the chips stay while the feed loads", 2, viewModel.state.value.moods.size)
        assertEquals(CatalogUiState.Loading, viewModel.state.value.feed)
        catalog.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(relax.id, viewModel.state.value.selectedMood)
        assertEquals("Relax filter loads its own feed", relax.id, ((viewModel.state.value.feed as CatalogUiState.Content).data.sections.single().title))
        assertEquals("filtered feeds do not replace the chips", 2, viewModel.state.value.moods.size)

        viewModel.selectMood(focus)
        advanceUntilIdle()
        assertEquals(focus.id, viewModel.state.value.selectedMood)

        viewModel.selectMood(focus)
        advanceUntilIdle()
        assertNull("choosing it again clears the mood", viewModel.state.value.selectedMood)
        assertEquals(listOf<String?>(null, relax.id, focus.id, null), catalog.requested)
    }

    @Test
    fun continueListeningFollowsThePlayerAndTheAccountShowsItsInitial() = runTest(dispatcher) {
        val player = FakePlayer()
        val auth = FakeAuth()
        val viewModel = HomeViewModel(FakeCatalog(emptyList()).apply { failNext = false }, player, auth)
        advanceUntilIdle()
        assertNull(viewModel.state.value.continueListening)
        assertNull(viewModel.state.value.accountInitial)

        val track = MusicTrack("id", "youtube", "Komang", emptyList(), null, null, null)
        player.state.value = PlayerState.Idle.copy(currentTrack = track, isPlaying = true)
        auth.state.value = AuthState.SignedIn("bima", null, null)
        advanceUntilIdle()
        assertEquals(ContinueListening(track, isPlaying = true), viewModel.state.value.continueListening)
        assertEquals("B", viewModel.state.value.accountInitial)

        viewModel.togglePlayPause()
        assertTrue(player.toggles == 1)
    }
}
