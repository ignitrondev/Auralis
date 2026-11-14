/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

/**
 * Press feedback shared by every tappable glass control: scales down slightly while pressed and
 * springs back on release (spec section 8.3).
 *
 * The scale is read inside [graphicsLayer], so it animates in the draw phase without
 * recomposing the content. With reduced motion the control does not move.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val motion = AuralisTheme.motion
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) motion.pressedScale else 1f,
        animationSpec = motion.press(),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
