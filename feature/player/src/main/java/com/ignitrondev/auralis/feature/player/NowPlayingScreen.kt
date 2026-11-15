/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.AmbientBackdrop
import com.ignitrondev.auralis.core.designsystem.component.Artwork
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.component.GlassLayers
import com.ignitrondev.auralis.core.designsystem.component.GlassSlider
import com.ignitrondev.auralis.core.designsystem.component.GlassStyle
import com.ignitrondev.auralis.core.designsystem.component.GlassSurface
import com.ignitrondev.auralis.core.designsystem.component.GlowingArtwork
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.share.ShareCard
import com.ignitrondev.auralis.core.share.ShareSheet
import com.ignitrondev.auralis.core.share.R as ShareR
import com.ignitrondev.auralis.core.ui.MessageState
import com.ignitrondev.auralis.core.ui.formatDuration
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.player.PlayerState
import com.ignitrondev.auralis.domain.player.RepeatMode
import androidx.compose.ui.text.intl.Locale

/**
 * Now Playing (spec section 9). The cover, zoomed and blurred, is the background of the whole
 * screen. Under a small cover the lyrics show live; tapping them opens them full screen. One glass
 * panel holds the title, the progress and the controls.
 *
 * The lyrics and the full screen are one layout that changes shape: the cover shrinks away, the
 * lyrics grow to fill the screen, and the panel folds down to the progress and three buttons. It
 * is animated with springs and runs in reverse when the lyrics are closed. Songs without lyrics
 * keep the large cover.
 *
 * The app shell opens it from the mini player and knows nothing about the layout: it passes the
 * modifiers that animate that transition.
 *
 * @param modifier The whole screen; the shell grows it out of the mini player.
 * @param artworkModifier The cover, which moves from the mini player into place.
 * @param revealModifier The panel, which appears after the cover.
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
    val lyricsState by viewModel.lyrics.collectAsStateWithLifecycle()
    // The account may have changed since the screen was last open (another device, another app).
    LaunchedEffect(Unit) { viewModel.refreshLike() }
    val track = state.currentTrack
    if (track == null) {
        MessageState(stringResource(R.string.feature_player_nothing))
        return
    }
    val motion = AuralisTheme.motion
    val spacing = AuralisTheme.spacing
    val lyrics = (lyricsState as? LyricsUiState.Shown)?.lyrics

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

    var expanded by rememberSaveable { mutableStateOf(false) }
    // A song without lyrics has nothing to show full screen.
    LaunchedEffect(lyrics == null) { if (lyrics == null) expanded = false }
    // Registered after the shell's handler, so it is asked first: Back closes the lyrics, not the player.
    BackHandler(enabled = expanded) { expanded = false }
    // How far the lyrics are open (0 live, 1 full screen) and how much the screen has become a
    // lyrics screen at all (0 large cover, 1 small cover with lyrics). Springs, so they can be
    // interrupted and reversed; with reduced motion they jump.
    val expand = animateFloatAsState(if (expanded) 1f else 0f, motion.standard(), label = "lyrics expand").value.coerceIn(0f, 1f)
    // The springs overshoot a little; sizes and padding derived from them must never go negative.
    val lyricsMode = animateFloatAsState(if (lyrics != null) 1f else 0f, motion.standard(), label = "lyrics mode").value.coerceIn(0f, 1f)

    GlassLayers(
        modifier = modifier.fillMaxSize(),
        overlay = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = spacing.screenGutter),
            ) {
                // The top bar and the lyrics header share one row and cross-fade.
                Box(modifier = Modifier.fillMaxWidth().height(HeaderHeight)) {
                    if (expand < 0.999f) {
                        TopBar(
                            onCollapse = onCollapse,
                            like = like,
                            onToggleLike = viewModel::toggleLike,
                            onShare = { sharing = true },
                            modifier = Modifier.graphicsLayer { alpha = 1f - expand },
                        )
                    }
                    if (expand > 0.001f) {
                        LyricsHeader(
                            track = track,
                            onClose = { expanded = false },
                            modifier = Modifier.graphicsLayer { alpha = expand },
                        )
                    }
                }
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val middleWidth = maxWidth
                    val failed = lyricsState is LyricsUiState.Failed
                    val retryHeight = if (failed) RetryRowHeight else 0.dp
                    // The cover's band: the whole middle for a song without lyrics, a small cover
                    // when there are lyrics, nothing when they are full screen.
                    val topGap = spacing.xl * lyricsMode
                    val liveBand = CoverSize + topGap + spacing.lg
                    val coverBand = lerp(lerp(maxHeight - retryHeight, liveBand, lyricsMode), 0.dp, expand)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(coverBand).padding(top = topGap),
                            contentAlignment = Alignment.Center,
                        ) {
                            val side = minOf(middleWidth * 0.86f, coverBand - topGap - spacing.sm, 420.dp)
                            if (side > spacing.sm) {
                                GlowingArtwork(
                                    uri = track.artworkUri,
                                    contentDescription = stringResource(R.string.feature_player_artwork),
                                    shape = AuralisTheme.shapes.large,
                                    modifier = artworkModifier.size(side),
                                )
                            }
                        }
                        if (lyrics != null) {
                            LyricsStage(
                                lyrics = lyrics,
                                positionMs = state.positionMs,
                                isPlaying = state.isPlaying,
                                expand = expand,
                                expanded = expanded,
                                onSeek = viewModel::seekTo,
                                onExpand = { expanded = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = lyricsMode },
                            )
                        } else if (failed) {
                            Box(Modifier.fillMaxWidth().height(retryHeight), contentAlignment = Alignment.Center) {
                                GlassButton(
                                    text = stringResource(R.string.feature_player_lyrics_failed_retry),
                                    icon = AuralisIcons.Retry,
                                    onClick = viewModel::retryLyrics,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(spacing.md))
                Box(modifier = revealModifier) {
                    PlayerPanel(track = track, state = state, like = like, expanded = expanded, viewModel = viewModel)
                }
                Spacer(Modifier.height(spacing.lg))
            }
        },
    ) {
        AmbientBackdrop(uri = track.artworkUri, vivid = true)
    }
}

@Composable
private fun TopBar(
    onCollapse: () -> Unit,
    like: LikeUiState,
    onToggleLike: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        GlassIconButton(
            icon = AuralisIcons.Collapse,
            contentDescription = stringResource(R.string.feature_player_collapse),
            onClick = onCollapse,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = stringResource(R.string.feature_player_now_playing).toUpperCase(Locale.current),
            style = AuralisTheme.typography.labelSmall,
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

/** What is playing, and the way back, over the full-screen lyrics. */
@Composable
private fun LyricsHeader(track: MusicTrack, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = AuralisTheme.spacing
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Artwork(uri = track.artworkUri, contentDescription = null, shape = AuralisTheme.shapes.small, modifier = Modifier.size(HeaderCoverSize))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = AuralisTheme.typography.itemTitle,
                color = AuralisTheme.colors.contentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artistNames,
                style = AuralisTheme.typography.itemSubtitle,
                color = AuralisTheme.colors.contentSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GlassIconButton(
            icon = AuralisIcons.Shrink,
            contentDescription = stringResource(R.string.feature_player_close_lyrics),
            onClick = onClose,
        )
    }
}

/**
 * The player: title, artist, progress and controls on one glass panel, with no cover thumbnail
 * (the cover is the screen's background). With the lyrics full screen it folds down to the
 * progress and previous, play and next.
 */
@Composable
private fun PlayerPanel(
    track: MusicTrack,
    state: PlayerState,
    like: LikeUiState,
    expanded: Boolean,
    viewModel: NowPlayingViewModel,
) {
    val motion = AuralisTheme.motion
    val spacing = AuralisTheme.spacing
    // While dragging, the slider follows the finger; the player seeks once on release.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(0)
    val fraction = dragFraction ?: if (duration > 0) state.positionMs.toFloat() / duration else 0f
    val enter = fadeIn(motion.fade())
    val exit = fadeOut(motion.fade())
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = AuralisTheme.shapes.extraLarge,
        style = GlassStyle.Floating,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            AnimatedVisibility(
                visible = !expanded,
                enter = enter + expandVertically(motion.standard<IntSize>()),
                exit = exit + shrinkVertically(motion.standard<IntSize>()),
            ) {
                Column(modifier = Modifier.padding(bottom = spacing.sm)) {
                    Text(
                        text = track.title,
                        style = AuralisTheme.typography.nowPlayingTitle,
                        color = AuralisTheme.colors.contentPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = track.artistNames,
                        style = AuralisTheme.typography.body,
                        color = AuralisTheme.colors.contentSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                    if (like.failed) {
                        Text(
                            text = stringResource(R.string.feature_player_like_failed),
                            style = AuralisTheme.typography.bodySmall,
                            color = AuralisTheme.colors.contentSecondary,
                            modifier = Modifier.padding(top = spacing.xs),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text(
                    text = formatDuration((fraction * duration).toLong()),
                    style = AuralisTheme.typography.timestamp,
                    color = AuralisTheme.colors.contentPrimary,
                )
                GlassSlider(
                    value = fraction,
                    onValueChange = { dragFraction = it },
                    onValueChangeFinished = {
                        dragFraction?.let { viewModel.seekTo((it * duration).toLong()) }
                        dragFraction = null
                    },
                    contentDescription = stringResource(R.string.feature_player_seek),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (duration > 0) formatDuration(duration) else "--:--",
                    style = AuralisTheme.typography.timestamp,
                    color = AuralisTheme.colors.contentPrimary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shuffle and repeat fold away with the lyrics full screen, and the row re-centres.
                AnimatedVisibility(
                    visible = !expanded,
                    enter = enter + expandHorizontally(motion.standard<IntSize>()),
                    exit = exit + shrinkHorizontally(motion.standard<IntSize>()),
                ) {
                    TransportButton(
                        icon = AuralisIcons.Shuffle,
                        contentDescription = stringResource(R.string.feature_player_shuffle),
                        onClick = viewModel::toggleShuffle,
                        selected = state.shuffleEnabled,
                    )
                }
                TransportButton(
                    icon = AuralisIcons.SkipPrevious,
                    contentDescription = stringResource(R.string.feature_player_previous),
                    onClick = viewModel::previous,
                    size = 56.dp,
                    iconSize = 34.dp,
                )
                TransportButton(
                    icon = if (state.isPlaying) AuralisIcons.Pause else AuralisIcons.Play,
                    contentDescription = stringResource(if (state.isPlaying) R.string.feature_player_pause else R.string.feature_player_play),
                    onClick = viewModel::togglePlayPause,
                    size = 72.dp,
                    iconSize = 48.dp,
                )
                TransportButton(
                    icon = AuralisIcons.SkipNext,
                    contentDescription = stringResource(R.string.feature_player_next),
                    onClick = viewModel::next,
                    enabled = state.currentIndex < state.queue.lastIndex || state.repeatMode != RepeatMode.OFF,
                    size = 56.dp,
                    iconSize = 34.dp,
                )
                AnimatedVisibility(
                    visible = !expanded,
                    enter = enter + expandHorizontally(motion.standard<IntSize>()),
                    exit = exit + shrinkHorizontally(motion.standard<IntSize>()),
                ) {
                    TransportButton(
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
}

/**
 * A control on the panel: a bare icon, as on the reference, with no circle of its own (the panel
 * is the glass). On means a soft fill behind the icon, not a colour (spec 8.2.1).
 */
@Composable
private fun TransportButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp,
) {
    val colors = AuralisTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (selected) Modifier.background(colors.selection) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) colors.contentPrimary else colors.contentDisabled,
            modifier = Modifier.size(iconSize),
        )
    }
}

private val HeaderHeight = 56.dp
private val CoverSize = 150.dp
private val HeaderCoverSize = 44.dp
private val RetryRowHeight = 56.dp
