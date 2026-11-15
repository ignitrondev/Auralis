/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.domain.model.Lyrics

/**
 * How one lyric line looks.
 *
 * @property alpha Opacity: the line being sung is 1 and the rest fade with their distance from it.
 * @property sizeSp Font size, larger for the line being sung.
 */
internal data class LineLook(val alpha: Float, val sizeSp: Float)

/**
 * The look of a lyric line [distance] lines from the one being sung (0 is the line being sung),
 * for [expand] from 0 (live, a few centred lines under the cover) to 1 (full screen, large text).
 * Unsynced lyrics have no line being sung: every line is the same. Opacity and size, never colour,
 * tell the lines apart (spec 8.2.1). The two looks are blended, so the change is a continuous
 * morph rather than a jump.
 */
internal fun lineLook(distance: Int, expand: Float, synced: Boolean): LineLook {
    if (!synced) return LineLook(alpha = 0.88f, sizeSp = lerp(LIVE_PLAIN_SP, FULL_OTHER_SP, expand))
    val liveAlpha = when (kotlin.math.abs(distance)) {
        0 -> 1f
        1 -> if (distance < 0) 0.55f else 0.6f
        2 -> 0.32f
        else -> 0.14f
    }
    val liveSize = when (kotlin.math.abs(distance)) {
        0 -> LIVE_ACTIVE_SP
        1 -> 18f
        else -> 16f
    }
    // Full screen: lines still to come are brighter than the ones already sung.
    val fullAlpha = when {
        distance == 0 -> 1f
        distance > 0 -> (0.74f - (distance - 1) * 0.06f).coerceAtLeast(0.5f)
        else -> (0.4f + distance * 0.05f).coerceAtLeast(0.22f)
    }
    val fullSize = if (distance == 0) FULL_ACTIVE_SP else FULL_OTHER_SP
    return LineLook(alpha = lerp(liveAlpha, fullAlpha, expand), sizeSp = lerp(liveSize, fullSize, expand))
}

/**
 * The lyrics, in one list that changes shape. With [expand] at 0 it is a few centred lines under
 * the cover, not scrollable, and tapping it opens the full screen; at 1 it is a large, left-aligned,
 * scrollable list. Everything between is a blend, so the screen morphs as [expand] is animated.
 *
 * Playing follows the line being sung (centred live, a little above the middle full screen); while
 * the user scrolls, and for a few seconds after, it stays where they put it. Tapping a line full
 * screen seeks to it. The two ends fade out so no line is cut off hard.
 */
@Composable
internal fun LyricsStage(
    lyrics: Lyrics,
    positionMs: Long,
    expand: Float,
    expanded: Boolean,
    onSeek: (Long) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = AuralisTheme.motion
    val spacing = AuralisTheme.spacing
    val density = LocalDensity.current
    val list = rememberLazyListState()
    val active = lyrics.activeLineIndex(positionMs)
    val dragged by list.interactionSource.collectIsDraggedAsState()
    var lastTouchAtMs by remember { mutableLongStateOf(0L) }
    // Counts up when the pause after the user's last touch is over, so the list follows playback again.
    var resume by remember { mutableIntStateOf(0) }
    // Both when a finger lands and when it lifts, so the pause is counted from the end of the scroll.
    LaunchedEffect(dragged) {
        lastTouchAtMs = System.currentTimeMillis()
        if (!dragged) {
            delay(FOLLOW_PAUSE_MS)
            resume++
        }
    }
    val currentExpand by rememberUpdatedState(expand)
    // Where the focused line sits in the viewport, 0 at the top. Plain lyrics read from the top;
    // synced ones follow the line being sung, a little higher full screen.
    fun focusFraction(): Float = when {
        !lyrics.isSynced -> UNSYNCED_FOCUS
        else -> lerp(LIVE_FOCUS, FULL_FOCUS, currentExpand)
    }
    suspend fun focus(animate: Boolean) {
        val target = active.coerceAtLeast(0)
        val viewport = list.layoutInfo.viewportSize.height
        val itemSize = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == target }?.size ?: with(density) { 56.dp.roundToPx() }
        val offset = -(viewport * focusFraction() - itemSize / 2f).toInt()
        if (animate && !motion.reducedMotion) list.animateScrollToItem(target, offset) else list.scrollToItem(target, offset)
    }
    // While the screen opens or closes its height changes every frame; keep the focused line pinned
    // so the text moves with it instead of jumping when the morph ends.
    LaunchedEffect(active) {
        snapshotFlow { currentExpand }.collect { value ->
            if (value > 0.001f && value < 0.999f && !dragged) focus(animate = false)
        }
    }
    LaunchedEffect(active, expanded, resume) {
        if (dragged || (expanded && System.currentTimeMillis() - lastTouchAtMs < FOLLOW_PAUSE_MS)) return@LaunchedEffect
        // A settled list is moved with animation; the morph itself is handled above.
        val settled = currentExpand <= 0.001f || currentExpand >= 0.999f
        if (settled) focus(animate = true)
    }
    val openLabel = stringResource(R.string.feature_player_open_lyrics)
    Box(
        modifier = modifier
            .fadingEdges()
            .then(if (!expanded) Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = onExpand) else Modifier),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = expanded,
                // Half a screen above and below, so the first and last lines can reach the focus line.
                contentPadding = PaddingValues(
                    // Plain lyrics read from the top, so they start near it instead of mid-screen.
                    top = if (lyrics.isSynced) maxHeight / 2 else maxHeight * UNSYNCED_TOP,
                    bottom = maxHeight / 2,
                ),
                verticalArrangement = Arrangement.spacedBy(lerpDp(LIVE_GAP, FULL_GAP, expand)),
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val look = lineLook(if (lyrics.isSynced) index - active else 0, expand, lyrics.isSynced)
                    val start = line.startMs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = lerpDp(spacing.sm, spacing.lg, expand)),
                        // Centred live, left aligned full screen, and every step in between.
                        contentAlignment = BiasAlignment(horizontalBias = lerp(0f, -1f, expand), verticalBias = 0f),
                    ) {
                        Text(
                            // A blank line is an instrumental break.
                            text = line.text.ifBlank { BREAK },
                            style = AuralisTheme.typography.nowPlayingTitle.copy(
                                fontSize = look.sizeSp.sp,
                                lineHeight = (look.sizeSp * 1.2f).sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = AuralisTheme.colors.contentPrimary,
                            textAlign = if (expand < 0.5f) TextAlign.Center else TextAlign.Start,
                            modifier = Modifier
                                .graphicsLayer { alpha = look.alpha }
                                .then(if (expanded && start != null) Modifier.clickable(role = Role.Button) { onSeek(start) } else Modifier),
                        )
                    }
                }
            }
        }
        // Opens the full screen. It fades out as the screen opens.
        if (expand < 0.999f) {
            GlassIconButton(
                icon = AuralisIcons.Expand,
                contentDescription = openLabel,
                onClick = onExpand,
                enabled = !expanded,
                size = ExpandButtonSize,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = spacing.xs)
                    .graphicsLayer { alpha = 1f - expand },
            )
        }
    }
}

/** Fades the top and bottom edges to nothing, so lines scrolling out are never cut hard. */
private fun Modifier.fadingEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                EDGE_FADE to Color.Black,
                1f - EDGE_FADE to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

private val LIVE_GAP = 10.dp
private val FULL_GAP = 22.dp
private val ExpandButtonSize = 40.dp
private const val LIVE_PLAIN_SP = 18f
private const val LIVE_ACTIVE_SP = 26f
private const val FULL_ACTIVE_SP = 34f
private const val FULL_OTHER_SP = 30f
private const val LIVE_FOCUS = 0.5f
private const val FULL_FOCUS = 0.38f
private const val UNSYNCED_FOCUS = 0.17f
private const val UNSYNCED_TOP = 0.1f
private const val EDGE_FADE = 0.1f
private const val FOLLOW_PAUSE_MS = 3_000L
private const val BREAK = "♪"
