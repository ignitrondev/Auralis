/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.domain.update.AppVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubReleaseParsingTest {
    /** The fields Auralis reads from the GitHub Releases API, with made-up URLs. */
    private val body = """
        [
          {"tag_name": "v2.0.0-beta.2", "draft": false, "prerelease": true, "html_url": "https://example.com/r/beta2",
           "published_at": "2026-11-14T15:55:03Z", "body": "- First line\n- Second line",
           "assets": [{"name": "notes.txt", "browser_download_url": "https://example.com/notes.txt"},
                      {"name": "Auralis-v2.0.0-beta.2.apk", "browser_download_url": "https://example.com/beta2.apk"}]},
          {"tag_name": "v2.0.0-beta.3", "draft": true, "html_url": "https://example.com/r/draft", "assets": []},
          {"tag_name": "nightly", "draft": false, "html_url": "https://example.com/r/nightly", "assets": []},
          {"tag_name": "v1.1.0", "draft": false, "prerelease": false, "html_url": "https://example.com/r/v110",
           "assets": [{"name": "IgniteTunes-arm64.apk", "browser_download_url": "https://example.com/ignitetunes.apk"}]}
        ]
    """.trimIndent()

    @Test
    fun keepsPublishedVersionTagsAndFindsTheApk(): Unit {
        val releases = GitHubReleaseRepository.parseReleases(body)
        assertEquals(listOf(AppVersion.parse("2.0.0-beta.2"), AppVersion.parse("1.1.0")), releases.map { it.version })
        assertEquals("https://example.com/beta2.apk", releases[0].apkUrl)
        assertEquals("https://example.com/r/beta2", releases[0].pageUrl)
        assertEquals(listOf("First line", "Second line"), releases[0].notes.map { it.text })
        assertEquals("2026-11-14T15:55:03Z", releases[0].publishedAt)
        assertNull("an IgniteTunes APK is not an Auralis update", releases[1].apkUrl)
    }
}
