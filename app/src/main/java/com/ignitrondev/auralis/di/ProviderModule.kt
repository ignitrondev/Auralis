/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.di

import android.content.Context
import com.ignitrondev.auralis.domain.repository.ShareLinks
import com.ignitrondev.auralis.provider.api.AccountLibraryProvider
import com.ignitrondev.auralis.provider.api.AccountSessionProvider
import com.ignitrondev.auralis.provider.api.CatalogProvider
import com.ignitrondev.auralis.provider.api.StreamProvider
import com.ignitrondev.auralis.provider.botguard.WebViewAttestationProvider
import com.ignitrondev.auralis.provider.youtube.YouTubeMusicProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Chooses the music provider. YouTube Music is the core source (spec section 6); only this
 * module knows the concrete provider class. One instance serves the catalog and streams.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {
    @Provides
    @Singleton
    fun provideYouTubeMusic(@ApplicationContext context: Context): YouTubeMusicProvider =
        YouTubeMusicProvider(attestation = WebViewAttestationProvider(context))

    @Provides
    fun provideCatalogProvider(provider: YouTubeMusicProvider): CatalogProvider = provider

    @Provides
    fun provideStreamProvider(provider: YouTubeMusicProvider): StreamProvider = provider

    @Provides
    fun provideAccountSessionProvider(provider: YouTubeMusicProvider): AccountSessionProvider = provider

    @Provides
    fun provideAccountLibraryProvider(provider: YouTubeMusicProvider): AccountLibraryProvider = provider

    @Provides
    fun provideShareLinks(provider: YouTubeMusicProvider): ShareLinks = provider
}
