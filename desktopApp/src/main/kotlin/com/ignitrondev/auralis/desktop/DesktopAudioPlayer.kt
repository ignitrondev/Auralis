/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.desktop

import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/** Small JavaFX audio adapter for the desktop target. Stream URLs are held in memory only. */
internal class DesktopAudioPlayer(
    private val onPlayingChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
) : AutoCloseable {
    private var mediaPlayer: MediaPlayer? = null

    fun play(url: String) {
        onFx {
            try {
                mediaPlayer?.dispose()
                val media = Media(url)
                val next = MediaPlayer(media)
                mediaPlayer = next
                media.setOnError { onError(media.error?.message ?: "Could not load this audio stream.") }
                next.setOnReady {
                    next.play()
                    onPlayingChanged(true)
                }
                next.setOnError {
                    onPlayingChanged(false)
                    onError(next.error?.message ?: "This audio format is not supported by the desktop player.")
                }
                next.setOnEndOfMedia { onPlayingChanged(false) }
            } catch (error: Exception) {
                onPlayingChanged(false)
                onError(error.message ?: "Could not start the desktop audio player.")
            }
        }
    }

    fun toggle() {
        onFx {
            val current = mediaPlayer ?: return@onFx
            if (current.status == MediaPlayer.Status.PLAYING) {
                current.pause()
                onPlayingChanged(false)
            } else {
                current.play()
                onPlayingChanged(true)
            }
        }
    }

    override fun close() {
        onFx {
            mediaPlayer?.dispose()
            mediaPlayer = null
            Platform.exit()
        }
    }

    private fun onFx(block: () -> Unit) {
        startJavaFx()
        if (Platform.isFxApplicationThread()) block() else Platform.runLater(block)
    }

    private companion object {
        val started = AtomicBoolean(false)

        fun startJavaFx() {
            if (!started.compareAndSet(false, true)) return
            val ready = CountDownLatch(1)
            Platform.startup {
                Platform.setImplicitExit(false)
                ready.countDown()
            }
            ready.await()
        }
    }
}
