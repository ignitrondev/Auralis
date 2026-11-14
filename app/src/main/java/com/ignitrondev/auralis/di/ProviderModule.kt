/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.di

import com.ignitrondev.auralis.provider.api.CatalogProvider
import com.ignitrondev.auralis.provider.youtube.YouTubeMusicProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Chooses the music provider. YouTube Music is the core source (spec section 6); only this
 * module knows the concrete provider class.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {
    @Provides
    @Singleton
    fun provideCatalogProvider(): CatalogProvider = YouTubeMusicProvider()
}
