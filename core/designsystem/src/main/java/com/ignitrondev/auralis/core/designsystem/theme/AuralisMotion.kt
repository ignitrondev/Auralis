/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Every spring, duration and easing in Auralis (spec section 8.3). Feature modules take their
 * animation specs from here and never use magic numbers.
 *
 * When [reducedMotion] is on (the system "Remove animations" setting), movement is replaced by
 * an instant change and visibility changes become short fades.
 */
@Immutable
class AuralisMotion(val reducedMotion: Boolean) {

    /** Default spring for most UI movement: mini player, sheets, selection changes. */
    fun <T> standard(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else spring(dampingRatio = 0.85f, stiffness = 380f)

    /** Livelier spring for large moves, such as the artwork transition to Now Playing. */
    fun <T> emphasized(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)

    /** Press feedback: quick to scale down, slightly springy on release. */
    fun <T> press(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else spring(dampingRatio = 0.6f, stiffness = 700f)

    /** Short tween for color and opacity changes. Kept even with reduced motion. */
    fun <T> fade(): FiniteAnimationSpec<T> = tween(durationMillis = DURATION_SHORT_MS)

    /** Fade used to show elements. */
    val enter: EnterTransition get() = fadeIn(tween(DURATION_MEDIUM_MS))

    /** Fade used to hide elements. */
    val exit: ExitTransition get() = fadeOut(tween(DURATION_SHORT_MS))

    /**
     * How an element shared between two layouts moves and resizes, such as the mini player growing
     * into Now Playing. Instant with reduced motion; the two layouts then simply cross-fade.
     */
    val sharedBounds: BoundsTransform
        get() = BoundsTransform { _, _ ->
            if (reducedMotion) snap() else spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
        }

    /** Content that appears once a larger transition has settled, such as Now Playing's controls. */
    val reveal: EnterTransition
        get() = if (reducedMotion) {
            fadeIn(tween(DURATION_SHORT_MS))
        } else {
            fadeIn(tween(DURATION_MEDIUM_MS, delayMillis = DURATION_SHORT_MS)) +
                slideInVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) { it / REVEAL_OFFSET_DIVISOR }
        }

    /** The reverse of [reveal]: gone quickly, before the larger transition runs back. */
    val conceal: ExitTransition
        get() = fadeOut(tween(DURATION_SHORT_MS)) +
            if (reducedMotion) ExitTransition.None else slideOutVertically(tween(DURATION_SHORT_MS)) { it / REVEAL_OFFSET_DIVISOR }

    /** Floating chrome (the navigation bar) leaving or returning along the bottom edge. */
    val chromeEnter: EnterTransition
        get() = if (reducedMotion) enter else fadeIn(tween(DURATION_MEDIUM_MS)) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) { it }

    val chromeExit: ExitTransition
        get() = if (reducedMotion) exit else fadeOut(tween(DURATION_SHORT_MS)) + slideOutVertically(tween(DURATION_MEDIUM_MS)) { it }

    /** Scale a pressed control shrinks to before springing back. */
    val pressedScale: Float get() = if (reducedMotion) 1f else PRESSED_SCALE

    companion object {
        const val DURATION_SHORT_MS = 120
        const val DURATION_MEDIUM_MS = 240
        const val DURATION_LONG_MS = 360
        private const val PRESSED_SCALE = 0.96f
        private const val REVEAL_OFFSET_DIVISOR = 6
    }
}

/** Reads the system animator duration scale. 0 means the user turned animations off. */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

internal val LocalAuralisMotion = staticCompositionLocalOf { AuralisMotion(reducedMotion = false) }
