/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.player

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.domain.model.Lyrics
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * How one lyric line looks.
 *
 * @property alpha Opacity: the line being sung is 1 and the rest fade with their distance from it.
 * @property sizeSp Apparent font size, larger for the line being sung. Every line is laid out at
 *   the size of the line being sung and scaled down to this, so a line changing look never
 *   re-wraps or moves its neighbours.
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
        1 -> 19f
        else -> 17f
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
 * The lyrics, in one list that changes shape. With [expand] at 0 it is a few small centred lines
 * under the cover, not scrollable, and tapping it opens the full screen; at 1 it is a large,
 * left-aligned, scrollable list. Everything between is a blend, so the screen morphs as [expand]
 * is animated.
 *
 * The list is laid out once, left aligned and at full-screen size, and fills the whole stage. The live look is
 * that same layout drawn smaller and lower, behind a mask that hides what the cover and the player
 * panel cover. So opening and closing only re-draw layers: [expand] is read while drawing, never
 * while composing or measuring, and no text is laid out again.
 *
 * Synced lyrics follow the line being sung: the list is pulled to it by a spring and the lines
 * fade and scale into their new looks. While the user scrolls, and for a few seconds after, the
 * list stays where they put it. Plain lyrics are never moved. Tapping a line full screen seeks to
 * it. The two ends fade out so no line is cut off hard.
 *
 * @param expand How far the lyrics are open, 0 to 1. Read while drawing.
 * @param topInset Height in px of the cover's band over the top of the stage, while live.
 * @param bottomInset Height in px that the player panel covers at the bottom. Read while drawing.
 */
@Composable
internal fun LyricsStage(
    lyrics: Lyrics,
    positionMs: Long,
    isPlaying: Boolean,
    expand: () -> Float,
    expanded: Boolean,
    topInset: Float,
    bottomInset: () -> Float,
    onSeek: (Long) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = AuralisTheme.motion
    val spacing = AuralisTheme.spacing
    val synced = lyrics.isSynced
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
    val currentBottomInset by rememberUpdatedState(bottomInset)
    // Speed of the follow, in px/s. It outlives a line change, so a new line picks up from the
    // speed the list already has instead of starting again from a stop.
    val velocity = remember(lyrics) { FloatArray(1) }
    // Keeps the line being sung on the list's focus line. It runs frame by frame while the list is
    // on its way there, pulled by a spring with no bounce, which eases in and out.
    LaunchedEffect(lyrics, active, expanded, resume) {
        if (!synced) return@LaunchedEffect
        if (expanded && System.currentTimeMillis() < pausedUntilMs) return@LaunchedEffect
        val target = active.coerceAtLeast(0)
        var jumped = false
        var lastFrame = 0L
        var calmSince = 0L
        while (true) {
            val frame = withFrameNanos { it }
            if (dragged) {
                velocity[0] = 0f
                return@LaunchedEffect
            }
            // A dropped frame must not turn into a leap.
            val seconds = if (lastFrame == 0L) FRAME_S else ((frame - lastFrame) / 1_000_000_000f).coerceAtMost(2 * FRAME_S)
            lastFrame = frame
            val info = list.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == target }
            if (item == null) {
                // Not on screen (first frame, or a seek): jump next to it and settle from there.
                list.scrollToItem(target)
                jumped = true
                velocity[0] = 0f
                continue
            }
            val delta = focusDelta(item.offset, item.size, info.viewportStartOffset, info.viewportEndOffset, LIST_FOCUS)
            val step = if (jumped || motion.reducedMotion) {
                velocity[0] = 0f
                delta
            } else {
                velocity[0] += (FOLLOW_OMEGA * FOLLOW_OMEGA * delta - 2f * FOLLOW_OMEGA * velocity[0]) * seconds
                velocity[0] * seconds
            }
            if (abs(delta) <= 0.5f) jumped = false
            var resting = abs(delta) <= 0.5f && abs(velocity[0]) < REST_SPEED
            if (!resting) {
                // The ends of the list cannot scroll further: nothing consumed counts as arrived.
                val blocked = abs(step) > 0.5f && abs(list.scrollBy(step)) < 0.01f
                if (blocked) {
                    velocity[0] = 0f
                    resting = true
                }
            }
            if (!resting) {
                calmSince = 0L
            } else {
                if (calmSince == 0L) calmSince = frame
                if ((frame - calmSince) / 1_000_000 >= SETTLE_MS) return@LaunchedEffect
            }
        }
    }
    // Every line is laid out at this size, the size of the line being sung full screen. A line's
    // look, and the smaller live text, are scale and opacity on a layer.
    val baseSp = lineLook(0, 1f, synced).sizeSp
    val fadePx = with(LocalDensity.current) { EdgeFade.toPx() }
    val showOpenButton by remember { derivedStateOf { currentExpand() < 0.999f } }
    val openLabel = stringResource(R.string.feature_player_open_lyrics)
    BoxWithConstraints(modifier = modifier) {
        // The point of the list that stays under the eye while the stage changes shape, as a
        // fraction of its height: the focus line for synced lyrics, the first line for plain ones.
        val anchor = if (synced) LIST_FOCUS else PLAIN_TOP
        val stageHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fadingEdges(
                    top = { lerp(topInset, 0f, currentExpand()) },
                    bottom = { currentBottomInset() },
                    fade = fadePx,
                )
                .then(if (!expanded) Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = onExpand) else Modifier),
        ) {
            LazyColumn(
                state = list,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val open = currentExpand()
                        val scale = lineLook(0, open, synced).sizeSp / baseSp
                        transformOrigin = TransformOrigin(0.5f, anchor)
                        scaleX = scale
                        scaleY = scale
                        // Live, the anchor sits in the middle of what shows between the cover and
                        // the panel (plain lyrics: just under the cover); full screen, where the
                        // list itself has it.
                        val liveY = if (synced) (topInset + size.height - currentBottomInset()) / 2f else topInset + fadePx
                        translationY = (liveY - size.height * anchor) * (1f - open)
                    },
                userScrollEnabled = expanded,
                // Room above and below, so the first and last lines can reach the focus line.
                contentPadding = PaddingValues(
                    top = stageHeight * anchor,
                    bottom = stageHeight * (if (synced) 1f - LIST_FOCUS else PLAIN_BOTTOM),
                ),
                verticalArrangement = Arrangement.spacedBy(LINE_GAP),
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    // The distance glides when the line being sung changes, and the look with it.
                    val distance = animateFloatAsState(
                        targetValue = if (synced) (index - active).toFloat() else 0f,
                        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = 1f, stiffness = FOLLOW_OMEGA * FOLLOW_OMEGA),
                        label = "lyric line",
                    )
                    val start = line.startMs
                    val textLayout = remember { arrayOfNulls<TextLayoutResult>(1) }
                    Text(
                        // A blank line is an instrumental break.
                        text = line.text.ifBlank { BREAK },
                        style = AuralisTheme.typography.nowPlayingTitle.copy(
                            fontSize = baseSp.sp,
                            lineHeight = (baseSp * 1.2f).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = AuralisTheme.colors.contentPrimary,
                        onTextLayout = { textLayout[0] = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.lg)
                            .graphicsLayer {
                                val open = currentExpand()
                                val look = lineLook(distance.value, open, synced)
                                alpha = look.alpha
                                scaleX = look.sizeSp / lineLook(0, open, synced).sizeSp
                                scaleY = scaleX
                                // Shrinks towards where the line is anchored: its middle live, its left edge full screen.
                                transformOrigin = TransformOrigin(lerp(0.5f, 0f, open), 0.5f)
                            }
                            .centredLines(layout = { textLayout[0] }, amount = { 1f - currentExpand() })
                            .then(if (expanded && start != null) Modifier.clickable(role = Role.Button) { onSeek(start) } else Modifier),
                    )
                }
            }
        }
        // Opens the full screen, from the corner under the cover. It fades out as the screen opens.
        if (showOpenButton) {
            GlassIconButton(
                icon = AuralisIcons.Expand,
                contentDescription = openLabel,
                onClick = onExpand,
                enabled = !expanded,
                size = OpenButtonSize,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, topInset.roundToInt()) }
                    .graphicsLayer { alpha = 1f - currentExpand() },
            )
        }
    }
}

/**
 * Draws left-aligned text with each of its lines moved towards the centre: [amount] 1 is centred
 * text, 0 leaves it left aligned. The text is laid out once, left aligned; the lines are moved
 * while drawing, so going between centred (live) and left aligned (full screen) is a slide with
 * no new layout.
 */
private fun Modifier.centredLines(layout: () -> TextLayoutResult?, amount: () -> Float): Modifier = drawWithContent {
    val result = layout()
    val centred = amount()
    if (result == null || centred <= 0f) {
        drawContent()
        return@drawWithContent
    }
    val last = result.lineCount - 1
    for (line in 0..last) {
        val shift = (size.width - (result.getLineRight(line) - result.getLineLeft(line))) / 2f * centred
        // One band per line; the first and last reach past the text so tall glyphs are not cut.
        val top = if (line == 0) -size.height else result.getLineTop(line)
        val bottom = if (line == last) 2 * size.height else result.getLineBottom(line)
        clipRect(left = -size.width, top = top, right = 2 * size.width, bottom = bottom) {
            translate(left = shift) { this@drawWithContent.drawContent() }
        }
    }
}

/**
 * Shows the content only between [top] px from the top and [bottom] px from the bottom, fading to
 * nothing over [fade] px at both ends, so lines scrolling out are never cut hard. Both are read
 * while drawing.
 */
private fun Modifier.fadingEdges(top: () -> Float, bottom: () -> Float, fade: Float): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val height = size.height
        if (height <= 0f) return@drawWithContent
        val from = top().coerceIn(0f, height)
        val to = (height - bottom()).coerceIn(from, height)
        val ramp = fade.coerceAtMost((to - from) / 2f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                from / height to Color.Transparent,
                (from + ramp) / height to Color.Black,
                (to - ramp) / height to Color.Black,
                to / height to Color.Transparent,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

private val LINE_GAP = 16.dp
private val OpenButtonSize = 40.dp
private val EdgeFade = 44.dp
private const val LIVE_PLAIN_SP = 18f
private const val LIVE_ACTIVE_SP = 24f
private const val FULL_ACTIVE_SP = 34f
private const val FULL_OTHER_SP = 30f
// Where the line being sung sits in the stage, 0 at its top. The stage runs on under the player
// panel, so this is a little above the middle of what shows full screen.
private const val LIST_FOCUS = 0.3f
private const val PLAIN_TOP = 0.05f
private const val PLAIN_BOTTOM = 0.4f
private const val FOLLOW_PAUSE_MS = 3_000L
private const val LIVE_TICK_MS = 50L
// A line lights up a little before it is sung, so it has arrived by the time it is heard.
private const val LEAD_MS = 200L
// Natural frequency of the follow spring (rad/s): about half a second to settle, no bounce.
private const val FOLLOW_OMEGA = 13f
private const val REST_SPEED = 8f
private const val FRAME_S = 0.016f
private const val SETTLE_MS = 300L
private const val BREAK = "♪"
