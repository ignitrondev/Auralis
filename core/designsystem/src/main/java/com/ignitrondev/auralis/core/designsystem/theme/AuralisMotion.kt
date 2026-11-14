/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

    /** Scale a pressed control shrinks to before springing back. */
    val pressedScale: Float get() = if (reducedMotion) 1f else PRESSED_SCALE

    companion object {
        const val DURATION_SHORT_MS = 120
        const val DURATION_MEDIUM_MS = 240
        const val DURATION_LONG_MS = 360
        private const val PRESSED_SCALE = 0.96f
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
