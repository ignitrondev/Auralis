/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme


/**
 * Full-screen ambient background for Now Playing: a soft wash of the artwork's colours under the
 * theme scrim (spec section 8.2.1: artwork is the only source of color, and it lives behind the
 * glass). The wash is a precomputed bitmap ([rememberArtworkWash]), not a live blur, so it costs
 * nothing per frame and works on every Android version. Without artwork it is the neutral gradient.
 */
@Composable
fun AmbientBackdrop(uri: String?, modifier: Modifier = Modifier) {
    val colors = AuralisTheme.colors
    val wash by rememberArtworkWash(uri)
    // The wash fades in over the neutral gradient once ready (an alpha change, never an animated blur).
    val washAlpha by animateFloatAsState(if (wash != null) WASH_ALPHA else 0f, AuralisTheme.motion.fade(), label = "ambient wash")
    // Clipped: the wash must not spill past the screen it belongs to (it showed as a tinted band
    // over the page below while Now Playing slid away).
    Box(modifier = modifier.fillMaxSize().clipToBounds().background(colors.background)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.artworkFallbackTop, colors.background))))
        wash?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = washAlpha },
            )
        }
        // Scrim keeps controls and text readable over bright artwork.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(colors.scrim.copy(alpha = 0.25f), colors.scrim, colors.background))),
        )
    }
}

private const val WASH_ALPHA = 0.85f

/**
 * Large rounded artwork with a glow in its own colors: a soft wash of the artwork
 * ([rememberArtworkWash]) sits behind it, slightly larger and lower, its edges faded out like a
 * coloured shadow. No live blur, so it costs nothing per frame (a blurred copy made opening Now
 * Playing stutter) and it works on every Android version.
 */
@Composable
fun GlowingArtwork(
    uri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = AuralisTheme.shapes.large,
) {
    val wash by rememberArtworkWash(uri)
    val glowAlpha by animateFloatAsState(if (wash != null) 1f else 0f, AuralisTheme.motion.fade(), label = "artwork glow")
    BoxWithConstraints(modifier = modifier) {
        val glowOffset = maxWidth * GLOW_OFFSET
        wash?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = glowOffset)
                    .graphicsLayer {
                        scaleX = GLOW_SCALE
                        scaleY = GLOW_SCALE
                        alpha = glowAlpha
                        // Offscreen so the fade below masks only the glow, not what is under it.
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        // Fade the edges out: a soft halo instead of a coloured rectangle.
                        drawRect(
                            brush = Brush.radialGradient(
                                0.35f to Color.Black,
                                1f to Color.Transparent,
                                center = center,
                                radius = size.maxDimension / 2,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }
        Artwork(
            uri = uri,
            contentDescription = contentDescription,
            shape = shape,
            modifier = Modifier.matchParentSize(),
        )
    }
}

private const val GLOW_OFFSET = 0.09f
private const val GLOW_SCALE = 1.12f
