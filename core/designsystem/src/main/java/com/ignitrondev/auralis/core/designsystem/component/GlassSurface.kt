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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.theme.AuralisColors
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow

/** True inside another glass surface, so nested glass draws a light inner layer. */
private val LocalInsideGlass = staticCompositionLocalOf { false }

/** Which glass material a [GlassSurface] uses. */
enum class GlassStyle {
    /** Blurred and tinted glass for controls and cards. */
    Standard,

    /**
     * Floating chrome (mini player, navigation bar): barely blurred, a whiter tint, a clearer
     * outline, and light catching the top and left edges. What is behind shows through, softened
     * by the white rather than by blur.
     */
    Floating,
}

/**
 * The base Liquid Glass material (spec section 8.2). Every glass component is built on it.
 *
 * It renders one of three ways:
 * - **Over a backdrop** (inside the overlay of [GlassLayers]): the backdrop is blurred
 *   (Android 12+) and refracted through a lens at the edges (Android 13+), with an even rim and
 *   a soft shadow. Text stays readable because the glass tint includes the theme scrim.
 *   Below Android 12, where blur is unavailable, a near-opaque fallback tint is used instead.
 * - **Without a backdrop**: a flat, opaque-enough fallback fill.
 * - **Nested in another glass surface**: a light inner layer, so glass never stacks heavily.
 *
 * The surface is flat: no gradient sheen. [GlassStyle.Floating] adds edge light, a directional
 * highlight on the rim only. Every variant has a hairline border so the edge stays visible in the
 * light theme. Content gets [AuralisColors.contentPrimary] as its
 * content color. The blur itself is never animated.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AuralisTheme.shapes.large,
    style: GlassStyle = GlassStyle.Standard,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AuralisTheme.colors
    val floating = style == GlassStyle.Floating
    val backdrop = LocalGlassBackdrop.current
    val nested = LocalInsideGlass.current

    val material = when {
        nested -> Modifier
            .clip(shape)
            .background(colors.selection)
        backdrop != null -> {
            val tint = remember(colors, floating) { glassTint(colors, floating) }
            val shadow = remember(colors) { Shadow(color = colors.shadow) }
            val highlight = remember(colors, floating) { if (floating) edgeLightHighlight(colors) else FlatRim }
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        blur((if (floating) FloatingBlurRadius else GlassBlurRadius).toPx())
                        lens(
                            refractionHeight = GlassLensHeight.toPx(),
                            refractionAmount = GlassLensAmount.toPx(),
                        )
                    },
                    highlight = { highlight },
                    shadow = { shadow },
                    onDrawSurface = { drawRect(tint) },
                )
                .clip(shape)
        }
        else -> Modifier
            .clip(shape)
            .background(colors.surfaceGlassFallback)
    }

    Box(
        modifier = modifier
            .then(material)
            .border(
                width = 1.dp,
                color = if (floating && backdrop != null && !nested) colors.borderGlassFloating else colors.borderSubtle,
                shape = shape,
            ),
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
private fun glassTint(colors: AuralisColors, floating: Boolean): Color =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (floating) {
            colors.surfaceGlassFloating.compositeOver(colors.scrimGlassFloating)
        } else {
            colors.surfaceGlass.compositeOver(colors.scrim)
        }
    } else {
        colors.surfaceGlassFallback
    }

/** Light from the top-left: the rim is bright on the top and left edges and fades towards the bottom right. */
private fun edgeLightHighlight(colors: AuralisColors) = Highlight(
    width = 1.5.dp,
    alpha = colors.edgeLight.alpha,
    style = HighlightStyle.Default(color = colors.edgeLight.copy(alpha = 1f), angle = EdgeLightAngle, falloff = EdgeLightFalloff),
)

/** An even rim light around the edge, with no directional gradient. */
private val FlatRim = Highlight(style = HighlightStyle.Plain())

private val GlassBlurRadius = 12.dp
private val FloatingBlurRadius = 1.dp
private const val EdgeLightAngle = 45f
private const val EdgeLightFalloff = 1f
private val GlassLensHeight = 16.dp
private val GlassLensAmount = 32.dp

/**
 * A small glass lens, used to mark the selected item inside a glass bar. It refracts the content
 * behind the bar, so selection reads as a piece of glass rather than a flat fill. Without a
 * backdrop it falls back to the flat selection fill.
 *
 * @param alpha Opacity, so the lens can fade in and out without animating the blur.
 */
@Composable
internal fun GlassIndicator(
    alpha: () -> Float,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AuralisTheme.shapes.capsule,
) {
    val colors = AuralisTheme.colors
    val backdrop = LocalGlassBackdrop.current
    if (backdrop == null) {
        Box(
            modifier
                .graphicsLayer { this.alpha = alpha() }
                .clip(shape)
                .background(colors.selection),
        )
        return
    }
    val indicatorHighlight = remember(colors) { edgeLightHighlight(colors) }
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(IndicatorBlurRadius.toPx())
                    lens(
                        refractionHeight = IndicatorLensHeight.toPx(),
                        refractionAmount = IndicatorLensAmount.toPx(),
                    )
                },
                highlight = { indicatorHighlight },
                shadow = null,
                layerBlock = { this.alpha = alpha() },
                onDrawSurface = { drawRect(colors.selection) },
            ),
    )
}

private val IndicatorBlurRadius = 3.dp
private val IndicatorLensHeight = 10.dp
private val IndicatorLensAmount = 20.dp
