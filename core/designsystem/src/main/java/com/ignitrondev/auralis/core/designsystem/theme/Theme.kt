/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Placeholder monochrome scheme (spec section 8.2.1). Every Material 3 color role is set to a
// neutral value, so the default baseline purple never leaks into components such as
// NavigationBar (which reads surfaceContainer and secondaryContainer). Errors are neutral too:
// the spec signals errors with icon plus text, not red. Dynamic color is intentionally not used.
// Replaced by the Liquid Glass design system tokens in Phase 2.
private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color.White,
    inversePrimary = Color.Black,
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2E2E2E),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFBDBDBD),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2E2E2E),
    onTertiaryContainer = Color.White,
    background = Color(0xFF0B0B0B),
    onBackground = Color.White,
    surface = Color(0xFF0B0B0B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceTint = Color.White,
    inverseSurface = Color(0xFFEDEDED),
    inverseOnSurface = Color(0xFF0B0B0B),
    error = Color.White,
    onError = Color.Black,
    errorContainer = Color(0xFF2E2E2E),
    onErrorContainer = Color.White,
    outline = Color(0xFF5C5C5C),
    outlineVariant = Color(0xFF2E2E2E),
    scrim = Color.Black,
    surfaceBright = Color(0xFF2A2A2A),
    surfaceDim = Color(0xFF0B0B0B),
    surfaceContainerLowest = Color(0xFF080808),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF242424),
)

private val LightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDADADA),
    onPrimaryContainer = Color.Black,
    inversePrimary = Color.White,
    secondary = Color(0xFF424242),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6D6D6),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF424242),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6D6D6),
    onTertiaryContainer = Color.Black,
    background = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    surface = Color(0xFFF5F5F5),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF424242),
    surfaceTint = Color.Black,
    inverseSurface = Color(0xFF1C1C1C),
    inverseOnSurface = Color(0xFFF5F5F5),
    error = Color.Black,
    onError = Color.White,
    errorContainer = Color(0xFFD6D6D6),
    onErrorContainer = Color.Black,
    outline = Color(0xFF9E9E9E),
    outlineVariant = Color(0xFFD0D0D0),
    scrim = Color.Black,
    surfaceBright = Color(0xFFFAFAFA),
    surfaceDim = Color(0xFFDCDCDC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F0F0),
    surfaceContainer = Color(0xFFEBEBEB),
    surfaceContainerHigh = Color(0xFFE5E5E5),
    surfaceContainerHighest = Color(0xFFDFDFDF),
)

@Composable
fun AuralisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
