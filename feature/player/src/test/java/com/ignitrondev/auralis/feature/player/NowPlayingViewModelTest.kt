/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player

import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.model.ListeningHistory
import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.domain.player.PlayerState
import com.ignitrondev.auralis.domain.player.RepeatMode
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModelTest {
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

    private class FakeAuth : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.SignedIn("Listener", null, null))
        override suspend fun signOut() = Unit
    }

    private class FakeLibrary : LibraryRepository {
        val liked = mutableSetOf("liked-track")
        var nextWrite: CompletableDeferred<Unit>? = null
        val writes = mutableListOf<Pair<String, Boolean>>()
        override suspend fun history(): ListeningHistory = error("unused")
        override suspend fun collection(
            collection: com.ignitrondev.auralis.domain.model.LibraryCollection,
            pageToken: String?,
        ): com.ignitrondev.auralis.domain.model.Page<com.ignitrondev.auralis.domain.model.CatalogItem> = error("unused")
        override suspend fun isTrackLiked(trackId: String) = trackId in liked
        override suspend fun setTrackLiked(trackId: String, liked: Boolean) {
            writes += trackId to liked
            nextWrite?.await()
            if (liked) this.liked += trackId else this.liked -= trackId
        }
        override suspend fun setAlbumSaved(libraryPlaylistId: String, saved: Boolean) = error("unused")
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun followsTheCurrentTrackAndHidesWhenSignedOut() = runTest(dispatcher) {
        val player = FakePlayer()
        val auth = FakeAuth()
        val viewModel = NowPlayingViewModel(player, auth, FakeLibrary()) { null }

        player.state.value = PlayerState.Idle.copy(currentTrack = track("liked-track"))
        advanceUntilIdle()
        assertEquals(true, viewModel.like.value.liked)

        player.state.value = PlayerState.Idle.copy(currentTrack = track("other"))
        advanceUntilIdle()
        assertEquals(false, viewModel.like.value.liked)
        assertTrue(viewModel.like.value.visible)

        auth.state.value = AuthState.SignedOut
        advanceUntilIdle()
        assertFalse("no like button signed out", viewModel.like.value.visible)
    }

    @Test
    fun aLikeShowsAtOnceAndIsRevertedWhenTheAccountRejectsIt() = runTest(dispatcher) {
        val player = FakePlayer().apply { state.value = PlayerState.Idle.copy(currentTrack = track("song")) }
        val library = FakeLibrary()
        val viewModel = NowPlayingViewModel(player, FakeAuth(), library) { null }
        advanceUntilIdle()
        assertEquals(false, viewModel.like.value.liked)

        val write = CompletableDeferred<Unit>().also { library.nextWrite = it }
        viewModel.toggleLike()
        advanceUntilIdle()
        assertEquals("shown before the account answers", true, viewModel.like.value.liked)
        assertTrue(viewModel.like.value.pending)
        viewModel.toggleLike()
        assertEquals("taps while pending are ignored", 1, library.writes.size)

        write.completeExceptionally(CatalogException(CatalogException.Reason.Network, "offline"))
        advanceUntilIdle()
        assertEquals(false, viewModel.like.value.liked)
        assertTrue(viewModel.like.value.failed)

        library.nextWrite = null
        viewModel.toggleLike()
        advanceUntilIdle()
        assertEquals(true, viewModel.like.value.liked)
        assertFalse(viewModel.like.value.failed)
        assertEquals(listOf("song" to true, "song" to true), library.writes)
    }
}
