/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.feature.home.navigation.HomeRoute
import com.ignitrondev.auralis.feature.library.navigation.LibraryRoute
import com.ignitrondev.auralis.feature.search.navigation.SearchRoute
import com.ignitrondev.auralis.feature.settings.navigation.SettingsRoute
import com.ignitrondev.auralis.feature.home.R as HomeR
import com.ignitrondev.auralis.feature.library.R as LibraryR
import com.ignitrondev.auralis.feature.search.R as SearchR
import com.ignitrondev.auralis.feature.settings.R as SettingsR

/**
 * Destinations shown in the bottom navigation bar, in display order.
 *
 * @property route The type-safe route object to navigate to.
 */
enum class TopLevelDestination(
    val icon: ImageVector,
    @param:StringRes val labelRes: Int,
    val route: Any,
) {
    HOME(AuralisIcons.Home, HomeR.string.feature_home_title, HomeRoute),
    SEARCH(AuralisIcons.Search, SearchR.string.feature_search_title, SearchRoute),
    LIBRARY(AuralisIcons.Library, LibraryR.string.feature_library_title, LibraryRoute),
    SETTINGS(AuralisIcons.Settings, SettingsR.string.feature_settings_title, SettingsRoute),
}
