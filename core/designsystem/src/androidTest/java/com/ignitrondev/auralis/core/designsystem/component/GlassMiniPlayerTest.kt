/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlassMiniPlayerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playPauseTogglesAndOpenAndNextCallBack() {
        var playing by mutableStateOf(false)
        var opened by mutableIntStateOf(0)
        var skipped by mutableIntStateOf(0)
        composeRule.setContent {
            AuralisTheme {
                GlassMiniPlayer(
                    title = "Midnight City",
                    artist = "M83",
                    isPlaying = playing,
                    progress = { 0.4f },
                    onPlayPauseClick = { playing = !playing },
                    onClick = { opened++ },
                    onNextClick = { skipped++ },
                )
            }
        }

        composeRule.onNodeWithText("Midnight City").assertIsDisplayed()
        composeRule.onNodeWithText("M83").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Play").performClick()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Next track").performClick()
        assertEquals(1, skipped)

        composeRule.onNodeWithText("Midnight City").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun hidesNextButtonAndArtistWhenAbsent() {
        composeRule.setContent {
            AuralisTheme {
                GlassMiniPlayer(
                    title = "Untitled",
                    artist = null,
                    isPlaying = true,
                    progress = { 0f },
                    onPlayPauseClick = {},
                    onClick = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Next track").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }
}
