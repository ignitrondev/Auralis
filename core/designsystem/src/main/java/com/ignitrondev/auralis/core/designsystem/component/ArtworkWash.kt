/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A soft wash of an artwork's colours: the artwork loaded tiny and scaled back up in steps. It
 * looks like a heavy blur, but it is a plain bitmap made once off the main thread, so drawing it
 * costs nothing per frame. A live blur of a full-screen image re-renders on every frame of an
 * animation, which made opening Now Playing stutter.
 *
 * Null until it is ready (the caller shows its fallback meanwhile) and when there is no artwork.
 * Washes of recent artworks are kept, so reopening Now Playing shows it at once.
 */
@Composable
fun rememberArtworkWash(uri: String?): State<ImageBitmap?> {
    val context = LocalContext.current.applicationContext
    return produceState(initialValue = uri?.let(ArtworkWash::cached), uri) {
        // The state outlives a change of artwork, so it still holds the last one's wash here. That
        // one stays up while the new one loads (no flash of the fallback), then gives way to it.
        value = when (uri) {
            null -> null
            else -> ArtworkWash.cached(uri) ?: ArtworkWash.load(context, uri)
        }
    }
}

internal object ArtworkWash {
    private const val SOURCE_WIDTH = 6
    private const val SOURCE_HEIGHT = 11
    private const val STEP = 4
    private const val TARGET_WIDTH = 384

    private val cache = LruCache<String, ImageBitmap>(6)

    fun cached(uri: String): ImageBitmap? = cache.get(uri)

    suspend fun load(context: Context, uri: String): ImageBitmap? = try {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(SOURCE_WIDTH * 16)
            .allowHardware(false)
            .build()
        val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image
        image?.let {
            withContext(Dispatchers.Default) {
                var wash = Bitmap.createScaledBitmap(it.toBitmap(), SOURCE_WIDTH, SOURCE_HEIGHT, true)
                // Scaling up in 4x steps keeps it smooth; one big step shows soft blocks.
                while (wash.width < TARGET_WIDTH) wash = Bitmap.createScaledBitmap(wash, wash.width * STEP, wash.height * STEP, true)
                wash.asImageBitmap().also { cache.put(uri, it) }
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        null
    }
}
