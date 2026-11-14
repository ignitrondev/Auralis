/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.domain.update.AppRelease
import com.ignitrondev.auralis.domain.update.AppUpdates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Gives the app shell the single player controller (mini player and track taps) and whether an
 * update is available (the dot on the Settings tab). The update check runs on launch, at most
 * every few hours.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    val player: PlayerController,
    updates: AppUpdates,
) : ViewModel() {
    val availableUpdate: StateFlow<AppRelease?> = updates.available

    init {
        viewModelScope.launch { updates.checkIfDue() }
    }
}
