/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.botguard

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ignitrondev.auralis.provider.youtube.YouTubeMusicProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * End-to-end playback spike on a device: BotGuard in a WebView, stream resolution, and the first
 * bytes of audio. Talks to YouTube, so it only runs when asked:
 * ./gradlew :provider:botguard:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.live=true
 * Never logs tokens or stream URLs.
 */
@RunWith(AndroidJUnit4::class)
class LivePlaybackSpikeTest {
    @Test
    fun mintsTokensAndResolvesAPlayableStream() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("live playback spike disabled", args.getString("live") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val attestation = WebViewAttestationProvider(context)

        val started = System.currentTimeMillis()
        val token = attestation.mint("spike-identifier")
        Log.i(TAG, "SPIKE mint: ${if (token != null) "ok, ${token.length} chars" else "null"} in ${System.currentTimeMillis() - started} ms")
        checkNotNull(token) { "BotGuard did not mint a token" }

        YouTubeMusicProvider(attestation) { warning, tag, message -> Log.i(TAG, "ITX ${if (warning) "W" else "I"} $tag: $message") }.use { provider ->
            val resolveStart = System.currentTimeMillis()
            val stream = provider.resolveStream("dX3k_QDnzHE")
            Log.i(TAG, "SPIKE stream: $stream in ${System.currentTimeMillis() - resolveStart} ms")
            val (status, bytes) = withContext(Dispatchers.IO) {
                val connection = URL(stream.url).openConnection() as HttpURLConnection
                stream.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
                connection.setRequestProperty("Range", "bytes=0-65535")
                val code = connection.responseCode
                val read = if (code in 200..299) connection.inputStream.use { it.readBytes().size } else 0
                connection.disconnect()
                code to read
            }
            Log.i(TAG, "SPIKE media: status=$status bytes=$bytes")
            check(status in 200..299 && bytes > 0) { "media request failed: $status" }
        }
    }

    private companion object {
        const val TAG = "AuralisSpike"
    }
}
