/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.di

import com.ignitrondev.auralis.data.repository.DefaultCatalogRepository
import com.ignitrondev.auralis.data.repository.RoomPlaybackSnapshotRepository
import com.ignitrondev.auralis.data.repository.RoomSearchHistoryRepository
import com.ignitrondev.auralis.domain.repository.CatalogRepository
import com.ignitrondev.auralis.domain.repository.PlaybackSnapshotRepository
import com.ignitrondev.auralis.domain.repository.SearchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindCatalogRepository(impl: DefaultCatalogRepository): CatalogRepository

    @Binds
    abstract fun bindSearchHistoryRepository(impl: RoomSearchHistoryRepository): SearchHistoryRepository

    @Binds
    abstract fun bindPlaybackSnapshotRepository(impl: RoomPlaybackSnapshotRepository): PlaybackSnapshotRepository
}
