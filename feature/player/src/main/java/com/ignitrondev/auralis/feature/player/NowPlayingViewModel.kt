/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.model.Lyrics
import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.domain.player.PlayerState
import com.ignitrondev.auralis.domain.player.RepeatMode
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import com.ignitrondev.auralis.domain.repository.LyricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The like button of the current track. Shown only when signed in and once the account's status
 * is known, so it never looks like a control that does nothing.
 *
 * @property failed The last change was rejected; the button is back to the account's state.
 */
data class LikeUiState(
    val trackId: String? = null,
    val liked: Boolean? = null,
    val pending: Boolean = false,
    val failed: Boolean = false,
) {
    val visible: Boolean get() = trackId != null && liked != null
}

/** The lyrics panel of Now Playing. */
sealed interface LyricsUiState {
    data object Loading : LyricsUiState

    /** The source has no lyrics for this track. */
    data object Unavailable : LyricsUiState

    data object Failed : LyricsUiState

    data class Shown(val lyrics: Lyrics) : LyricsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val player: PlayerController,
    auth: AuthRepository,
    private val library: LibraryRepository,
    private val lyricsRepository: LyricsRepository,
) : ViewModel() {
    val state: StateFlow<PlayerState> = player.state

    private val _like = MutableStateFlow(LikeUiState())
    val like: StateFlow<LikeUiState> = _like.asStateFlow()
    private val likeRefreshes = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                auth.state,
                player.state.map { it.currentTrack?.id }.distinctUntilChanged(),
                likeRefreshes,
            ) { account, trackId, refresh ->
                trackId.takeIf { account is AuthState.SignedIn } to refresh
            }.distinctUntilChanged().collectLatest { (trackId, _) ->
                _like.value = LikeUiState(trackId = trackId)
                if (trackId == null) return@collectLatest
                val liked = try {
                    library.isTrackLiked(trackId)
                } catch (error: CatalogException) {
                    null // Unknown: the button stays hidden rather than guessing.
                }
                _like.update { if (it.trackId == trackId) it.copy(liked = liked) else it }
            }
        }
    }

    private val _showLyrics = MutableStateFlow(false)

    /** Whether the lyrics panel replaces the artwork. Kept while Now Playing is closed and reopened. */
    val showLyrics: StateFlow<Boolean> = _showLyrics.asStateFlow()

    private val lyricsRetries = MutableStateFlow(0)

    /**
     * Lyrics of the current track, loaded only while the panel is shown: the lyrics page is a
     * large response, so it is not fetched for every track that plays.
     */
    val lyrics: StateFlow<LyricsUiState> =
        combine(_showLyrics, player.state.map { it.currentTrack?.id }.distinctUntilChanged(), lyricsRetries) { shown, trackId, _ ->
            trackId.takeIf { shown }
        }.flatMapLatest<String?, LyricsUiState> { trackId ->
            if (trackId == null) {
                flowOf(LyricsUiState.Loading)
            } else {
                flow {
                    emit(LyricsUiState.Loading)
                    emit(
                        try {
                            lyricsRepository.lyrics(trackId)?.let(LyricsUiState::Shown) ?: LyricsUiState.Unavailable
                        } catch (error: CatalogException) {
                            LyricsUiState.Failed
                        },
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LyricsUiState.Loading)

    fun toggleLyrics() = _showLyrics.update { !it }

    fun retryLyrics() = lyricsRetries.update { it + 1 }

    /** Reads the like status from the account again, for example when Now Playing opens. */
    fun refreshLike() = likeRefreshes.update { it + 1 }

    /** Changes the like right away and reverts it if the account rejects the change. */
    fun toggleLike() {
        val current = _like.value
        val trackId = current.trackId ?: return
        val liked = current.liked ?: return
        if (current.pending) return
        _like.value = current.copy(liked = !liked, pending = true, failed = false)
        viewModelScope.launch {
            val failed = try {
                library.setTrackLiked(trackId, !liked)
                false
            } catch (error: CatalogException) {
                true
            }
            _like.update { state ->
                when {
                    state.trackId != trackId -> state
                    failed -> state.copy(liked = liked, pending = false, failed = true)
                    else -> state.copy(pending = false)
                }
            }
        }
    }

    fun togglePlayPause() = player.togglePlayPause()

    fun next() = player.next()

    fun previous() = player.previous()

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun toggleShuffle() = player.setShuffle(!state.value.shuffleEnabled)

    /** Off, then all, then one, then off again. */
    fun cycleRepeat() = player.setRepeatMode(
        when (state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        },
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
