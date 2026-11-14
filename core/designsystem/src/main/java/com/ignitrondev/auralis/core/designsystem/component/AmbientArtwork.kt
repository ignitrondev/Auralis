/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

/** Real blur needs Android 12; older versions get the neutral fallback instead. */
private val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Full-screen ambient background for Now Playing: the artwork, heavily blurred, under the theme
 * scrim (spec section 8.2.1: artwork is the only source of color, and it lives behind the glass).
 * Without artwork, or without blur support, it is the neutral gradient.
 */
@Composable
fun AmbientBackdrop(uri: String?, modifier: Modifier = Modifier) {
    val colors = AuralisTheme.colors
    // Clipped: the scaled, blurred artwork must not spill past the screen it belongs to (it showed
    // as a tinted band over the page below while Now Playing slid away).
    Box(modifier = modifier.fillMaxSize().clipToBounds().background(colors.background)) {
        if (uri != null && blurSupported) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.4f)
                    .blur(90.dp)
                    .alpha(0.85f),
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.artworkFallbackTop, colors.background))))
        }
        // Scrim keeps controls and text readable over bright artwork.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(colors.scrim.copy(alpha = 0.25f), colors.scrim, colors.background))),
        )
    }
}

/**
 * Large rounded artwork with a glow in its own colors: a blurred copy of the artwork sits
 * behind it, slightly lower, like a colored shadow. Below Android 12 it gets a neutral shadow.
 */
@Composable
fun GlowingArtwork(
    uri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = AuralisTheme.shapes.large,
) {
    BoxWithConstraints(modifier = modifier) {
        val glowOffset = maxWidth * 0.09f
        if (uri != null && blurSupported) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = maxWidth * 0.05f)
                    .offset(y = glowOffset)
                    // Unbounded: the blur spreads past the image bounds like a soft shadow,
                    // instead of being cut into a hard-edged rectangle.
                    .blur(36.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
            )
        }
        Artwork(
            uri = uri,
            contentDescription = contentDescription,
            shape = shape,
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (blurSupported) Modifier else Modifier.shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black),
                ),
        )
    }
}
