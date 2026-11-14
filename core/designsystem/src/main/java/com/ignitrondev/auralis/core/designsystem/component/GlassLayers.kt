/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** The backdrop that glass surfaces in an overlay blur. Null outside [GlassLayers]. */
internal val LocalGlassBackdrop = staticCompositionLocalOf<HazeState?> { null }

/**
 * Stacks floating glass over content.
 *
 * [content] fills the box and is the backdrop. Glass surfaces in [overlay] (for example the
 * navigation bar and mini player) blur and refract it. A glass surface can only blur content
 * from a backdrop it is not part of, so glass placed inside [content] uses the readable fallback
 * fill instead.
 */
@Composable
fun GlassLayers(
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val backdrop = rememberHazeState()
    Box(modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(backdrop),
        ) {
            content()
        }
        val scope = this
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            scope.overlay()
        }
    }
}
