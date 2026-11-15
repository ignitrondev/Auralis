/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberUpdatedState
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
import com.ignitrondev.auralis.core.designsystem.theme.AuralisMotion
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.domain.model.Lyrics
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor

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
 * The look for a [distance] between two lines, so a line can glide from one look to the next when
 * the line being sung changes instead of switching at once.
 */
internal fun lineLook(distance: Float, expand: Float, synced: Boolean): LineLook {
    val below = floor(distance).toInt()
    val from = lineLook(below, expand, synced)
    val part = distance - below
    if (!synced || part == 0f) return from
    val to = lineLook(below + 1, expand, synced)
    return LineLook(alpha = lerp(from.alpha, to.alpha, part), sizeSp = lerp(from.sizeSp, to.sizeSp, part))
}

/**
 * How far the list must scroll (positive is forward) to put the middle of an item at [fraction] of
 * the viewport, 0 at its top and 1 at its bottom.
 *
 * The offsets are the list's own: an item's offset is measured from the end of the top content
 * padding, and the viewport starts above that, at a negative offset. Leaving the padding out puts
 * the line half a screen too low.
 */
internal fun focusDelta(itemOffset: Int, itemSize: Int, viewportStart: Int, viewportEnd: Int, fraction: Float): Float =
    itemOffset + itemSize / 2f - (viewportStart + (viewportEnd - viewportStart) * fraction)

/**
 * The playback position, advanced between the player's updates. The player reports twice a second,
 * which is too coarse to change lines on time; while playing, this counts on from the last report.
 */
@Composable
private fun rememberLivePosition(positionMs: Long, isPlaying: Boolean): LongState {
    val live = remember { mutableLongStateOf(positionMs) }
    LaunchedEffect(positionMs, isPlaying) {
        live.longValue = positionMs
        if (!isPlaying) return@LaunchedEffect
        val reportedAt = SystemClock.elapsedRealtime()
        while (true) {
            delay(LIVE_TICK_MS)
            live.longValue = positionMs + (SystemClock.elapsedRealtime() - reportedAt)
        }
    }
    return live
}

/**
 * The lyrics, in one list that changes shape. With [expand] at 0 it is a few centred lines under
 * the cover, not scrollable, and tapping it opens the full screen; at 1 it is a large, left-aligned,
 * scrollable list. Everything between is a blend, so the screen morphs as [expand] is animated.
 *
 * Synced lyrics follow the line being sung (centred live, a little above the middle full screen):
 * the list glides to it and the lines fade and resize into their new looks. While the user scrolls,
 * and for a few seconds after, the list stays where they put it. Plain lyrics are never moved.
 * Tapping a line full screen seeks to it. The two ends fade out so no line is cut off hard.
 */
@Composable
internal fun LyricsStage(
    lyrics: Lyrics,
    positionMs: Long,
    isPlaying: Boolean,
    expand: Float,
    expanded: Boolean,
    onSeek: (Long) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = AuralisTheme.motion
    val spacing = AuralisTheme.spacing
    // A new song starts from the top, not from where the last one was.
    val list = remember(lyrics) { LazyListState() }
    val livePosition = rememberLivePosition(positionMs, isPlaying)
    // Read through derived state, so the screen recomposes when the line changes, not on every tick.
    val active by remember(lyrics) { derivedStateOf { lyrics.activeLineIndex(livePosition.longValue + LEAD_MS) } }
    val dragged by list.interactionSource.collectIsDraggedAsState()
    // Until when the list is left where the user scrolled it.
    var pausedUntilMs by remember(lyrics) { mutableLongStateOf(0L) }
    // Counts up when that pause is over, so the list follows playback again.
    var resume by remember { mutableIntStateOf(0) }
    LaunchedEffect(dragged) {
        if (dragged) {
            pausedUntilMs = Long.MAX_VALUE
        } else if (pausedUntilMs == Long.MAX_VALUE) {
            // Counted from when the finger lifts, so a long scroll is not undone.
            pausedUntilMs = System.currentTimeMillis() + FOLLOW_PAUSE_MS
            delay(FOLLOW_PAUSE_MS)
            resume++
        }
    }
    val currentExpand by rememberUpdatedState(expand)
    // Keeps the line being sung at its place in the viewport. It runs frame by frame for as long as
    // something is moving: the line changed, the screen is opening or closing (its height changes
    // every frame), or the lines are still resizing. Each frame it closes part of the gap, which
    // reads as a glide; during the morph it closes all of it, so the text moves with the screen.
    LaunchedEffect(lyrics, active, expanded, resume) {
        if (!lyrics.isSynced) return@LaunchedEffect
        if (expanded && System.currentTimeMillis() < pausedUntilMs) return@LaunchedEffect
        val target = active.coerceAtLeast(0)
        var jumped = false
        var lastFrame = 0L
        var calmSince = 0L
        while (true) {
            val frame = withFrameNanos { it }
            if (dragged) return@LaunchedEffect
            val elapsedMs = if (lastFrame == 0L) 16f else (frame - lastFrame) / 1_000_000f
            lastFrame = frame
            val info = list.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == target }
            if (item == null) {
                // Not on screen (first frame, or a seek): jump next to it and settle from there.
                list.scrollToItem(target)
                jumped = true
                continue
            }
            val morphing = currentExpand > 0.001f && currentExpand < 0.999f
            val fraction = lerp(LIVE_FOCUS, FULL_FOCUS, currentExpand)
            val delta = focusDelta(item.offset, item.size, info.viewportStartOffset, info.viewportEndOffset, fraction)
            val share = if (jumped || morphing || motion.reducedMotion) 1f else 1f - exp(-elapsedMs / GLIDE_MS)
            // The ends of the list cannot scroll further: nothing consumed counts as arrived.
            val moved = if (abs(delta) > 0.5f) abs(list.scrollBy(delta * share)) > 0.01f else false
            if (abs(delta) <= 0.5f) jumped = false
            if (moved || morphing) {
                calmSince = 0L
            } else {
                if (calmSince == 0L) calmSince = frame
                if ((frame - calmSince) / 1_000_000 >= SETTLE_MS) return@LaunchedEffect
            }
        }
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
                    // The distance glides when the line being sung changes, and the look with it.
                    val distance by animateFloatAsState(
                        targetValue = if (lyrics.isSynced) (index - active).toFloat() else 0f,
                        animationSpec = if (motion.reducedMotion) snap() else tween(AuralisMotion.DURATION_LONG_MS),
                        label = "lyric line",
                    )
                    val look = lineLook(distance, expand, lyrics.isSynced)
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
private const val UNSYNCED_TOP = 0.1f
private const val EDGE_FADE = 0.1f
private const val FOLLOW_PAUSE_MS = 3_000L
private const val LIVE_TICK_MS = 50L
// A line lights up a little before it is sung, so it has arrived by the time it is heard.
private const val LEAD_MS = 200L
private const val GLIDE_MS = 110f
private const val SETTLE_MS = 450L
private const val BREAK = "♪"
