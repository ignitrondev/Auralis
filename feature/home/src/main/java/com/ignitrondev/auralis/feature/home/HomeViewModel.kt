/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.catalogState
import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.model.HomeFeed
import com.ignitrondev.auralis.domain.model.Mood
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The song the player has, for the "Continue listening" card. */
data class ContinueListening(val track: MusicTrack, val isPlaying: Boolean)

/**
 * Everything Home shows. The header and the mood chips stay on screen while the feed below them
 * loads, so choosing a mood does not blank the page.
 *
 * @property accountInitial The signed-in account's initial, or null when signed out.
 */
data class HomeUiState(
    val feed: CatalogUiState<HomeFeed> = CatalogUiState.Loading,
    val moods: List<Mood> = emptyList(),
    val selectedMood: String? = null,
    val continueListening: ContinueListening? = null,
    val accountInitial: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val player: PlayerController,
    auth: AuthRepository,
) : ViewModel() {

    private val feed = MutableStateFlow<CatalogUiState<HomeFeed>>(CatalogUiState.Loading)
    private val moods = MutableStateFlow<List<Mood>>(emptyList())
    private val selectedMood = MutableStateFlow<String?>(null)
    private var loading: Job? = null

    private val playing = player.state
        .map { state -> state.currentTrack?.let { ContinueListening(it, state.isPlaying) } }
        .distinctUntilChanged()

    val state: StateFlow<HomeUiState> =
        combine(feed, moods, selectedMood, playing, auth.state) { feed, moods, selected, playing, account ->
            HomeUiState(
                feed = feed,
                moods = moods,
                selectedMood = selected,
                continueListening = playing,
                accountInitial = (account as? AuthState.SignedIn)?.name?.firstOrNull { it.isLetterOrDigit() }?.uppercase(),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    init {
        load()
    }

    fun retry() = load()

    /** Chooses [mood], or clears the choice when it is already chosen. */
    fun selectMood(mood: Mood) {
        selectedMood.update { if (it == mood.id) null else mood.id }
        load()
    }

    fun togglePlayPause() = player.togglePlayPause()

    private fun load() {
        val mood = selectedMood.value
        loading?.cancel()
        feed.value = CatalogUiState.Loading
        loading = viewModelScope.launch {
            val result = catalogState { catalog.home(mood) }
            feed.value = result
            // The chips come from the unfiltered feed and stay while a mood is chosen.
            if (mood == null && result is CatalogUiState.Content) moods.value = result.data.moods
        }
    }
}
