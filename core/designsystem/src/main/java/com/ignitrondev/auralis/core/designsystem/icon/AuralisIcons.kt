/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** The single source of icons for Auralis. Feature modules use these instead of Icons.* directly. */
object AuralisIcons {
    val Home: ImageVector = Icons.Rounded.Home
    val Search: ImageVector = Icons.Rounded.Search
    val Library: ImageVector = Icons.AutoMirrored.Rounded.List
    val Settings: ImageVector = Icons.Rounded.Settings
}
