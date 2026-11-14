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
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Serializable
data object SignInRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) = navigate(SettingsRoute, navOptions)

fun NavController.navigateToSignIn() = navigate(SignInRoute)

fun NavGraphBuilder.settingsScreen(onSignIn: () -> Unit, onSignInDone: () -> Unit) {
    composable<SettingsRoute> { SettingsScreen(onSignIn = onSignIn) }
    composable<SignInRoute> { SignInScreen(onDone = onSignInDone) }
}
