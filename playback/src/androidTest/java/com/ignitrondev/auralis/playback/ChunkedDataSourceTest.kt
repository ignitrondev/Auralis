/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ChunkedDataSourceTest {
    /** A server that answers at most [maxRange] bytes per request, like a range-limited stream. */
    private class RangeLimitedServer(private val content: ByteArray, private val maxRange: Long) : DataSource {
        val requests = mutableListOf<Pair<Long, Long>>()
        private var position = 0
        private var end = 0

        override fun addTransferListener(transferListener: TransferListener) = Unit
        override fun open(dataSpec: DataSpec): Long {
            val requested = if (dataSpec.length == C.LENGTH_UNSET.toLong()) content.size - dataSpec.position else dataSpec.length
            val served = minOf(requested, maxRange, content.size - dataSpec.position)
            requests += dataSpec.position to served
            position = dataSpec.position.toInt()
            end = (dataSpec.position + served).toInt()
            return served
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= end) return C.RESULT_END_OF_INPUT
            val count = minOf(length, end - position)
            System.arraycopy(content, position, buffer, offset, count)
            position += count
            return count
        }
        override fun getUri(): Uri? = null
        override fun getResponseHeaders(): Map<String, List<String>> =
            mapOf("Content-Range" to listOf("bytes 0-0/${content.size}"))
        override fun close() = Unit
    }

    private fun readAll(source: DataSource, spec: DataSpec): ByteArray {
        source.open(spec)
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(700)
        while (true) {
            val read = source.read(buffer, 0, buffer.size)
            if (read == C.RESULT_END_OF_INPUT) break
            out.write(buffer, 0, read)
        }
        source.close()
        return out.toByteArray()
    }

    private val content = ByteArray(10_000) { (it % 251).toByte() }

    @Test
    fun joinsBoundedRangesIntoTheWholeStream() {
        val server = RangeLimitedServer(content, maxRange = 3_000)
        val spec = DataSpec.Builder().setUri("https://example/audio").setCustomData(3_000L).build()
        assertArrayEquals(content, readAll(ChunkedDataSource(server), spec))
        assertEquals(listOf(0L to 3_000L, 3_000L to 3_000L, 6_000L to 3_000L, 9_000L to 1_000L), server.requests)
    }

    @Test
    fun seeksStartFromTheRequestedPosition() {
        val server = RangeLimitedServer(content, maxRange = 4_000)
        val spec = DataSpec.Builder().setUri("https://example/audio").setPosition(2_500).setCustomData(4_000L).build()
        assertArrayEquals(content.copyOfRange(2_500, content.size), readAll(ChunkedDataSource(server), spec))
        assertTrue(server.requests.all { it.second <= 4_000 })
    }

    @Test
    fun specsWithoutChunkSizePassThrough() {
        val server = RangeLimitedServer(content, maxRange = Long.MAX_VALUE)
        val spec = DataSpec.Builder().setUri("https://example/audio").build()
        assertArrayEquals(content, readAll(ChunkedDataSource(server), spec))
        assertEquals(1, server.requests.size)
    }
}
