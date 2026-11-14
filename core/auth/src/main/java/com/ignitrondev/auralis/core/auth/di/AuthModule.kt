/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.auth.di

import com.ignitrondev.auralis.core.auth.SessionManager
import com.ignitrondev.auralis.domain.auth.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthModule {
    @Binds
    abstract fun bindAuthRepository(impl: SessionManager): AuthRepository
}
