/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The single source of icons for Auralis. Feature modules use these instead of Icons.* directly. */
object AuralisIcons {
    val Home: ImageVector = Icons.Rounded.Home
    val Search: ImageVector = Icons.Rounded.Search
    val Library: ImageVector = Icons.AutoMirrored.Rounded.List
    val Settings: ImageVector = Icons.Rounded.Settings
    val Play: ImageVector = Icons.Rounded.PlayArrow

    /** Two rounded bars. Not in material-icons-core, so it is drawn here. */
    val Pause: ImageVector by lazy {
        roundedIcon(
            name = "Auralis.Pause",
            pathData = "M8,19c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2v10c0,1.1 0.9,2 2,2z" +
                "M14,7v10c0,1.1 0.9,2 2,2s2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2z",
        )
    }

    /** Rounded triangle and bar. Not in material-icons-core, so it is drawn here. */
    val SkipNext: ImageVector by lazy {
        roundedIcon(
            name = "Auralis.SkipNext",
            pathData = "M7.58,16.89l5.77,-4.07c0.56,-0.4 0.56,-1.24 0,-1.63L7.58,7.11" +
                "C6.91,6.65 6,7.12 6,7.93v8.14c0,0.81 0.91,1.28 1.58,0.82z" +
                "M16,7v10c0,0.55 0.45,1 1,1s1,-0.45 1,-1V7c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1z",
        )
    }
}

private fun roundedIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
