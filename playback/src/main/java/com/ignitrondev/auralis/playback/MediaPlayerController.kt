/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.player.PlayerController
import com.ignitrondev.auralis.domain.player.PlayerState
import com.ignitrondev.auralis.domain.player.RepeatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PlayerController] backed by a Media3 [MediaController] connected to [PlaybackService]. Commands
 * issued before the connection is ready run once it is. Position is refreshed every
 * [POSITION_INTERVAL_MS] while playing (spec 7.3: no per-frame updates).
 */
@Singleton
class MediaPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errors: PlaybackErrors,
) : PlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller = CompletableDeferred<MediaController>()
    private val tracks = mutableMapOf<String, MusicTrack>()
    private var ticker: Job? = null
    private var connecting = false

    private val _state = MutableStateFlow(PlayerState.Idle)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    init {
        scope.launch { errors.current.collect { error -> _state.value = _state.value.copy(error = error) } }
        // Connect right away: the service restores the last queue, and the mini player shows it.
        scope.launch { connect() }
    }

    private fun withController(block: (MediaController) -> Unit) {
        connect()
        scope.launch { block(controller.await()) }
    }

    private fun connect() {
        if (connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val connected = future.get()
            connected.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = publish(player)
            })
            controller.complete(connected)
            publish(connected)
        }, ContextCompat.getMainExecutor(context))
    }

    override fun play(queue: List<MusicTrack>, startIndex: Int) {
        if (queue.isEmpty()) return
        queue.forEach { tracks[it.id] = it }
        errors.report(null)
        withController { player ->
            player.setMediaItems(queue.map(MediaItems::toMediaItem), startIndex.coerceIn(0, queue.lastIndex), 0L)
            player.prepare()
            player.play()
        }
    }

    override fun togglePlayPause() = withController { player ->
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    override fun next() = withController { it.seekToNextMediaItem() }

    override fun previous() = withController { it.seekToPrevious() }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0)) }

    override fun skipTo(queueIndex: Int) = withController { player ->
        if (queueIndex in 0 until player.mediaItemCount) player.seekTo(queueIndex, 0L)
    }

    override fun setShuffle(enabled: Boolean) = withController { it.shuffleModeEnabled = enabled }

    override fun setRepeatMode(mode: RepeatMode) = withController { it.repeatMode = mode.toPlayer() }

    override fun retry() {
        errors.report(null)
        withController { player ->
            player.prepare()
            player.play()
        }
    }

    private fun publish(player: Player) {
        val queue = (0 until player.mediaItemCount).map { index ->
            val item = player.getMediaItemAt(index)
            tracks[item.mediaId] ?: MediaItems.toTrack(item)
        }
        val index = if (queue.isEmpty()) -1 else player.currentMediaItemIndex
        _state.value = PlayerState(
            currentTrack = queue.getOrNull(index),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: queue.getOrNull(index)?.durationMs ?: 0L,
            queue = queue,
            currentIndex = index,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode.toRepeatMode(),
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            error = errors.current.value,
        )
        updateTicker(player)
    }

    private fun updateTicker(player: Player) {
        if (player.isPlaying && ticker?.isActive != true) {
            ticker = scope.launch {
                while (isActive) {
                    delay(POSITION_INTERVAL_MS)
                    _state.value = _state.value.copy(positionMs = player.currentPosition.coerceAtLeast(0))
                }
            }
        } else if (!player.isPlaying) {
            ticker?.cancel()
            ticker = null
        }
    }

    private companion object {
        const val POSITION_INTERVAL_MS = 500L
    }
}
