/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.share

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareCardRendererTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** A flat red square stands in for album artwork. */
    private fun artwork() = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

    private val song = ShareCard("Midnight City", "Song • M83", artworkUri = null, link = "https://music.youtube.com/watch?v=x")

    @Test
    fun cardsAreStorySizedAndFullyPainted() {
        for (card in listOf(song, song.copy(lyrics = listOf("First line", "Second line")))) {
            for (art in listOf(null, artwork())) {
                val image = ShareCardRenderer.render(context, card, art)
                assertEquals(ShareCardRenderer.WIDTH, image.width)
                assertEquals(ShareCardRenderer.HEIGHT, image.height)
                // Every corner is painted: the background covers the whole card.
                listOf(0 to 0, image.width - 1 to 0, 0 to image.height - 1, image.width - 1 to image.height - 1).forEach { (x, y) ->
                    assertEquals("corner $x,$y is opaque", 255, Color.alpha(image.getPixel(x, y)))
                }
            }
        }
    }

    @Test
    fun theArtworkColoursTheItemCard() {
        val image = ShareCardRenderer.render(context, song, artwork())
        // The centre of the artwork square is the artwork itself.
        val centre = image.getPixel(ShareCardRenderer.WIDTH / 2, 330 + 380)
        assertTrue("artwork is drawn", Color.red(centre) > 200 && Color.green(centre) < 60)
        // The background is a darkened wash of the artwork, not the neutral grey.
        val corner = image.getPixel(10, 10)
        assertTrue("background takes the artwork colour", Color.red(corner) > Color.green(corner))
        assertNotEquals(corner, ShareCardRenderer.render(context, song, null).getPixel(10, 10))
    }
}
