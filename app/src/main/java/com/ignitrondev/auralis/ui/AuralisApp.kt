/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.ignitrondev.auralis.navigation.AuralisNavHost
import com.ignitrondev.auralis.navigation.TopLevelDestination

/**
 * App shell: bottom navigation plus the navigation host.
 *
 * The plain Material NavigationBar is temporary. Phase 2 replaces it with the floating
 * GlassNavigationBar from the design system.
 */
@Composable
fun AuralisApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = currentDestination.isInHierarchy(destination),
                        onClick = { navController.navigateToTopLevel(destination) },
                        icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        AuralisNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private fun NavDestination?.isInHierarchy(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

/**
 * Switches tabs without growing the back stack: pops to the start destination, keeps one copy
 * of each tab, and saves/restores each tab's state.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    val options = navOptions {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    navigate(destination.route, options)
}
