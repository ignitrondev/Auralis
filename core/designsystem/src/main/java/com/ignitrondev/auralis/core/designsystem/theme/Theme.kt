/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Root theme for Auralis. Provides the Auralis tokens and a Material 3 theme built from them,
 * so Material components also stay neutral. Dynamic (wallpaper) color is never used.
 *
 * @param themeMode Dark by default. Settings will let the user pick Light or System.
 */
@Composable
fun AuralisTheme(
    themeMode: ThemeMode = ThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkAuralisColors else LightAuralisColors
    val reducedMotion = rememberReducedMotion()
    val motion = remember(reducedMotion) { AuralisMotion(reducedMotion) }
    val colorScheme = remember(colors) { colors.toMaterialColorScheme() }

    CompositionLocalProvider(
        LocalAuralisColors provides colors,
        LocalAuralisTypography provides DefaultAuralisTypography,
        LocalAuralisShapes provides DefaultAuralisShapes,
        LocalAuralisSpacing provides AuralisSpacing(),
        LocalAuralisMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DefaultAuralisTypography.toMaterialTypography(),
            shapes = DefaultAuralisShapes.toMaterialShapes(),
            content = content,
        )
    }
}

/** Access to the current Auralis tokens, for example `AuralisTheme.colors.contentPrimary`. */
object AuralisTheme {
    val colors: AuralisColors
        @Composable @ReadOnlyComposable get() = LocalAuralisColors.current
    val typography: AuralisTypography
        @Composable @ReadOnlyComposable get() = LocalAuralisTypography.current
    val shapes: AuralisShapes
        @Composable @ReadOnlyComposable get() = LocalAuralisShapes.current
    val spacing: AuralisSpacing
        @Composable @ReadOnlyComposable get() = LocalAuralisSpacing.current
    val motion: AuralisMotion
        @Composable @ReadOnlyComposable get() = LocalAuralisMotion.current
}

/**
 * Maps the Auralis tokens onto every Material 3 color role. Material expects opaque colors, so
 * translucent tokens are composited over the ground first. Errors are neutral as well: the spec
 * signals errors with an icon plus text, not with red.
 */
internal fun AuralisColors.toMaterialColorScheme(): ColorScheme {
    val ground = background
    val base = if (isDark) ground else surfaceSolid
    fun tone(alpha: Float): Color = contentPrimary.copy(alpha = alpha).compositeOver(base)
    val selected = selection.compositeOver(surfaceSolid)
    val secondaryText = contentSecondary.compositeOver(ground)
    val scheme = if (isDark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = contentPrimary,
        onPrimary = ground,
        primaryContainer = selected,
        onPrimaryContainer = contentPrimary,
        inversePrimary = ground,
        secondary = secondaryText,
        onSecondary = ground,
        secondaryContainer = selected,
        onSecondaryContainer = contentPrimary,
        tertiary = secondaryText,
        onTertiary = ground,
        tertiaryContainer = selected,
        onTertiaryContainer = contentPrimary,
        background = ground,
        onBackground = contentPrimary,
        surface = ground,
        onSurface = contentPrimary,
        surfaceVariant = surfaceSolid,
        onSurfaceVariant = secondaryText,
        surfaceTint = contentPrimary,
        inverseSurface = contentPrimary,
        inverseOnSurface = ground,
        error = contentPrimary,
        onError = ground,
        errorContainer = selected,
        onErrorContainer = contentPrimary,
        outline = borderStrong.compositeOver(ground),
        outlineVariant = borderSubtle.compositeOver(ground),
        scrim = Color.Black,
        surfaceBright = tone(0.14f),
        surfaceDim = ground,
        surfaceContainerLowest = if (isDark) ground else surfaceSolid,
        surfaceContainerLow = tone(0.03f),
        surfaceContainer = tone(0.05f),
        surfaceContainerHigh = tone(0.08f),
        surfaceContainerHighest = tone(0.11f),
    )
}

internal fun AuralisTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = wordmark.copy(fontSize = wordmark.fontSize * 1.6f, lineHeight = wordmark.lineHeight * 1.6f),
    displayMedium = wordmark.copy(fontSize = wordmark.fontSize * 1.3f, lineHeight = wordmark.lineHeight * 1.3f),
    displaySmall = wordmark,
    headlineLarge = screenTitle,
    headlineMedium = nowPlayingTitle,
    headlineSmall = sectionTitle,
    titleLarge = sectionTitle,
    titleMedium = itemTitle,
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = itemSubtitle,
    bodySmall = bodySmall,
    labelLarge = label,
    labelMedium = labelSmall,
    labelSmall = labelSmall,
)

internal fun AuralisShapes.toMaterialShapes(): Shapes = Shapes(
    extraSmall = small,
    small = small,
    medium = medium,
    large = large,
    extraLarge = extraLarge,
)
