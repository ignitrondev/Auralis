/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.di

import android.content.Context
import com.ignitrondev.auralis.domain.update.AppInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The installed version, read from the package so it matches what the release workflow built. */
@Module
@InstallIn(SingletonComponent::class)
object AppInfoModule {
    @Provides
    @Singleton
    fun provideAppInfo(@ApplicationContext context: Context): AppInfo =
        AppInfo(versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty())
}
