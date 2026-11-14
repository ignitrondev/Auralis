/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube

import com.metrolist.innertubex.models.YouTubeLocale
import java.util.Locale

/**
 * Language tags YouTube accepts with a region. For every other locale YouTube expects the bare
 * language: a full device tag such as "en-ID" makes the search endpoint fail with HTTP 400.
 */
private val RegionalLanguages = setOf(
    "en-GB", "en-IN", "es-419", "es-US", "fr-CA", "pt-BR", "pt-PT", "zh-CN", "zh-HK", "zh-TW", "sr-Latn",
)

/**
 * The InnerTube locale for a device locale: the device region as `gl`, and a language YouTube
 * accepts as `hl` (spec: catalog results follow the user's language and country).
 */
internal fun youTubeLocaleFor(locale: Locale): YouTubeLocale {
    val tag = locale.toLanguageTag() // Normalizes legacy codes, for example "in" to "id".
    val language = tag.substringBefore('-').ifBlank { "en" }
    val withScript = if (locale.script.isNotEmpty()) "$language-${locale.script}" else null
    val withRegion = if (locale.country.isNotEmpty()) "$language-${locale.country}" else null
    val hl = listOfNotNull(withScript, withRegion).firstOrNull { it in RegionalLanguages }
        ?: if (language == "es" && locale.country.isNotEmpty() && locale.country != "ES") "es-419" else language
    return YouTubeLocale(gl = locale.country.uppercase(Locale.ROOT), hl = hl)
}
