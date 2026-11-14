/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Text tokens must stay readable on the ground of both themes (spec: aim for 4.5:1). */
class ColorContrastTest {

    private fun contrast(foreground: Color, background: Color): Double {
        val fg = foreground.compositeOver(background).luminance()
        val bg = background.luminance()
        val lighter = maxOf(fg, bg) + 0.05
        val darker = minOf(fg, bg) + 0.05
        return lighter / darker
    }

    private fun assertReadable(name: String, colors: AuralisColors) {
        listOf(
            "contentPrimary" to colors.contentPrimary,
            "contentSecondary" to colors.contentSecondary,
            "contentTertiary" to colors.contentTertiary,
        ).forEach { (role, color) ->
            listOf(
                "background" to colors.background,
                "surfaceSolid" to colors.surfaceSolid,
                "surfaceGlassFallback" to colors.surfaceGlassFallback.compositeOver(colors.background),
            ).forEach { (groundName, ground) ->
                val ratio = contrast(color, ground)
                assertTrue("$name $role on $groundName is $ratio:1", ratio >= 4.5)
            }
        }
    }

    @Test
    fun darkTextTokensMeetContrast() = assertReadable("dark", DarkAuralisColors)

    @Test
    fun lightTextTokensMeetContrast() = assertReadable("light", LightAuralisColors)

    @Test
    fun tokensAreMonochrome() {
        listOf(DarkAuralisColors, LightAuralisColors).forEach { colors ->
            listOf(
                colors.background, colors.surfaceSolid, colors.surfaceGlass, colors.surfaceGlassFallback,
                colors.contentPrimary, colors.contentSecondary, colors.contentTertiary, colors.contentDisabled,
                colors.selection, colors.borderSubtle, colors.borderStrong, colors.highlight, colors.shadow,
                colors.scrim, colors.artworkFallbackTop, colors.artworkFallbackBottom,
            ).forEach { color ->
                assertTrue("$color is not neutral", color.red == color.green && color.green == color.blue)
            }
        }
    }
}
