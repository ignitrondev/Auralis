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
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.max

/** True inside another glass surface, so nested glass draws a light inner layer. */
private val LocalInsideGlass = staticCompositionLocalOf { false }

/**
 * The base Liquid Glass material (spec section 8.2). Every glass component is built on it.
 *
 * It renders one of three ways:
 * - **Over a backdrop** (inside the overlay of [GlassLayers]): the backdrop is blurred
 *   (Android 12+) and refracted through a lens at the edges (Android 13+), with a specular rim
 *   and a soft shadow. Text stays readable because the glass tint includes the theme scrim.
 *   Below Android 12, where blur is unavailable, a near-opaque fallback tint is used instead.
 * - **Without a backdrop**: an opaque-enough fallback fill with a top-left highlight.
 * - **Nested in another glass surface**: a light inner layer, so glass never stacks heavily.
 *
 * Every variant has a hairline border so the edge stays visible in the light theme, and a soft
 * top-left highlight. Content gets [AuralisColors.contentPrimary] as its content color. The blur
 * itself is never animated.
 */
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
            val tint = remember(colors) { glassTint(colors) }
            val shadow = remember(colors) { Shadow(color = colors.shadow) }
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        blur(GlassBlurRadius.toPx())
                        lens(
                            refractionHeight = GlassLensHeight.toPx(),
                            refractionAmount = GlassLensAmount.toPx(),
                        )
                    },
                    highlight = { Highlight.Default },
                    shadow = { shadow },
                    onDrawSurface = { drawRect(tint) },
                )
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

/**
 * Tint drawn over the blurred backdrop. It stacks the glass tint on the theme scrim (dark scrim
 * in dark, light scrim in light) so text stays readable over bright artwork. Without blur
 * (below Android 12) the near-opaque fallback fill is used instead.
 */
private fun glassTint(colors: AuralisColors): Color =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colors.surfaceGlass.compositeOver(colors.scrim)
    } else {
        colors.surfaceGlassFallback
    }

private val GlassBlurRadius = 12.dp
private val GlassLensHeight = 16.dp
private val GlassLensAmount = 32.dp

/** Soft sheen from the top-left corner, used by the fallback glass fill. */
private fun Modifier.glassHighlight(color: Color): Modifier = drawWithCache {
    val brush = Brush.radialGradient(
        colors = listOf(color, Color.Transparent),
        center = Offset.Zero,
        radius = max(size.width, size.height) * 0.9f,
    )
    onDrawBehind { drawRect(brush) }
}
