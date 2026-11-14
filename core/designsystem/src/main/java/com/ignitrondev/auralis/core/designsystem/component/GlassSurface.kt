/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.theme.AuralisColors
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import kotlin.math.max

/** True inside another glass surface, so nested glass draws a light inner layer. */
private val LocalInsideGlass = staticCompositionLocalOf { false }

/**
 * The base Liquid Glass material (spec section 8.2). Every glass component is built on it.
 *
 * It renders one of three ways:
 * - **Over a backdrop** (inside the overlay of [GlassLayers]): the backdrop is blurred, and
 *   refracted on Android 13+. Text stays readable because the glass tint includes the theme
 *   scrim. Below Android 12, where blur is unavailable, the tint is close to opaque.
 * - **Without a backdrop**: an opaque-enough fallback fill with a top-left highlight.
 * - **Nested in another glass surface**: a light inner layer, so glass never stacks heavily.
 *
 * Every variant has a hairline border so the edge stays visible in the light theme. Content gets
 * [AuralisColors.contentPrimary] as its content color. The blur itself is never animated.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AuralisTheme.shapes.large,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AuralisTheme.colors
    val backdrop = LocalGlassBackdrop.current
    val nested = LocalInsideGlass.current

    val material = when {
        nested -> Modifier
            .clip(shape)
            .background(colors.selection)
        backdrop != null -> {
            val style = remember(colors, shape) { glassStyle(colors, shape) }
            Modifier
                .hazeGlass(input = HazeInput.Sources(backdrop), style = style)
                .clip(shape)
        }
        else -> Modifier
            .clip(shape)
            .background(colors.surfaceGlassFallback)
            .glassHighlight(colors.highlight)
    }

    Box(
        modifier = modifier
            .then(material)
            .border(width = 1.dp, color = colors.borderSubtle, shape = shape),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.contentPrimary,
            LocalInsideGlass provides true,
        ) {
            content()
        }
    }
}

/** Builds the Haze glass style from Auralis tokens, independent of the system dark setting. */
@OptIn(ExperimentalHazeApi::class)
private fun glassStyle(colors: AuralisColors, shape: RoundedCornerShape): GlassStyle {
    val blurAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val tint = if (blurAvailable) {
        colors.surfaceGlass.compositeOver(colors.scrim)
    } else {
        colors.surfaceGlassFallback
    }
    // GlassStyle.regular follows the system dark setting, but Auralis has its own theme
    // (dark by default), so every appearance-dependent value is set explicitly.
    return GlassStyle.regular.then {
        shape(shape)
        tint(tint)
        edgeShadow(colors.shadow)
        chromaMultiplier(1f)
        if (colors.isDark) {
            specularIntensity(0.38f)
            ambientResponse(0.08f)
            contrast(0.08f)
            whitePoint(-0.22f)
        } else {
            specularIntensity(0.48f)
            ambientResponse(0.15f)
            contrast(0f)
            whitePoint(0.55f)
        }
    }
}

/** Soft sheen from the top-left corner, used by the fallback glass fill. */
private fun Modifier.glassHighlight(color: Color): Modifier = drawWithCache {
    val brush = Brush.radialGradient(
        colors = listOf(color, Color.Transparent),
        center = Offset.Zero,
        radius = max(size.width, size.height) * 0.9f,
    )
    onDrawBehind { drawRect(brush) }
}
