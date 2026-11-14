/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocalesTest {
    private fun hlGl(tag: String) = youTubeLocaleFor(Locale.forLanguageTag(tag)).let { it.hl to it.gl }

    @Test
    fun deviceRegionBecomesGlAndLanguageBecomesHl() {
        assertEquals("en" to "ID", hlGl("en-ID"))
        assertEquals("id" to "ID", hlGl("id-ID"))
        assertEquals("en" to "US", hlGl("en-US"))
        assertEquals("ja" to "JP", hlGl("ja-JP"))
    }

    @Test
    fun keepsRegionalLanguagesYouTubeKnows() {
        assertEquals("en-GB" to "GB", hlGl("en-GB"))
        assertEquals("pt-BR" to "BR", hlGl("pt-BR"))
        assertEquals("zh-TW" to "TW", hlGl("zh-TW"))
        assertEquals("es-419" to "MX", hlGl("es-MX"))
        assertEquals("es" to "ES", hlGl("es-ES"))
    }

    @Test
    fun legacyIndonesianCodeIsNormalized() {
        @Suppress("DEPRECATION")
        val legacy = Locale("in", "ID")
        assertEquals("id", youTubeLocaleFor(legacy).hl)
    }

    @Test
    fun localeWithoutRegionHasEmptyGl() {
        assertEquals("it" to "", hlGl("it"))
    }
}
