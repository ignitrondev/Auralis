/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.ignitrondev.auralis.feature.settings.SettingsScreen
import com.ignitrondev.auralis.feature.settings.SignInScreen
import com.ignitrondev.auralis.feature.settings.WhatsNewScreen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Serializable
data object SignInRoute

/** The changelog: the notes of every Auralis release, newest first. */
@Serializable
data object WhatsNewRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) = navigate(SettingsRoute, navOptions)

fun NavController.navigateToSignIn() = navigate(SignInRoute)

fun NavController.navigateToWhatsNew() = navigate(WhatsNewRoute)

fun NavGraphBuilder.settingsScreen(onSignIn: () -> Unit, onSignInDone: () -> Unit, onOpenWhatsNew: () -> Unit, onBack: () -> Unit) {
    composable<SettingsRoute> { SettingsScreen(onSignIn = onSignIn, onOpenWhatsNew = onOpenWhatsNew) }
    composable<SignInRoute> { SignInScreen(onDone = onSignInDone) }
    composable<WhatsNewRoute> { WhatsNewScreen(onBack = onBack) }
}
