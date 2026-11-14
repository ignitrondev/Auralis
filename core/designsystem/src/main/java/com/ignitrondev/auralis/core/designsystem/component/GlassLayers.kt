/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** The backdrop that glass surfaces in an overlay sample. Null outside [GlassLayers]. */
internal val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Stacks floating glass over content.
 *
 * [content] fills the box on the theme background and is recorded as the backdrop. Glass
 * surfaces in [overlay] (for example the navigation bar and mini player) blur and refract it.
 * A glass surface cannot sample a backdrop it is part of, so glass placed inside [content] uses
 * the readable fallback fill instead.
 */
@Composable
fun GlassLayers(
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                // Recorded with the content, so glass never samples a transparent ground.
                .background(AuralisTheme.colors.background),
        ) {
            content()
        }
        val scope = this
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            scope.overlay()
        }
    }
}
