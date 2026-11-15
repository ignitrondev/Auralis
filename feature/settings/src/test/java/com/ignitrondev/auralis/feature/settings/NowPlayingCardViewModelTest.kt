/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.LibraryCollection
import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.model.Page
import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.domain.player.PlayerState
import com.ignitrondev.auralis.domain.player.RepeatMode
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingCardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private fun track(id: String) = MusicTrack(id, "youtube", id, emptyList(), null, null, null)

    private class FakePlayer : PlayerController {
        override val state = MutableStateFlow(PlayerState.Idle)
        override fun play(queue: List<MusicTrack>, startIndex: Int) = Unit
        override fun togglePlayPause() = Unit
        override fun next() = Unit
        override fun previous() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun skipTo(queueIndex: Int) = Unit
        override fun setShuffle(enabled: Boolean) = Unit
        override fun setRepeatMode(mode: RepeatMode) = Unit
        override fun retry() = Unit
        override fun stop() = Unit
    }

    private class FakeAuth(initial: AuthState) : AuthRepository {
        override val state = MutableStateFlow(initial)
        override suspend fun signOut() = Unit
    }

    private class FakeLibrary : LibraryRepository {
        val liked = mutableSetOf<String>()
        var write: CompletableDeferred<Unit>? = null
        override suspend fun history(): ListeningHistory = error("unused")
        override suspend fun collection(collection: LibraryCollection, pageToken: String?): Page<CatalogItem> = error("unused")
        override suspend fun isTrackLiked(trackId: String) = trackId in liked
        override suspend fun setTrackLiked(trackId: String, liked: Boolean) {
            write?.await()
            if (liked) this.liked += trackId else this.liked -= trackId
        }
        override suspend fun setAlbumSaved(libraryPlaylistId: String, saved: Boolean) = error("unused")
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theCardFollowsThePlayerAndShowsProgress() = runTest(dispatcher) {
        val player = FakePlayer()
        val viewModel = NowPlayingCardViewModel(player, FakeAuth(AuthState.SignedOut), FakeLibrary()) { null }
        backgroundScope.launch { viewModel.card.collect {} }
        advanceUntilIdle()
        assertNull("nothing playing: no card", viewModel.card.value)

        player.state.value = PlayerState.Idle.copy(
            currentTrack = track("a"), isPlaying = true, positionMs = 30_000, durationMs = 120_000,
            queue = listOf(track("a"), track("b")), currentIndex = 0,
        )
        advanceUntilIdle()
        val card = viewModel.card.value!!
        assertEquals(0.25f, card.progress, 0.001f)
        assertEquals("first of two: next, no previous", true to false, card.hasNext to card.hasPrevious)
    }

    @Test
    fun likeIsHiddenSignedOutAndRevertsWhenTheAccountRejectsIt() = runTest(dispatcher) {
        val player = FakePlayer().apply { state.value = PlayerState.Idle.copy(currentTrack = track("a")) }
        val auth = FakeAuth(AuthState.SignedOut)
        val library = FakeLibrary()
        val viewModel = NowPlayingCardViewModel(player, auth, library) { null }
        advanceUntilIdle()
        assertNull("signed out: no like button", viewModel.liked.value)

        auth.state.value = AuthState.SignedIn("Listener", null, null)
        advanceUntilIdle()
        assertEquals(false, viewModel.liked.value)

        library.write = CompletableDeferred()
        viewModel.toggleLike()
        runCurrent()
        assertEquals("shown before the account answers", true, viewModel.liked.value)
        library.write!!.completeExceptionally(CatalogException(CatalogException.Reason.Network, "offline"))
        advanceUntilIdle()
        assertEquals("a rejected like is taken back", false, viewModel.liked.value)

        library.write = null
        viewModel.toggleLike()
        advanceUntilIdle()
        assertEquals(true, viewModel.liked.value)
        assertEquals(setOf("a"), library.liked)
    }
}
