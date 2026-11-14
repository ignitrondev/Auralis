/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.database.di

import android.content.Context
import androidx.room.Room
import com.ignitrondev.auralis.core.database.AuralisDatabase
import com.ignitrondev.auralis.core.database.dao.PlaybackQueueDao
import com.ignitrondev.auralis.core.database.dao.RecentSearchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuralisDatabase =
        Room.databaseBuilder(context, AuralisDatabase::class.java, "auralis.db").build()

    @Provides
    fun provideRecentSearchDao(database: AuralisDatabase): RecentSearchDao = database.recentSearchDao()

    @Provides
    fun providePlaybackQueueDao(database: AuralisDatabase): PlaybackQueueDao = database.playbackQueueDao()
}
