/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
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
    val Close: ImageVector = Icons.Rounded.Close
    val Back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    val Forward: ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight
    val Retry: ImageVector = Icons.Rounded.Refresh
    val Collapse: ImageVector = Icons.Rounded.KeyboardArrowDown

    /** Liked is shown by shape, never by colour (spec 8.2.1): filled when liked, outline when not. */
    val Liked: ImageVector = Icons.Rounded.Favorite
    val NotLiked: ImageVector = Icons.Rounded.FavoriteBorder

    /** Save to the library, and saved. */
    val Add: ImageVector = Icons.Rounded.Add
    val Saved: ImageVector = Icons.Rounded.Check

    /** Arrow into a tray. Not in material-icons-core, so it is drawn here. */
    val Download: ImageVector by lazy {
        roundedIcon(
            name = "Auralis.Download",
            pathData = "M16.59,9H15V4c0,-0.55 -0.45,-1 -1,-1h-4c-0.55,0 -1,0.45 -1,1v5H7.41c-0.89,0 -1.34,1.08 -0.71,1.71" +
                "l4.59,4.59c0.39,0.39 1.02,0.39 1.41,0l4.59,-4.59c0.63,-0.63 0.19,-1.71 -0.7,-1.71z" +
                "M5,19c0,0.55 0.45,1 1,1h12c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H6c-0.55,0 -1,0.45 -1,1z",
        )
    }

    /** Two rounded bars. Not in material-icons-core, so it is drawn here. */
    val Pause: ImageVector by lazy {
        roundedIcon(
            name = "Auralis.Pause",
            pathData = "M8,19c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2v10c0,1.1 0.9,2 2,2z" +
                "M14,7v10c0,1.1 0.9,2 2,2s2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2z",
        )
    }

    /** Mirror of [SkipNext]. */
    val SkipPrevious: ImageVector by lazy {
        roundedIcon(
            name = "Auralis.SkipPrevious",
            pathData = "M7,6c0.55,0 1,0.45 1,1v10c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V7c0,-0.55 0.45,-1 1,-1z" +
                "M10.66,12.82l5.77,4.07c0.66,0.47 1.58,-0.01 1.58,-0.82V7.93c0,-0.81 -0.91,-1.28 -1.58,-0.82l-5.77,4.07" +
                "c-0.57,0.4 -0.57,1.24 0,1.64z",
        )
    }

    val Shuffle: ImageVector by lazy {
        roundedIcon(
            name = "Auralis.Shuffle",
            pathData = "M10.59,9.17L6.12,4.7c-0.39,-0.39 -1.02,-0.39 -1.41,0 -0.39,0.39 -0.39,1.02 0,1.41l4.46,4.46 1.42,-1.4z" +
                "M15.35,4.85l1.19,1.19L4.7,17.88c-0.39,0.39 -0.39,1.02 0,1.41 0.39,0.39 1.02,0.39 1.41,0L17.96,7.46l1.19,1.19" +
                "c0.31,0.31 0.85,0.09 0.85,-0.36V4.5c0,-0.28 -0.22,-0.5 -0.5,-0.5h-3.79c-0.45,0 -0.67,0.54 -0.36,0.85z" +
                "M14.83,13.41l-1.41,1.41 3.13,3.13 -1.2,1.2c-0.31,0.31 -0.09,0.85 0.36,0.85h3.79c0.28,0 0.5,-0.22 0.5,-0.5" +
                "v-3.79c0,-0.45 -0.54,-0.67 -0.85,-0.35l-1.19,1.19 -3.13,-3.14z",
        )
    }

    val Repeat: ImageVector by lazy {
        roundedIcon(
            name = "Auralis.Repeat",
            pathData = "M7,7h10v1.79c0,0.45 0.54,0.67 0.85,0.35l2.79,-2.79c0.2,-0.2 0.2,-0.51 0,-0.71l-2.79,-2.79" +
                "c-0.31,-0.31 -0.85,-0.09 -0.85,0.36V5H6c-0.55,0 -1,0.45 -1,1v4c0,0.55 0.45,1 1,1s1,-0.45 1,-1V7z" +
                "M17,17H7v-1.79c0,-0.45 -0.54,-0.67 -0.85,-0.35l-2.79,2.79c-0.2,0.2 -0.2,0.51 0,0.71l2.79,2.79" +
                "c0.31,0.31 0.85,0.09 0.85,-0.36V19h11c0.55,0 1,-0.45 1,-1v-4c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v3z",
        )
    }

    val RepeatOne: ImageVector by lazy {
        roundedIcon(
            name = "Auralis.RepeatOne",
            pathData = "M7,7h10v1.79c0,0.45 0.54,0.67 0.85,0.35l2.79,-2.79c0.2,-0.2 0.2,-0.51 0,-0.71l-2.79,-2.79" +
                "c-0.31,-0.31 -0.85,-0.09 -0.85,0.36V5H6c-0.55,0 -1,0.45 -1,1v4c0,0.55 0.45,1 1,1s1,-0.45 1,-1V7z" +
                "M17,17H7v-1.79c0,-0.45 -0.54,-0.67 -0.85,-0.35l-2.79,2.79c-0.2,0.2 -0.2,0.51 0,0.71l2.79,2.79" +
                "c0.31,0.31 0.85,0.09 0.85,-0.36V19h11c0.55,0 1,-0.45 1,-1v-4c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v3z" +
                "M13,14.25V9.81c0,-0.45 -0.36,-0.81 -0.81,-0.81 -0.13,0 -0.25,0.03 -0.36,0.09l-1.42,0.71" +
                "c-0.25,0.12 -0.41,0.38 -0.41,0.66 0,0.41 0.33,0.74 0.74,0.74h0.76v3.05c0,0.41 0.34,0.75 0.75,0.75s0.75,-0.34 0.75,-0.75z",
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
