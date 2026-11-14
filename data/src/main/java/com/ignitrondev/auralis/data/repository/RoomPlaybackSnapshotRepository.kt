/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.data.repository

import com.ignitrondev.auralis.core.database.dao.PlaybackQueueDao
import com.ignitrondev.auralis.core.database.entity.QueueItemEntity
import com.ignitrondev.auralis.core.database.entity.QueueItemEntity.Companion.LIST_SEPARATOR
import com.ignitrondev.auralis.core.database.entity.QueueStateEntity
import com.ignitrondev.auralis.domain.model.AlbumRef
import com.ignitrondev.auralis.domain.model.ArtistRef
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.domain.player.PlaybackSnapshot
import com.ignitrondev.auralis.domain.player.RepeatMode
import com.ignitrondev.auralis.domain.repository.PlaybackSnapshotRepository
import javax.inject.Inject

class RoomPlaybackSnapshotRepository @Inject constructor(
    private val dao: PlaybackQueueDao,
) : PlaybackSnapshotRepository {

    override suspend fun load(): PlaybackSnapshot? {
        val queue = dao.items().map { it.toTrack() }
        if (queue.isEmpty()) return null
        val state = dao.state()
        return PlaybackSnapshot(
            queue = queue,
            currentIndex = (state?.currentIndex ?: 0).coerceIn(0, queue.lastIndex),
            positionMs = (state?.positionMs ?: 0L).coerceAtLeast(0L),
            shuffleEnabled = state?.shuffleEnabled ?: false,
            repeatMode = state?.repeatMode?.let { saved -> RepeatMode.entries.find { it.name == saved } } ?: RepeatMode.OFF,
        )
    }

    override suspend fun saveQueue(queue: List<MusicTrack>) =
        dao.replaceItems(queue.mapIndexed { position, track -> track.toEntity(position) })

    override suspend fun saveProgress(currentIndex: Int, positionMs: Long, shuffleEnabled: Boolean, repeatMode: RepeatMode) =
        dao.upsertState(
            QueueStateEntity(
                currentIndex = currentIndex,
                positionMs = positionMs,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode.name,
            ),
        )
}

internal fun MusicTrack.toEntity(position: Int) = QueueItemEntity(
    position = position,
    trackId = id,
    sourceId = sourceId,
    title = title,
    artistNames = artists.joinToString(LIST_SEPARATOR.toString()) { it.name },
    artistIds = artists.joinToString(LIST_SEPARATOR.toString()) { it.id.orEmpty() },
    albumId = album?.id,
    albumTitle = album?.title,
    durationMs = durationMs,
    artworkUri = artworkUri,
    isVideo = isVideo,
    isExplicit = isExplicit,
)

internal fun QueueItemEntity.toTrack(): MusicTrack {
    val names = if (artistNames.isEmpty()) emptyList() else artistNames.split(LIST_SEPARATOR)
    val ids = artistIds.split(LIST_SEPARATOR)
    return MusicTrack(
        id = trackId,
        sourceId = sourceId,
        title = title,
        artists = names.mapIndexed { index, name -> ArtistRef(ids.getOrNull(index)?.ifEmpty { null }, name) },
        album = albumTitle?.let { AlbumRef(albumId, it) },
        durationMs = durationMs,
        artworkUri = artworkUri,
        isVideo = isVideo,
        isExplicit = isExplicit,
    )
}
