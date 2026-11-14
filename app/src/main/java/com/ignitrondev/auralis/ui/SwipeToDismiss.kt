/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import kotlinx.coroutines.launch

/**
 * Vertical swipe on the mini player: down past [DismissDistance] (or a quick flick down) slides it
 * away and calls [onDismiss]; up past [OpenDistance] (or a flick up) calls [onOpen]; anything
 * shorter springs back. Taps still reach the player, because a drag only starts after the touch
 * slop. Screen readers get the dismiss action as a custom action, since they cannot swipe.
 */
@Composable
internal fun Modifier.swipeToDismiss(onDismiss: () -> Unit, onOpen: () -> Unit, dismissLabel: String): Modifier {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val motion = AuralisTheme.motion
    val density = LocalDensity.current
    val dismissPx = with(density) { DismissDistance.toPx() }
    val openPx = with(density) { OpenDistance.toPx() }
    val flickPx = with(density) { FlickVelocity.toPx() }
    val state = rememberDraggableState { delta ->
        // Upward drags meet resistance: the gesture only needs to show intent to open.
        val step = if (offset.value + delta < 0f) delta * UPWARD_RESISTANCE else delta
        scope.launch { offset.snapTo(offset.value + step) }
    }
    return this
        .graphicsLayer {
            translationY = offset.value
            alpha = 1f - (offset.value / (dismissPx * 2)).coerceIn(0f, MAX_FADE)
        }
        .draggable(
            state = state,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity ->
                when {
                    offset.value > dismissPx || velocity > flickPx -> {
                        offset.animateTo(dismissPx * 2, motion.standard())
                        onDismiss()
                    }
                    offset.value < -openPx || velocity < -flickPx -> {
                        offset.animateTo(0f, motion.standard())
                        onOpen()
                    }
                    else -> offset.animateTo(0f, motion.standard())
                }
            },
        )
        .semantics {
            customActions = listOf(CustomAccessibilityAction(dismissLabel) { onDismiss(); true })
        }
}

private val DismissDistance = 72.dp
private val OpenDistance = 24.dp
private val FlickVelocity = 1200.dp
private const val UPWARD_RESISTANCE = 0.35f
private const val MAX_FADE = 0.9f
