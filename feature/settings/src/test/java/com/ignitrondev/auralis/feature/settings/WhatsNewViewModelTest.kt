/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import com.ignitrondev.auralis.domain.update.AppInfo
import com.ignitrondev.auralis.domain.update.AppRelease
import com.ignitrondev.auralis.domain.update.AppReleaseRepository
import com.ignitrondev.auralis.domain.update.AppVersion
import com.ignitrondev.auralis.domain.update.ReleaseNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class WhatsNewViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun release(tag: String, apk: Boolean = true, notes: List<String> = emptyList()) =
        AppRelease(AppVersion.parse(tag)!!, "https://example.com/$tag", if (apk) "https://example.com/$tag.apk" else null, notes.map { ReleaseNote(it) }, "2026-11-14T15:55:03Z")

    private class Releases(var all: List<AppRelease>, var fail: Boolean = false) : AppReleaseRepository {
        override suspend fun releases(): List<AppRelease> {
            if (fail) throw IOException("offline")
            return all
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun listsAuralisReleasesNewestFirstMarkedAgainstTheInstalledVersion() = runTest(dispatcher) {
        val repository = Releases(
            listOf(
                release("v1.0.0", apk = false),
                release("v2.0.0-beta.2", notes = listOf("First beta")),
                release("v1.1.0", apk = false),
                release("v2.0.0-beta.4", notes = listOf("Mini player grows", "Library")),
                release("v2.0.0-beta.3"),
                release("v2.0.0-beta.5", notes = listOf("Lyrics")),
            ),
        )
        val viewModel = WhatsNewViewModel(repository, AppInfo("2.0.0-beta.4"))
        advanceUntilIdle()

        val entries = (viewModel.state.value as WhatsNewState.Content).entries
        assertEquals("IgniteTunes releases have no Auralis APK and are left out", listOf("2.0.0-beta.5", "2.0.0-beta.4", "2.0.0-beta.3", "2.0.0-beta.2"), entries.map { it.version })
        assertEquals(listOf(ReleaseStatus.Newer, ReleaseStatus.Installed, ReleaseStatus.Earlier, ReleaseStatus.Earlier), entries.map { it.status })
        assertEquals(listOf("Mini player grows", "Library"), entries[1].notes.map { it.text })
        assertEquals(true, entries[0].isPreRelease)
    }

    @Test
    fun aDebugBuildSeesEveryReleaseAsNewerAndFailureCanBeRetried() = runTest(dispatcher) {
        val repository = Releases(listOf(release("v2.0.0-beta.2")), fail = true)
        val viewModel = WhatsNewViewModel(repository, AppInfo("0.1.0-debug"))
        advanceUntilIdle()
        assertEquals(WhatsNewState.Failed, viewModel.state.value)

        repository.fail = false
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(listOf(ReleaseStatus.Newer), (viewModel.state.value as WhatsNewState.Content).entries.map { it.status })
    }
}
