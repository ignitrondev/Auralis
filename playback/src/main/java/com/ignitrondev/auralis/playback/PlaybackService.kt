/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import android.content.Intent
import androidx.core.net.toUri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ignitrondev.auralis.domain.player.PlaybackError
import com.ignitrondev.auralis.provider.api.StreamException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Owns the one ExoPlayer and its MediaSession (spec section 7.2). Runs independently of the UI;
 * Media3 provides the notification, lock-screen controls and foreground handling.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var resolver: StreamResolver
    @Inject lateinit var errors: PlaybackErrors
    @Inject lateinit var ui: PlaybackUi

    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var retriedTrackId: String? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(streamDataSourceFactory()))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true, // handle audio focus
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.addListener(PlayerEvents(player))
        session = MediaSession.Builder(this, player)
            .setSessionActivity(ui.sessionActivity())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    /** auralis://<source>/<trackId> becomes the signed stream URL, requested in chunks when needed. */
    private fun streamDataSourceFactory(): DataSource.Factory {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        val chunked = DataSource.Factory { ChunkedDataSource(http.createDataSource()) }
        return ResolvingDataSource.Factory(chunked) { dataSpec -> resolve(dataSpec) }
    }

    private fun resolve(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != MediaItems.SCHEME) return dataSpec
        val trackId = dataSpec.uri.lastPathSegment ?: throw IOException("no track id")
        val stream = try {
            resolver.resolveBlocking(trackId)
        } catch (error: StreamException) {
            throw IOException("stream unavailable", error)
        }
        return dataSpec.buildUpon()
            .setUri(stream.url.toUri())
            .setHttpRequestHeaders(dataSpec.httpRequestHeaders + stream.headers)
            .setCustomData(stream.chunkSizeBytes)
            .build()
    }

    private inner class PlayerEvents(private val player: ExoPlayer) : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            retriedTrackId = null
            errors.report(null)
            // Resolve the next track ahead of time so it starts without waiting.
            val next = player.nextMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: return
            val nextId = player.getMediaItemAt(next).mediaId
            scope.launch { resolver.prefetch(nextId) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) errors.report(null)
        }

        override fun onPlayerError(error: PlaybackException) {
            val trackId = player.currentMediaItem?.mediaId
            val expired = (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 403
            if (expired && trackId != null && retriedTrackId != trackId) {
                // The signed URL expired or was rejected: resolve a fresh one once and continue.
                retriedTrackId = trackId
                resolver.invalidate(trackId)
                player.prepare()
                player.play()
                return
            }
            errors.report(PlaybackErrors.classify(error))
        }
    }
}
