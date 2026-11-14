/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

/**
 * Album, artist or playlist artwork. The neutral [ArtworkFallback] gradient shows while the
 * image loads and whenever there is no image, so there is never a colored placeholder.
 *
 * @param contentDescription Null for decorative artwork next to a title that already says what it is.
 */
@Composable
fun Artwork(
    uri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = AuralisTheme.shapes.medium,
) {
    Box(modifier = modifier.clip(shape)) {
        ArtworkFallback()
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
