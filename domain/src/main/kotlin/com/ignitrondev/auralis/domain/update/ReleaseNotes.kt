/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.update

/** One line of a release's notes: a change, or a heading that groups the changes after it. */
data class ReleaseNote(val text: String, val isHeading: Boolean = false)

/**
 * The lines of a release's notes, as plain text for display.
 *
 * Notes are Markdown written by people (a tag message) or by GitHub (generated from pull
 * requests). Headings are kept as headings ("Added", "Fixed"); list markers and emphasis are
 * removed; "Full Changelog" links, empty lines and GitHub's generic "What's Changed" heading are
 * dropped; and GitHub's " by @user in https://…/pull/12" suffix is cut. What is left reads as a
 * list of changes. The default notes the release workflow used to write ("Beta release of
 * Auralis.") count as no notes.
 */
fun parseReleaseNotes(body: String?): List<ReleaseNote> {
    if (body.isNullOrBlank()) return emptyList()
    return body.lineSequence()
        .map { it.trim() }
        .filterNot { it.isEmpty() || it.startsWith("**Full Changelog**", ignoreCase = true) }
        .mapNotNull { line ->
            if (line.startsWith("#")) {
                val heading = clean(line.trimStart('#'))
                ReleaseNote(heading, isHeading = true).takeIf { heading.isNotEmpty() && !heading.equals("What's Changed", ignoreCase = true) }
            } else {
                val text = clean(line.removePrefix("- ").removePrefix("* ").removePrefix("• ").replace(AUTHOR_SUFFIX, ""))
                ReleaseNote(text).takeIf { text.isNotEmpty() && text !in DEFAULT_NOTES }
            }
        }
        .toList()
}

private fun clean(text: String) = text.trim().replace("**", "").replace("__", "").replace("`", "")

private val AUTHOR_SUFFIX = Regex("""\s+by @\S+ in https?://\S+$""")
private val DEFAULT_NOTES = setOf("Beta release of Auralis.", "Auralis release.")
