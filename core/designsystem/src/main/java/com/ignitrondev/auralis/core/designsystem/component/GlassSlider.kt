/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

/**
 * Seek slider for glass panels (spec section 8.1). A neutral capsule track with a filled part and
 * a round thumb that grows while dragging. Stateless: [value] is 0 to 1; [onValueChangeFinished]
 * fires once when the user lets go, so the player seeks once per gesture.
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = AuralisTheme.colors
    var dragging by remember { mutableStateOf(false) }
    val thumbScale by animateFloatAsState(if (dragging) 1.4f else 1f, AuralisTheme.motion.press(), label = "thumb")
    val clamped = value.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .semantics {
                contentDescription?.let { this.contentDescription = it }
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
                setProgress { target ->
                    onValueChange(target.coerceIn(0f, 1f))
                    onValueChangeFinished()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                    onValueChangeFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                    },
                    onDragEnd = {
                        dragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        dragging = false
                        onValueChangeFinished()
                    },
                    onHorizontalDrag = { change, _ ->
                        onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                    },
                )
            },
    ) {
        val track = 5.dp.toPx()
        val radius = 7.dp.toPx()
        val y = size.height / 2f
        val start = radius
        val width = size.width - 2 * radius
        drawRoundRect(
            color = colors.borderStrong,
            topLeft = Offset(start, y - track / 2),
            size = Size(width, track),
            cornerRadius = CornerRadius(track / 2),
        )
        drawRoundRect(
            color = colors.contentPrimary,
            topLeft = Offset(start, y - track / 2),
            size = Size(width * clamped, track),
            cornerRadius = CornerRadius(track / 2),
        )
        drawCircle(colors.contentPrimary, radius * thumbScale, Offset(start + width * clamped, y))
    }
}
