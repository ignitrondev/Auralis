/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

/**
 * Vertical swipe on the mini player: down past [DismissDistance] (or a quick flick down) calls
 * [onDismiss] and slides it away; up past [OpenDistance] (or a flick up) calls [onOpen]; anything
 * shorter springs back. Taps still reach the player, because a drag only starts after the touch
 * slop. Screen readers get the dismiss action as a custom action, since they cannot swipe.
 *
 * The offset follows the finger synchronously and is applied inside the gesture, so the gesture
 * is measured in coordinates that do not move with the player. The action runs before the settle animation, so
 * nothing (a late drag event, the player leaving composition) can cancel it, and the offset is
 * always back at zero afterwards: if the player is still shown, it is in its place.
 */
@Composable
internal fun Modifier.swipeToDismiss(onDismiss: () -> Unit, onOpen: () -> Unit, dismissLabel: String): Modifier {
    var offset by remember { mutableFloatStateOf(0f) }
    val spec: AnimationSpec<Float> = AuralisTheme.motion.standard()
    val density = LocalDensity.current
    val dismissPx = with(density) { DismissDistance.toPx() }
    val openPx = with(density) { OpenDistance.toPx() }
    val flickPx = with(density) { FlickVelocity.toPx() }
    val state = rememberDraggableState { delta ->
        // Upward drags meet resistance: the gesture only needs to show intent to open.
        offset += if (offset + delta < 0f) delta * UPWARD_RESISTANCE else delta
    }
    suspend fun settleTo(target: Float) {
        val animatable = Animatable(offset)
        try {
            animatable.animateTo(target, spec) { offset = value }
        } finally {
            offset = 0f
        }
    }
    // The gesture is measured outside the moving layer: if it were inside, the player moving with
    // the finger would cancel out (or reverse) the drag it reports.
    return this
        .draggable(
            state = state,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity ->
                when {
                    offset > dismissPx || velocity > flickPx -> {
                        onDismiss()
                        settleTo(dismissPx * 2)
                    }
                    offset < -openPx || velocity < -flickPx -> {
                        onOpen()
                        settleTo(0f)
                    }
                    else -> settleTo(0f)
                }
            },
        )
        .graphicsLayer {
            translationY = offset
            alpha = 1f - (offset / (dismissPx * 2)).coerceIn(0f, MAX_FADE)
        }
        .semantics {
            customActions = listOf(CustomAccessibilityAction(dismissLabel) { onDismiss(); true })
        }
}

private val DismissDistance = 72.dp
private val OpenDistance = 24.dp
private val FlickVelocity = 1200.dp
private const val UPWARD_RESISTANCE = 0.35f
private const val MAX_FADE = 0.9f
