/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Loading artwork for a card, and handing the finished card to other apps. */
internal object ShareActions {

    /**
     * The artwork as a software bitmap (a card is drawn on a plain canvas, which cannot read
     * hardware bitmaps), or null when there is none or it cannot load. Uses the app's image cache.
     */
    suspend fun loadArtwork(context: Context, uri: String?): Bitmap? {
        if (uri == null) return null
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(ShareCardRenderer.WIDTH)
            .allowHardware(false)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        return (result as? SuccessResult)?.image?.toBitmap()
    }

    /**
     * Opens the system share sheet with the card as a PNG and the link as text, so it reaches
     * story apps (which take the image) and chats (which keep the link).
     */
    suspend fun share(context: Context, card: ShareCard, image: Bitmap, chooserTitle: String) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
            // One file, replaced each time: old cards do not pile up in the cache.
            File(dir, FILE_NAME).also { file -> file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
        val text = listOfNotNull(card.title.takeIf { !card.isLyrics }, card.link).joinToString("\n").ifBlank { null }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            text?.let { putExtra(Intent.EXTRA_TEXT, it) }
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun copyLink(context: Context, link: String) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Auralis", link))
    }

    private const val SHARE_DIR = "share"
    private const val FILE_NAME = "auralis-share.png"
}
