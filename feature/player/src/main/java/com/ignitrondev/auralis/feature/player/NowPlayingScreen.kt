/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.AmbientBackdrop
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.component.GlassLayers
import com.ignitrondev.auralis.core.designsystem.component.GlassSlider
import com.ignitrondev.auralis.core.designsystem.component.GlassSurface
import com.ignitrondev.auralis.core.designsystem.component.GlowingArtwork
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.share.ShareCard
import com.ignitrondev.auralis.core.share.ShareSheet
import com.ignitrondev.auralis.core.share.R as ShareR
import com.ignitrondev.auralis.core.ui.MessageState
import com.ignitrondev.auralis.core.ui.formatDuration
import com.ignitrondev.auralis.domain.player.PlayerState
import com.ignitrondev.auralis.domain.player.RepeatMode

/**
 * Now Playing (spec section 9): blurred artwork as the ambient background, large rounded
 * artwork with a glow in its own colors, and the controls on glass.
 *
 * The app shell opens it from the mini player. It knows nothing about that transition: the shell
 * passes the modifiers that animate it.
 *
 * @param modifier The whole screen; the shell grows it out of the mini player.
 * @param artworkModifier The artwork, which moves from the mini player into place.
 * @param revealModifier Track information and controls, which appear after the artwork.
 */
@Composable
fun NowPlayingScreen(
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
    revealModifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val like by viewModel.like.collectAsStateWithLifecycle()
    // The account may have changed since the screen was last open (another device, another app).
    LaunchedEffect(Unit) { viewModel.refreshLike() }
    val track = state.currentTrack
    if (track == null) {
        MessageState(stringResource(R.string.feature_player_nothing))
        return
    }
    var sharing by remember { mutableStateOf(false) }
    if (sharing) {
        val kind = stringResource(if (track.isVideo) ShareR.string.core_share_kind_video else ShareR.string.core_share_kind_song)
        ShareSheet(
            card = ShareCard(
                title = track.title,
                subtitle = listOf(kind, track.artistNames).filter { it.isNotBlank() }.joinToString(" • "),
                artworkUri = track.artworkUri,
                link = viewModel.shareLink(track),
            ),
            onDismiss = { sharing = false },
        )
    }
    val spacing = AuralisTheme.spacing
    GlassLayers(
        modifier = modifier.fillMaxSize(),
        overlay = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = spacing.screenGutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TopBar(onCollapse = onCollapse, like = like, onToggleLike = viewModel::toggleLike, onShare = { sharing = true })
                Spacer(Modifier.weight(0.6f))
                GlowingArtwork(
                    uri = track.artworkUri,
                    contentDescription = stringResource(R.string.feature_player_artwork),
                    shape = AuralisTheme.shapes.large,
                    modifier = artworkModifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth(0.86f)
                        .aspectRatio(1f),
                )
                Spacer(Modifier.weight(0.7f))
                Column(modifier = revealModifier, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = track.title,
                        style = AuralisTheme.typography.nowPlayingTitle,
                        color = AuralisTheme.colors.contentPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = track.artistNames,
                        style = AuralisTheme.typography.body,
                        color = AuralisTheme.colors.contentSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                    if (like.failed) {
                        Text(
                            text = stringResource(R.string.feature_player_like_failed),
                            style = AuralisTheme.typography.bodySmall,
                            color = AuralisTheme.colors.contentSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = spacing.xs),
                        )
                    }
                    Spacer(Modifier.height(spacing.xl))
                    ControlsPanel(state = state, viewModel = viewModel)
                }
                Spacer(Modifier.height(spacing.lg))
            }
        },
    ) {
        AmbientBackdrop(uri = track.artworkUri)
    }
}

@Composable
private fun TopBar(onCollapse: () -> Unit, like: LikeUiState, onToggleLike: () -> Unit, onShare: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = AuralisTheme.spacing.sm)) {
        GlassIconButton(
            icon = AuralisIcons.Collapse,
            contentDescription = stringResource(R.string.feature_player_collapse),
            onClick = onCollapse,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = stringResource(R.string.feature_player_now_playing),
            style = AuralisTheme.typography.label,
            color = AuralisTheme.colors.contentSecondary,
            modifier = Modifier.align(Alignment.Center),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
        ) {
            GlassIconButton(
                icon = AuralisIcons.Share,
                contentDescription = stringResource(R.string.feature_player_share),
                onClick = onShare,
            )
            if (like.visible) {
                val liked = like.liked == true
                // Liked is shown by shape (filled or outline), never by colour (spec 8.2.1).
                GlassIconButton(
                    icon = if (liked) AuralisIcons.Liked else AuralisIcons.NotLiked,
                    contentDescription = stringResource(if (liked) R.string.feature_player_unlike else R.string.feature_player_like),
                    onClick = onToggleLike,
                    enabled = !like.pending,
                )
            }
        }
    }
}

/** Seek bar, times and transport controls on one glass panel over the blurred artwork. */
@Composable
private fun ControlsPanel(state: PlayerState, viewModel: NowPlayingViewModel) {
    val spacing = AuralisTheme.spacing
    // While dragging, the slider follows the finger; the player seeks once on release.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(0)
    val fraction = dragFraction ?: if (duration > 0) state.positionMs.toFloat() / duration else 0f
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = AuralisTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            GlassSlider(
                value = fraction,
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let { viewModel.seekTo((it * duration).toLong()) }
                    dragFraction = null
                },
                contentDescription = stringResource(R.string.feature_player_seek),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration((fraction * duration).toLong()),
                    style = AuralisTheme.typography.timestamp,
                    color = AuralisTheme.colors.contentSecondary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (duration > 0) formatDuration(duration) else "--:--",
                    style = AuralisTheme.typography.timestamp,
                    color = AuralisTheme.colors.contentSecondary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    icon = AuralisIcons.Shuffle,
                    contentDescription = stringResource(R.string.feature_player_shuffle),
                    onClick = viewModel::toggleShuffle,
                    selected = state.shuffleEnabled,
                )
                GlassIconButton(
                    icon = AuralisIcons.SkipPrevious,
                    contentDescription = stringResource(R.string.feature_player_previous),
                    onClick = viewModel::previous,
                    size = 56.dp,
                )
                GlassIconButton(
                    icon = if (state.isPlaying) AuralisIcons.Pause else AuralisIcons.Play,
                    contentDescription = stringResource(if (state.isPlaying) R.string.feature_player_pause else R.string.feature_player_play),
                    onClick = viewModel::togglePlayPause,
                    size = 76.dp,
                )
                GlassIconButton(
                    icon = AuralisIcons.SkipNext,
                    contentDescription = stringResource(R.string.feature_player_next),
                    onClick = viewModel::next,
                    enabled = state.currentIndex < state.queue.lastIndex || state.repeatMode != RepeatMode.OFF,
                    size = 56.dp,
                )
                GlassIconButton(
                    icon = if (state.repeatMode == RepeatMode.ONE) AuralisIcons.RepeatOne else AuralisIcons.Repeat,
                    contentDescription = stringResource(
                        when (state.repeatMode) {
                            RepeatMode.OFF -> R.string.feature_player_repeat_off
                            RepeatMode.ALL -> R.string.feature_player_repeat_all
                            RepeatMode.ONE -> R.string.feature_player_repeat_one
                        },
                    ),
                    onClick = viewModel::cycleRepeat,
                    selected = state.repeatMode != RepeatMode.OFF,
                )
            }
        }
    }
}
