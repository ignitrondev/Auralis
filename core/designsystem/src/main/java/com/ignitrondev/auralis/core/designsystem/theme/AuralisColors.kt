/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Monochrome color tokens, named by role (spec section 8.2.1).
 *
 * Dark theme uses white at different opacities over near-black; light theme uses black at
 * different opacities over a slightly gray ground. There is no accent color: album artwork is
 * the only source of color, and it lives behind the glass, never on controls.
 * Switching theme swaps the token values; feature code never changes.
 */
@Immutable
data class AuralisColors(
    /** True for the dark token set. */
    val isDark: Boolean,
    /** The ground behind every screen. */
    val background: Color,
    /** Opaque surface for content that must not show what is behind it. */
    val surfaceSolid: Color,
    /** Tint laid over the blurred backdrop of a glass surface. */
    val surfaceGlass: Color,
    /** Glass fill when backdrop blur is unavailable. Opaque enough to keep text readable. */
    val surfaceGlassFallback: Color,
    /** Main text and icons. */
    val contentPrimary: Color,
    /** Supporting text such as artist names. */
    val contentSecondary: Color,
    /** Low-emphasis text such as timestamps and captions. */
    val contentTertiary: Color,
    /** Disabled text and icons. */
    val contentDisabled: Color,
    /** Fill behind a selected item, such as the active navigation tab. */
    val selection: Color,
    /** Hairline edge of glass surfaces and dividers. */
    val borderSubtle: Color,
    /** Stronger edge, for focus and emphasis. */
    val borderStrong: Color,
    /** Soft sheen at the top-left of glass surfaces. */
    val highlight: Color,
    /** Shadow under floating glass. */
    val shadow: Color,
    /** Layer under glass that keeps text readable over busy artwork. */
    val scrim: Color,
    /** Top of the neutral gradient shown when there is no artwork. */
    val artworkFallbackTop: Color,
    /** Bottom of the neutral gradient shown when there is no artwork. */
    val artworkFallbackBottom: Color,
)

internal val DarkAuralisColors = AuralisColors(
    isDark = true,
    background = Color(0xFF0B0B0B),
    surfaceSolid = Color(0xFF161616),
    surfaceGlass = Color.White.copy(alpha = 0.08f),
    surfaceGlassFallback = Color(0xE61A1A1A),
    contentPrimary = Color.White,
    contentSecondary = Color.White.copy(alpha = 0.70f),
    contentTertiary = Color.White.copy(alpha = 0.50f),
    contentDisabled = Color.White.copy(alpha = 0.38f),
    selection = Color.White.copy(alpha = 0.14f),
    borderSubtle = Color.White.copy(alpha = 0.12f),
    borderStrong = Color.White.copy(alpha = 0.24f),
    highlight = Color.White.copy(alpha = 0.16f),
    shadow = Color.Black.copy(alpha = 0.40f),
    scrim = Color.Black.copy(alpha = 0.50f),
    artworkFallbackTop = Color(0xFF2A2A2A),
    artworkFallbackBottom = Color(0xFF121212),
)

/**
 * Light glass is tuned separately, not inverted: white glass on white has no edge, so light
 * surfaces sit on a slightly gray ground and rely on a thin dark border and a soft shadow.
 */
internal val LightAuralisColors = AuralisColors(
    isDark = false,
    background = Color(0xFFF2F2F2),
    surfaceSolid = Color.White,
    surfaceGlass = Color.White.copy(alpha = 0.60f),
    surfaceGlassFallback = Color(0xF2FFFFFF),
    contentPrimary = Color.Black,
    contentSecondary = Color.Black.copy(alpha = 0.64f),
    contentTertiary = Color.Black.copy(alpha = 0.55f),
    contentDisabled = Color.Black.copy(alpha = 0.32f),
    selection = Color.Black.copy(alpha = 0.08f),
    borderSubtle = Color.Black.copy(alpha = 0.10f),
    borderStrong = Color.Black.copy(alpha = 0.20f),
    highlight = Color.White.copy(alpha = 0.70f),
    shadow = Color.Black.copy(alpha = 0.12f),
    scrim = Color.White.copy(alpha = 0.60f),
    artworkFallbackTop = Color(0xFFE0E0E0),
    artworkFallbackBottom = Color(0xFFC8C8C8),
)

internal val LocalAuralisColors = staticCompositionLocalOf { DarkAuralisColors }
