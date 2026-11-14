/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun heading(text: String) = composeRule.onNode(isHeading() and hasText(text))

    private fun tab(text: String) = composeRule.onNode(hasText(text) and !isHeading())

    @Test
    fun startsOnHome() {
        heading("Home").assertExists()
    }

    @Test
    fun bottomBarSwitchesBetweenAllTopLevelDestinations() {
        listOf("Search", "Library", "Settings", "Home").forEach { name ->
            tab(name).performClick()
            heading(name).assertExists()
            tab(name).assertIsSelected()
        }
    }
}
