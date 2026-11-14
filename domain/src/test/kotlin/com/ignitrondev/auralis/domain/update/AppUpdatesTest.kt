/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatesTest {
    private fun v(text: String) = requireNotNull(AppVersion.parse(text)) { text }

    private fun release(tag: String, apk: Boolean = true) =
        AppRelease(v(tag), "https://example.com/$tag", if (apk) "https://example.com/$tag.apk" else null)

    @Test
    fun versionsFollowSemVerOrder(): Unit {
        val ordered = listOf("1.0.0", "1.1.0", "2.0.0-alpha", "2.0.0-beta.1", "2.0.0-beta.2", "2.0.0-beta.10", "2.0.0-rc.1", "2.0.0", "2.0.1", "10.0.0")
        assertEquals(ordered, ordered.reversed().map(::v).sorted().map { it.toString() })
        assertEquals(v("v2.0.0-beta.2"), v("2.0.0-beta.2"))
        assertEquals("build metadata is ignored", v("2.0.0+42"), v("2.0.0"))
        assertNull(AppVersion.parse("nightly"))
        assertNull(AppVersion.parse("2.0.0-"))
    }

    @Test
    fun aBetaInstallIsOfferedTheNewestBetaOrRelease(): Unit {
        val releases = listOf(release("v1.1.0"), release("v2.0.0-beta.2"), release("v2.0.0-beta.3"))
        assertEquals(v("2.0.0-beta.3"), findUpdate(v("2.0.0-beta.2"), releases)?.version)
        assertEquals(v("2.0.0"), findUpdate(v("2.0.0-beta.3"), releases + release("v2.0.0"))?.version)
        assertNull("already the newest", findUpdate(v("2.0.0-beta.3"), releases))
    }

    @Test
    fun aReleaseInstallIsNotMovedToABeta(): Unit {
        val releases = listOf(release("v2.0.0"), release("v2.1.0-beta.1"))
        assertNull(findUpdate(v("2.0.0"), releases))
        assertEquals(v("2.0.1"), findUpdate(v("2.0.0"), releases + release("v2.0.1"))?.version)
    }

    @Test
    fun releasesWithoutAnApkAreSkipped(): Unit {
        assertNull(findUpdate(v("2.0.0-beta.2"), listOf(release("v2.0.0-beta.3", apk = false))))
        assertTrue(v("2.0.0-beta.1").isPreRelease)
    }
}
