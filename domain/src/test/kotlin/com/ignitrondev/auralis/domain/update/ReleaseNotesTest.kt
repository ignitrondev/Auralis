/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTest {
    private fun h(text: String) = ReleaseNote(text, isHeading = true)

    private fun n(text: String) = ReleaseNote(text)

    @Test
    fun aTagMessageWithSectionsKeepsItsHeadings(): Unit {
        val body = """
            ### Added
            - Share the current song as a story card
            - Show lyrics in Now Playing

            ### Fixed
            * Smooth Now Playing transitions
        """.trimIndent()
        assertEquals(
            listOf(h("Added"), n("Share the current song as a story card"), n("Show lyrics in Now Playing"), h("Fixed"), n("Smooth Now Playing transitions")),
            parseReleaseNotes(body),
        )
    }

    @Test
    fun githubGeneratedNotesAreCleanedUp(): Unit {
        val body = """
            ## What's Changed
            * feat: show lyrics in Now Playing by @bimaadam in https://github.com/ignitrondev/Auralis/pull/44
            * fix: smooth **Now Playing** by @bimaadam in https://github.com/ignitrondev/Auralis/pull/46

            **Full Changelog**: https://github.com/ignitrondev/Auralis/compare/v2.0.0-beta.4...v2.0.0-beta.5
        """.trimIndent()
        assertEquals(listOf(n("feat: show lyrics in Now Playing"), n("fix: smooth Now Playing")), parseReleaseNotes(body))
    }

    @Test
    fun emptyAndDefaultNotesAreNoNotes(): Unit {
        assertEquals(emptyList<ReleaseNote>(), parseReleaseNotes(null))
        assertEquals(emptyList<ReleaseNote>(), parseReleaseNotes("  \n "))
        assertEquals(emptyList<ReleaseNote>(), parseReleaseNotes("Beta release of Auralis."))
        assertEquals(listOf(n("A")), parseReleaseNotes("Auralis release.\n- A"))
    }
}
