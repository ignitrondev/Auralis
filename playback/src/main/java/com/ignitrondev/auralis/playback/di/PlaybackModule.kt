/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback.di

import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.playback.MediaPlayerController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackModule {
    @Binds
    abstract fun bindPlayerController(impl: MediaPlayerController): PlayerController
}
