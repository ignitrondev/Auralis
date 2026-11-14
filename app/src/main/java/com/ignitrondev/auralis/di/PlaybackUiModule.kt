/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ignitrondev.auralis.MainActivity
import com.ignitrondev.auralis.playback.PlaybackUi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PlaybackUiModule {
    /** Tapping the media notification brings back the app. */
    @Provides
    fun providePlaybackUi(@ApplicationContext context: Context): PlaybackUi = PlaybackUi {
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
