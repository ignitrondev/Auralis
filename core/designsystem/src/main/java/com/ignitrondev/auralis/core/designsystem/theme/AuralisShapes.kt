/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Corner radius tokens. Every surface, card, button, sheet and artwork is rounded
 * (spec section 8.3). Feature modules never hardcode a corner radius.
 */
@Immutable
data class AuralisShapes(
    /** Small artwork thumbnails, chips. */
    val small: RoundedCornerShape,
    /** List artwork, small cards. */
    val medium: RoundedCornerShape,
    /** Cards and large artwork. */
    val large: RoundedCornerShape,
    /** Sheets and the Now Playing artwork. */
    val extraLarge: RoundedCornerShape,
    /** Fully rounded ends: navigation bar, mini player, buttons, search bar. */
    val capsule: RoundedCornerShape,
)

internal val DefaultAuralisShapes = AuralisShapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
    capsule = RoundedCornerShape(percent = 50),
)

internal val LocalAuralisShapes = staticCompositionLocalOf { DefaultAuralisShapes }
