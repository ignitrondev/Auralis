/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.domain.repository.CatalogException
import com.ignitrondev.auralis.domain.repository.LibraryRepository
import com.ignitrondev.auralis.domain.repository.ShareLinks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The song in the player, as the Now Playing card on Settings shows it. */
data class NowPlayingCardState(
    val track: MusicTrack,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
) {
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Drives the Now Playing card at the top of Settings. It has its own view model so the position,
 * which changes twice a second, only redraws the card and not the whole screen.
 *
 * The like button is shown only when signed in and once the account's status is known (null
 * until then), and changes the like right away, reverting it if the account rejects the change.
 */
@HiltViewModel
class NowPlayingCardViewModel @Inject constructor(
    private val player: PlayerController,
    auth: AuthRepository,
    private val library: LibraryRepository,
    private val shareLinks: ShareLinks,
) : ViewModel() {

    val card: StateFlow<NowPlayingCardState?> = player.state
        .map { state ->
            state.currentTrack?.let {
                NowPlayingCardState(
                    track = it,
                    isPlaying = state.isPlaying,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    hasNext = state.currentIndex < state.queue.lastIndex,
                    hasPrevious = state.currentIndex > 0,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _liked = MutableStateFlow<Boolean?>(null)

    /** Whether the account liked the current song; null while unknown or when signed out. */
    val liked: StateFlow<Boolean?> = _liked

    private var likedTrackId: String? = null

    init {
        viewModelScope.launch {
            combine(auth.state, player.state.map { it.currentTrack?.id }.distinctUntilChanged()) { account, trackId ->
                trackId.takeIf { account is AuthState.SignedIn }
            }.distinctUntilChanged().collectLatest { trackId ->
                _liked.value = null
                likedTrackId = trackId
                if (trackId == null) return@collectLatest
                _liked.value = try {
                    library.isTrackLiked(trackId)
                } catch (error: CatalogException) {
                    null // Unknown: the button stays hidden rather than guessing.
                }
            }
        }
    }

    fun togglePlayPause() = player.togglePlayPause()

    fun next() = player.next()

    fun previous() = player.previous()

    fun shareLink(track: MusicTrack): String? = shareLinks.link(track)

    fun toggleLike() {
        val trackId = likedTrackId ?: return
        val liked = _liked.value ?: return
        _liked.value = !liked
        viewModelScope.launch {
            try {
                library.setTrackLiked(trackId, !liked)
            } catch (error: CatalogException) {
                if (likedTrackId == trackId) _liked.value = liked
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
