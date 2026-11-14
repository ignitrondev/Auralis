/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import com.ignitrondev.auralis.domain.auth.AuthRepository
import com.ignitrondev.auralis.domain.auth.AuthState
import com.ignitrondev.auralis.domain.update.AppInfo
import com.ignitrondev.auralis.domain.update.AppRelease
import com.ignitrondev.auralis.domain.update.AppReleaseRepository
import com.ignitrondev.auralis.domain.update.AppVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsUpdateCheckTest {
    private val dispatcher = StandardTestDispatcher()

    private object Auth : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.SignedOut)
        override suspend fun signOut() = Unit
    }

    private class Releases(var tags: List<String>, var fail: Boolean = false) : AppReleaseRepository {
        override suspend fun releases(): List<AppRelease> {
            if (fail) throw IOException("offline")
            return tags.map { AppRelease(AppVersion.parse(it)!!, "https://example.com/$it", "https://example.com/$it.apk") }
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun reportsAnUpdateTheLatestVersionOrAFailure() = runTest(dispatcher) {
        val releases = Releases(listOf("v2.0.0-beta.2", "v2.0.0-beta.3"), fail = true)
        val viewModel = SettingsViewModel(Auth, AppInfo("2.0.0-beta.2"), releases)
        assertEquals(UpdateState.Idle, viewModel.update.value)

        viewModel.checkForUpdates()
        assertEquals(UpdateState.Checking, viewModel.update.value)
        advanceUntilIdle()
        assertEquals(UpdateState.Failed, viewModel.update.value)

        releases.fail = false
        viewModel.checkForUpdates()
        advanceUntilIdle()
        assertEquals(
            UpdateState.Available("2.0.0-beta.3", "https://example.com/v2.0.0-beta.3.apk", "https://example.com/v2.0.0-beta.3"),
            viewModel.update.value,
        )

        releases.tags = listOf("v2.0.0-beta.2")
        viewModel.checkForUpdates()
        advanceUntilIdle()
        assertEquals(UpdateState.UpToDate, viewModel.update.value)
    }
}
