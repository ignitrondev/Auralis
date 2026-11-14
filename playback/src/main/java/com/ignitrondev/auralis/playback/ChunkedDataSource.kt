/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * Reads a stream that the server only serves in bounded byte ranges. It requests consecutive
 * ranges of [DataSpec.customData] bytes and joins them into one continuous stream, so the player
 * never sees a range end as the end of the track. Specs without a chunk size pass straight
 * through to [upstream].
 */
@UnstableApi
class ChunkedDataSource(private val upstream: DataSource) : DataSource {
    private var spec: DataSpec? = null
    private var chunkSize = 0L
    private var position = 0L
    private var bytesRemaining = C.LENGTH_UNSET.toLong()
    private var chunkRemaining = 0L
    private var totalLength = C.LENGTH_UNSET.toLong()
    private var upstreamOpen = false

    override fun addTransferListener(transferListener: TransferListener) = upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        val size = (dataSpec.customData as? Long)?.takeIf { it > 0 }
        if (size == null) {
            spec = null
            upstreamOpen = true
            return upstream.open(dataSpec)
        }
        spec = dataSpec
        chunkSize = size
        position = dataSpec.position
        bytesRemaining = dataSpec.length
        totalLength = C.LENGTH_UNSET.toLong()
        openChunk()
        return when {
            bytesRemaining != C.LENGTH_UNSET.toLong() -> bytesRemaining
            totalLength != C.LENGTH_UNSET.toLong() -> (totalLength - position).also { bytesRemaining = it }
            else -> C.LENGTH_UNSET.toLong()
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (spec == null) return upstream.read(buffer, offset, length)
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        if (chunkRemaining == 0L) {
            if (!hasMore()) return C.RESULT_END_OF_INPUT
            openChunk()
        }
        val toRead = minOf(length.toLong(), chunkRemaining).toInt()
        val read = upstream.read(buffer, offset, toRead)
        if (read == C.RESULT_END_OF_INPUT) {
            chunkRemaining = 0
            return if (hasMore()) read(buffer, offset, length) else C.RESULT_END_OF_INPUT
        }
        position += read
        chunkRemaining -= read
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        return read
    }

    private fun hasMore(): Boolean = when {
        bytesRemaining != C.LENGTH_UNSET.toLong() -> bytesRemaining > 0
        totalLength != C.LENGTH_UNSET.toLong() -> position < totalLength
        else -> false
    }

    private fun openChunk() {
        val base = requireNotNull(spec)
        if (upstreamOpen) upstream.close()
        val length = if (bytesRemaining != C.LENGTH_UNSET.toLong()) minOf(chunkSize, bytesRemaining) else chunkSize
        val chunkSpec = base.buildUpon().setPosition(position).setLength(length).setCustomData(null).build()
        val opened = upstream.open(chunkSpec)
        upstreamOpen = true
        chunkRemaining = if (opened != C.LENGTH_UNSET.toLong()) opened else length
        if (totalLength == C.LENGTH_UNSET.toLong()) totalLength = totalFromContentRange()
    }

    /** "Content-Range: bytes 0-1048575/4567890" gives the full length. */
    private fun totalFromContentRange(): Long =
        upstream.responseHeaders.entries.firstOrNull { it.key.equals("Content-Range", ignoreCase = true) }
            ?.value?.firstOrNull()?.substringAfterLast('/')?.trim()?.toLongOrNull()
            ?: C.LENGTH_UNSET.toLong()

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        spec = null
        if (upstreamOpen) {
            upstreamOpen = false
            upstream.close()
        }
    }
}
