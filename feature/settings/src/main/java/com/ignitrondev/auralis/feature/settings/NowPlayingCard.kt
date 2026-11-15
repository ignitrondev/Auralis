/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.Artwork
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.component.GlassLayers
import com.ignitrondev.auralis.core.designsystem.component.GlassSurface
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.share.ShareCard
import com.ignitrondev.auralis.core.share.ShareSheet
import com.ignitrondev.auralis.core.share.R as ShareR
import com.ignitrondev.auralis.core.designsystem.R as DesignR
import com.ignitrondev.auralis.core.ui.formatDuration

/**
 * The song in the player, at the top of Settings: its cover fills the card, a label and the Share
 * and Like buttons sit on glass at the top, and a glass strip at the bottom holds the title, the
 * controls and the progress. Hidden when nothing is playing, so it never looks like a control
 * that does nothing.
 */
@Composable
internal fun NowPlayingCard(modifier: Modifier = Modifier, viewModel: NowPlayingCardViewModel = hiltViewModel()) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    val state = card ?: return
    val spacing = AuralisTheme.spacing
    val colors = AuralisTheme.colors
    var sharing by remember { mutableStateOf(false) }
    if (sharing) {
        val kind = stringResource(if (state.track.isVideo) ShareR.string.core_share_kind_video else ShareR.string.core_share_kind_song)
        ShareSheet(
            card = ShareCard(
                title = state.track.title,
                subtitle = listOf(kind, state.track.artistNames).filter { it.isNotBlank() }.joinToString(" • "),
                artworkUri = state.track.artworkUri,
                link = viewModel.shareLink(state.track),
            ),
            onDismiss = { sharing = false },
        )
    }
    GlassLayers(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(AuralisTheme.shapes.extraLarge),
        overlay = {
            // Label, top left.
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(spacing.md),
                shape = AuralisTheme.shapes.capsule,
            ) {
                Text(
                    text = stringResource(R.string.feature_settings_now_playing),
                    style = AuralisTheme.typography.labelSmall,
                    color = colors.contentPrimary,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs + spacing.xxs),
                )
            }
            // Share and Like, top right.
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                GlassIconButton(
                    icon = AuralisIcons.Share,
                    contentDescription = stringResource(R.string.feature_settings_share_song),
                    onClick = { sharing = true },
                )
                liked?.let { isLiked ->
                    // Liked is shown by shape (filled or outline), never by colour (spec 8.2.1).
                    GlassIconButton(
                        icon = if (isLiked) AuralisIcons.Liked else AuralisIcons.NotLiked,
                        contentDescription = stringResource(if (isLiked) R.string.feature_settings_unlike_song else R.string.feature_settings_like_song),
                        onClick = viewModel::toggleLike,
                    )
                }
            }
            // The strip: title and artist, previous, play or pause, next, and the progress.
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(spacing.md),
                shape = AuralisTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.sm, top = spacing.sm, bottom = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.track.title,
                                style = AuralisTheme.typography.itemTitle,
                                color = colors.contentPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = state.track.artistNames,
                                style = AuralisTheme.typography.itemSubtitle,
                                color = colors.contentSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        GlassIconButton(
                            icon = AuralisIcons.SkipPrevious,
                            contentDescription = stringResource(R.string.feature_settings_previous_track),
                            onClick = viewModel::previous,
                            enabled = state.hasPrevious,
                        )
                        GlassIconButton(
                            icon = if (state.isPlaying) AuralisIcons.Pause else AuralisIcons.Play,
                            contentDescription = stringResource(if (state.isPlaying) DesignR.string.core_designsystem_pause else DesignR.string.core_designsystem_play),
                            onClick = viewModel::togglePlayPause,
                            size = 56.dp,
                        )
                        GlassIconButton(
                            icon = AuralisIcons.SkipNext,
                            contentDescription = stringResource(DesignR.string.core_designsystem_next_track),
                            onClick = viewModel::next,
                            enabled = state.hasNext,
                        )
                    }
                    Row(
                        modifier = Modifier.padding(end = spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        Text(text = formatDuration(state.positionMs), style = AuralisTheme.typography.timestamp, color = colors.contentSecondary)
                        ProgressLine(progress = { state.progress }, modifier = Modifier.weight(1f))
                        Text(text = formatDuration(state.durationMs), style = AuralisTheme.typography.timestamp, color = colors.contentSecondary)
                    }
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Artwork(uri = state.track.artworkUri, contentDescription = null, shape = AuralisTheme.shapes.extraLarge, modifier = Modifier.fillMaxSize())
            // Darkens the lower part so the strip sits on a calm ground over any cover.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), startY = 120f)),
            )
        }
    }
}

/** A thin, read-only progress line. [progress] is read while drawing, so a tick repaints only the line. */
@Composable
private fun ProgressLine(progress: () -> Float, modifier: Modifier = Modifier) {
    val colors = AuralisTheme.colors
    Box(
        modifier = modifier
            .height(ProgressHeight)
            .clip(CircleShape)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress(), 0f..1f) }
            .drawBehind {
                drawRoundRect(colors.contentPrimary.copy(alpha = 0.22f), size = size, cornerRadius = CornerRadius(size.height / 2))
                drawRoundRect(
                    colors.contentPrimary,
                    size = Size(size.width * progress().coerceIn(0f, 1f), size.height),
                    cornerRadius = CornerRadius(size.height / 2),
                )
            },
    )
}

private val CardHeight = 252.dp
private val ProgressHeight = 4.dp
