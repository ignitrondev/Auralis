/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import androidx.media3.common.Player
import com.ignitrondev.auralis.domain.player.PlaybackSnapshot
import com.ignitrondev.auralis.domain.player.RepeatMode
import com.ignitrondev.auralis.domain.repository.PlaybackSnapshotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps the player's queue and progress on the device so playback can continue after the app
 * process ends (spec 7.1, "playback state restoration").
 *
 * - [restore] fills an empty player with the saved queue, paused at the saved position. Nothing is
 *   saved until it finishes, so a fresh empty player never overwrites the saved queue.
 * - The queue is written when it changes; the progress when the track, position, play state,
 *   shuffle or repeat changes, and every [SAVE_INTERVAL_MS] while playing.
 * - Writes run one at a time off the main thread, and finish even after the service is destroyed.
 */
internal class QueuePersistence(
    private val player: Player,
    private val snapshots: PlaybackSnapshotRepository,
) : Player.Listener {

    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val writes = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var restored = false
    private var savedQueueIds: List<String>? = null
    private var ticker: Job? = null

    fun restore() {
        main.launch {
            val snapshot = load()
            if (snapshot != null && player.mediaItemCount == 0) {
                player.setMediaItems(snapshot.queue.map(MediaItems::toMediaItem), snapshot.currentIndex, snapshot.positionMs)
                player.shuffleModeEnabled = snapshot.shuffleEnabled
                player.repeatMode = snapshot.repeatMode.toPlayer()
                savedQueueIds = snapshot.queue.map { it.id }
            }
            restored = true
            // Something may have started playing while the snapshot was loading: save it now.
            saveQueue()
            saveProgress()
        }
    }

    /** The saved snapshot, or null when there is none or it cannot be read. */
    suspend fun load(): PlaybackSnapshot? =
        withContext(writes.coroutineContext) { runCatching { snapshots.load() }.getOrNull() }

    override fun onEvents(player: Player, events: Player.Events) {
        if (!restored) return
        if (events.contains(Player.EVENT_TIMELINE_CHANGED)) saveQueue()
        if (events.containsAny(*PROGRESS_EVENTS)) saveProgress()
        if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) updateTicker()
    }

    /** Saves the current progress now, for example when the app is swiped away. */
    fun saveProgress() {
        if (!restored || player.mediaItemCount == 0) return
        val index = player.currentMediaItemIndex
        val position = player.currentPosition.coerceAtLeast(0)
        val shuffle = player.shuffleModeEnabled
        val repeat = player.repeatMode.toRepeatMode()
        writes.launch { runCatching { snapshots.saveProgress(index, position, shuffle, repeat) } }
    }

    /** Stops the periodic save after a final one. Pending writes still complete. */
    fun release() {
        saveProgress()
        main.cancel()
    }

    private fun saveQueue() {
        val queue = (0 until player.mediaItemCount).map { MediaItems.toTrack(player.getMediaItemAt(it)) }
        val ids = queue.map { it.id }
        if (ids == savedQueueIds) return
        savedQueueIds = ids
        writes.launch { runCatching { snapshots.saveQueue(queue) } }
    }

    private fun updateTicker() {
        ticker?.cancel()
        ticker = if (!player.isPlaying) {
            null
        } else {
            main.launch {
                while (isActive) {
                    delay(SAVE_INTERVAL_MS)
                    saveProgress()
                }
            }
        }
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 10_000L

        val PROGRESS_EVENTS = intArrayOf(
            Player.EVENT_TIMELINE_CHANGED,
            Player.EVENT_MEDIA_ITEM_TRANSITION,
            Player.EVENT_POSITION_DISCONTINUITY,
            Player.EVENT_IS_PLAYING_CHANGED,
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            Player.EVENT_REPEAT_MODE_CHANGED,
        )
    }
}

internal fun RepeatMode.toPlayer(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
}

internal fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}
