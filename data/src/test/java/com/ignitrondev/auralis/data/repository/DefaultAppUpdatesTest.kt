/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.domain.update.AppInfo
import com.ignitrondev.auralis.domain.update.AppRelease
import com.ignitrondev.auralis.domain.update.AppReleaseRepository
import com.ignitrondev.auralis.domain.update.AppVersion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class DefaultAppUpdatesTest {
    private fun release(tag: String) =
        AppRelease(AppVersion.parse(tag)!!, "https://example.com/$tag", "https://example.com/$tag.apk")

    private class MemoryStore : UpdateCheckStore {
        override var lastCheckedAtMs = 0L
        override var saved: AppRelease? = null
    }

    private class Releases(var tags: List<AppRelease>, var fail: Boolean = false) : AppReleaseRepository {
        var calls = 0
        override suspend fun releases(): List<AppRelease> {
            calls++
            if (fail) throw IOException("offline")
            return tags
        }
    }

    @Test
    fun checksOnLaunchAtMostEveryIntervalAndRemembersTheUpdate() = runTest {
        var now = 1_000_000L
        val store = MemoryStore()
        val releases = Releases(listOf(release("v2.0.0-beta.4")))
        val updates = DefaultAppUpdates(AppInfo("2.0.0-beta.3"), releases, store) { now }

        updates.checkIfDue()
        assertEquals(AppVersion.parse("2.0.0-beta.4"), updates.available.value?.version)

        now += DefaultAppUpdates.CHECK_INTERVAL_MS - 1
        updates.checkIfDue()
        assertEquals("not due yet", 1, releases.calls)

        // A new launch, offline: the saved result still shows the dot.
        releases.fail = true
        val relaunched = DefaultAppUpdates(AppInfo("2.0.0-beta.3"), releases, store) { now }
        assertEquals(AppVersion.parse("2.0.0-beta.4"), relaunched.available.value?.version)
        now += 2
        relaunched.checkIfDue()
        assertEquals("a failed background check keeps what is known", AppVersion.parse("2.0.0-beta.4"), relaunched.available.value?.version)
    }

    @Test
    fun theDotGoesOnceThatVersionIsInstalled() = runTest {
        val store = MemoryStore().apply { saved = AppRelease(AppVersion.parse("2.0.0-beta.4")!!, "page", "apk") }
        val updated = DefaultAppUpdates(AppInfo("2.0.0-beta.4"), Releases(emptyList()), store) { 0L }
        assertNull(updated.available.value)
    }

    @Test
    fun aClockSetBackDoesNotDelayTheNextCheck() = runTest {
        var now = 5 * DefaultAppUpdates.CHECK_INTERVAL_MS
        val releases = Releases(emptyList())
        val updates = DefaultAppUpdates(AppInfo("2.0.0-beta.3"), releases, MemoryStore()) { now }
        updates.checkIfDue()
        now -= DefaultAppUpdates.CHECK_INTERVAL_MS
        updates.checkIfDue()
        assertEquals(2, releases.calls)
    }

    @Test(expected = IOException::class)
    fun aCheckTheUserAsksForReportsFailure() = runTest {
        DefaultAppUpdates(AppInfo("2.0.0-beta.3"), Releases(emptyList(), fail = true), MemoryStore()) { 0L }.checkNow()
    }

    @Test
    fun aCheckThatFindsNothingClearsAnOldResult() = runTest {
        val store = MemoryStore().apply { saved = AppRelease(AppVersion.parse("2.0.0-beta.4")!!, "page", "apk") }
        val updates = DefaultAppUpdates(AppInfo("2.0.0-beta.3"), Releases(listOf(release("v2.0.0-beta.3"))), store) { 0L }
        assertNull(updates.checkNow())
        assertNull(updates.available.value)
        assertNull(store.saved)
    }
}
