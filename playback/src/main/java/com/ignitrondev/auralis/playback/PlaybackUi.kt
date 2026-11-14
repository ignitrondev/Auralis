/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import android.app.PendingIntent

/** Supplied by the app: what opens when the user taps the media notification. */
fun interface PlaybackUi {
    fun sessionActivity(): PendingIntent
}
