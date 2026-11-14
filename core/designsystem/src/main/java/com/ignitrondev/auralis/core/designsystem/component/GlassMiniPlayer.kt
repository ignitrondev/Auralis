/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.R
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.motion.pressScale
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

private val MiniPlayerHeight = 64.dp
private val ArtworkSize = 44.dp
private val ControlSize = 48.dp
private val ProgressStroke = 2.dp

/**
 * Floating glass mini player (spec section 8.1). Stateless: the caller passes the current track
 * and handles every action. Place it in the overlay of [GlassLayers], above the navigation bar.
 *
 * @param progress Playback progress from 0 to 1. It is a lambda read only while drawing, so
 * position updates repaint the progress line without recomposing the player.
 * @param onClick Opens the Now Playing screen; null when there is nowhere to go yet (the player
 * then has no click semantics, so it never looks like a control that does nothing).
 * @param onNextClick Shows a next-track button when not null.
 * @param artwork Album artwork, clipped to a small rounded square like the Now Playing artwork it
 * grows into. Defaults to the neutral gradient used when there is no artwork.
 */
@Composable
fun GlassMiniPlayer(
    title: String,
    artist: String?,
    isPlaying: Boolean,
    progress: () -> Float,
    onPlayPauseClick: () -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onNextClick: (() -> Unit)? = null,
    artwork: @Composable () -> Unit = { ArtworkFallback() },
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val openLabel = stringResource(R.string.core_designsystem_open_player)

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .pressScale(interactionSource),
        shape = AuralisTheme.shapes.capsule,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = colors.contentPrimary),
                            role = Role.Button,
                            onClickLabel = openLabel,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .progressLine(progress)
                .padding(start = spacing.sm + spacing.xxs, end = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(ArtworkSize)
                    .clip(AuralisTheme.shapes.small),
            ) {
                artwork()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AuralisTheme.typography.itemTitle,
                    color = colors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artist != null) {
                    Text(
                        text = artist,
                        style = AuralisTheme.typography.itemSubtitle,
                        color = colors.contentSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            MiniPlayerControl(
                icon = if (isPlaying) AuralisIcons.Pause else AuralisIcons.Play,
                contentDescription = stringResource(
                    if (isPlaying) R.string.core_designsystem_pause else R.string.core_designsystem_play,
                ),
                onClick = onPlayPauseClick,
            )
            if (onNextClick != null) {
                MiniPlayerControl(
                    icon = AuralisIcons.SkipNext,
                    contentDescription = stringResource(R.string.core_designsystem_next_track),
                    onClick = onNextClick,
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(ControlSize)
            .pressScale(interactionSource)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AuralisTheme.colors.contentPrimary),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AuralisTheme.colors.contentPrimary,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Thin progress line along the bottom inner edge. [progress] is read in the draw phase only. */
@Composable
private fun Modifier.progressLine(progress: () -> Float): Modifier {
    val track = AuralisTheme.colors.borderSubtle
    val fill = AuralisTheme.colors.contentSecondary
    return drawWithContent {
        drawContent()
        val stroke = ProgressStroke.toPx()
        val inset = size.height / 2f
        val y = size.height - stroke
        val start = Offset(inset, y)
        val end = Offset(size.width - inset, y)
        drawLine(track, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
        val fraction = progress().coerceIn(0f, 1f)
        if (fraction > 0f) {
            drawLine(
                color = fill,
                start = start,
                end = Offset(start.x + (end.x - start.x) * fraction, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Neutral gradient shown when a track has no artwork. Never a colored placeholder. */
@Composable
fun ArtworkFallback(modifier: Modifier = Modifier) {
    val colors = AuralisTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.artworkFallbackTop, colors.artworkFallbackBottom))),
    )
}
