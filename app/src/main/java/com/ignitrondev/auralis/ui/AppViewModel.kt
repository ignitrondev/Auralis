/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.ui

import androidx.lifecycle.ViewModel
import com.ignitrondev.auralis.domain.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Gives the app shell the single player controller (mini player and track taps). */
@HiltViewModel
class AppViewModel @Inject constructor(
    val player: PlayerController,
) : ViewModel()
