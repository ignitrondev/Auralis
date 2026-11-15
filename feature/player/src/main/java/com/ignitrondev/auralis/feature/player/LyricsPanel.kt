/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.domain.model.Lyrics

/**
 * Lyrics in place of the artwork (issue #34). Synced lyrics highlight the line being sung by
 * opacity and scale (never colour, spec 8.2.1), follow playback, and seek when a line is tapped.
 * Lyrics without timing are plain text. The list can always be scrolled by hand; following
 * playback pauses for a few seconds after the user drags it.
 */
@Composable
internal fun LyricsPanel(
    state: LyricsUiState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {
            LyricsUiState.Loading -> CircularProgressIndicator(
                color = AuralisTheme.colors.contentSecondary,
                trackColor = AuralisTheme.colors.selection,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp),
            )
            LyricsUiState.Unavailable -> PanelMessage(stringResource(R.string.feature_player_lyrics_unavailable))
            LyricsUiState.Failed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.md),
            ) {
                PanelMessage(stringResource(R.string.feature_player_lyrics_failed))
                GlassButton(text = stringResource(R.string.feature_player_lyrics_retry), icon = AuralisIcons.Retry, onClick = onRetry)
            }
            is LyricsUiState.Shown -> LyricsLines(lyrics = state.lyrics, positionMs = positionMs, onSeek = onSeek)
        }
    }
}

@Composable
private fun PanelMessage(text: String) {
    Text(
        text = text,
        style = AuralisTheme.typography.body,
        color = AuralisTheme.colors.contentSecondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LyricsLines(lyrics: Lyrics, positionMs: Long, onSeek: (Long) -> Unit) {
    val motion = AuralisTheme.motion
    val spacing = AuralisTheme.spacing
    val list = rememberLazyListState()
    val active = lyrics.activeLineIndex(positionMs)
    val dragged by list.interactionSource.collectIsDraggedAsState()
    var lastDragAtMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(dragged) { if (dragged) lastDragAtMs = System.currentTimeMillis() }
    // Keep the active line about a third of the way down, unless the user just scrolled.
    LaunchedEffect(active) {
        if (active < 0 || dragged || System.currentTimeMillis() - lastDragAtMs < FOLLOW_PAUSE_MS) return@LaunchedEffect
        val offset = -list.layoutInfo.viewportSize.height / 3
        if (motion.reducedMotion) list.scrollToItem(active, offset) else list.animateScrollToItem(active, offset)
    }
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isActive = index == active
            val alpha by animateFloatAsState(
                targetValue = when {
                    !lyrics.isSynced -> 1f
                    isActive -> 1f
                    index < active -> PAST_ALPHA
                    else -> UPCOMING_ALPHA
                },
                animationSpec = motion.fade(),
                label = "lyric alpha",
            )
            val scale by animateFloatAsState(
                targetValue = if (!lyrics.isSynced || isActive) 1f else INACTIVE_SCALE,
                animationSpec = motion.standard(),
                label = "lyric scale",
            )
            val start = line.startMs
            Text(
                // A blank line is an instrumental break.
                text = line.text.ifBlank { BREAK },
                style = if (lyrics.isSynced) AuralisTheme.typography.sectionTitle else AuralisTheme.typography.body,
                color = AuralisTheme.colors.contentPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .then(
                        if (start != null) {
                            Modifier.clickable(role = Role.Button, onClickLabel = null) { onSeek(start) }
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = spacing.xxs),
            )
        }
        lyrics.source?.let { source ->
            item(key = "source") {
                Text(
                    text = source,
                    style = AuralisTheme.typography.bodySmall,
                    color = AuralisTheme.colors.contentTertiary,
                    modifier = Modifier.padding(top = spacing.lg),
                )
            }
        }
    }
}

private const val FOLLOW_PAUSE_MS = 3_000L
private const val PAST_ALPHA = 0.45f
private const val UPCOMING_ALPHA = 0.6f
private const val INACTIVE_SCALE = 0.94f
private const val BREAK = "♪"
