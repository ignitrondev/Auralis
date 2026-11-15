/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withTranslation
import com.ignitrondev.auralis.core.designsystem.R as DesignR

/**
 * Draws share cards as 1080 × 1920 images (the 9:16 story size). Plain Android drawing, not a
 * screenshot, so the card is the same on every phone and the preview is exactly what is sent.
 *
 * Monochrome like the app (spec 8.2.1): the artwork is the only colour, as a softened wash behind
 * the card; text and the Auralis mark are white.
 */
object ShareCardRenderer {
    const val WIDTH = 1080
    const val HEIGHT = 1920

    fun render(context: Context, card: ShareCard, artwork: Bitmap?): Bitmap {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        val fonts = Fonts.load(context)
        drawBackground(canvas, artwork)
        if (card.isLyrics) drawLyricsCard(canvas, card, artwork, fonts) else drawItemCard(canvas, card, artwork, fonts)
        drawMark(canvas, fonts)
        return bitmap
    }

    private fun drawBackground(canvas: Canvas, artwork: Bitmap?) {
        if (artwork == null) {
            val paint = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), Color.rgb(0x2A, 0x2A, 0x2A), Color.rgb(0x0B, 0x0B, 0x0B), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)
            return
        }
        // A tiny copy of the artwork, scaled back up with filtering, is a wash of its colours: the
        // look of a heavy blur without a blur pass. Scaling up in 4x steps keeps the wash smooth;
        // one big step would show the tiny copy's pixels as soft blocks.
        var wash = artwork.scale(WASH_WIDTH, WASH_HEIGHT, filter = true)
        while (wash.width * WASH_STEP < WIDTH) wash = wash.scale(wash.width * WASH_STEP, wash.height * WASH_STEP, filter = true)
        canvas.drawBitmap(wash, null, RectF(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        val scrim = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                intArrayOf(Color.argb(0x66, 0, 0, 0), Color.argb(0x8C, 0, 0, 0), Color.argb(0xE6, 0, 0, 0)),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), scrim)
    }

    private fun drawItemCard(canvas: Canvas, card: ShareCard, artwork: Bitmap?, fonts: Fonts) {
        val size = 760f
        val left = (WIDTH - size) / 2
        val top = 330f
        drawArtwork(canvas, artwork, RectF(left, top, left + size, top + size), radius = 56f, shadow = true)
        var y = top + size + 96f
        y += drawText(canvas, card.title, fonts.display, 68f, Color.WHITE, y, maxLines = 2, align = Layout.Alignment.ALIGN_CENTER)
        card.subtitle?.let {
            y += 18f
            drawText(canvas, it, fonts.text, 40f, Color.argb(0xB8, 0xFF, 0xFF, 0xFF), y, maxLines = 1, align = Layout.Alignment.ALIGN_CENTER)
        }
    }

    private fun drawLyricsCard(canvas: Canvas, card: ShareCard, artwork: Bitmap?, fonts: Fonts) {
        val margin = 96f
        val inner = 64f
        val width = WIDTH - 2 * margin
        // Measure the lines first: the glass panel fits around them.
        val lyricPaint = textPaint(fonts.display, 64f, Color.WHITE)
        val lyricLayouts = card.lyrics.take(MAX_LYRIC_LINES).map { layout(it, lyricPaint, (width - 2 * inner).toInt(), maxLines = 3, Layout.Alignment.ALIGN_NORMAL) }
        val headerHeight = 132f
        val lyricsHeight = lyricLayouts.sumOf { it.height } + LINE_GAP * (lyricLayouts.size - 1).coerceAtLeast(0)
        val panelHeight = inner + headerHeight + 72f + lyricsHeight + inner
        val top = ((HEIGHT - panelHeight) / 2).coerceAtLeast(240f) - 60f
        val panel = RectF(margin, top, WIDTH - margin, top + panelHeight)

        // Glass panel: a light fill and a thin rim, as glass surfaces look in the app.
        canvas.drawRoundRect(panel, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(0x24, 0xFF, 0xFF, 0xFF) })
        canvas.drawRoundRect(
            panel, 56f, 56f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = Color.argb(0x40, 0xFF, 0xFF, 0xFF)
            },
        )

        val artLeft = panel.left + inner
        val artTop = panel.top + inner
        drawArtwork(canvas, artwork, RectF(artLeft, artTop, artLeft + headerHeight, artTop + headerHeight), radius = 24f, shadow = false)
        val textLeft = artLeft + headerHeight + 36f
        val textWidth = (panel.right - inner - textLeft).toInt()
        val titleLayout = layout(card.title, textPaint(fonts.display, 44f, Color.WHITE), textWidth, maxLines = 1, Layout.Alignment.ALIGN_NORMAL)
        val subtitleLayout = card.subtitle?.let { layout(it, textPaint(fonts.text, 34f, Color.argb(0xB8, 0xFF, 0xFF, 0xFF)), textWidth, maxLines = 1, Layout.Alignment.ALIGN_NORMAL) }
        val headerTextHeight = titleLayout.height + (subtitleLayout?.height?.plus(8) ?: 0)
        canvas.withTranslation(textLeft, artTop + (headerHeight - headerTextHeight) / 2) {
            titleLayout.draw(this)
            subtitleLayout?.let {
                translate(0f, titleLayout.height + 8f)
                it.draw(this)
            }
        }

        var y = artTop + headerHeight + 72f
        lyricLayouts.forEach { lyric ->
            canvas.withTranslation(panel.left + inner, y) { lyric.draw(this) }
            y += lyric.height + LINE_GAP
        }
    }

    private fun drawArtwork(canvas: Canvas, artwork: Bitmap?, bounds: RectF, radius: Float, shadow: Boolean) {
        if (shadow) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                setShadowLayer(64f, 0f, 28f, Color.argb(0x99, 0, 0, 0))
            }
            canvas.drawRoundRect(bounds, radius, radius, shadowPaint)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (artwork == null) {
            paint.shader = LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom, Color.rgb(0x3A, 0x3A, 0x3A), Color.rgb(0x1C, 0x1C, 0x1C), Shader.TileMode.CLAMP)
        } else {
            // Centre-crop the artwork into the rounded square.
            val scale = maxOf(bounds.width() / artwork.width, bounds.height() / artwork.height)
            val dx = bounds.left + (bounds.width() - artwork.width * scale) / 2
            val dy = bounds.top + (bounds.height() - artwork.height * scale) / 2
            paint.shader = BitmapShader(artwork, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply { setScale(scale, scale); postTranslate(dx, dy) })
            }
        }
        canvas.drawRoundRect(bounds, radius, radius, paint)
    }

    /** The Arch mark (spec 8.5, 120-unit grid) and the wordmark, centred at the bottom. */
    private fun drawMark(canvas: Canvas, fonts: Fonts) {
        val markSize = 64f
        val wordPaint = textPaint(fonts.display, 44f, Color.argb(0xE6, 0xFF, 0xFF, 0xFF))
        val word = "Auralis"
        val wordWidth = wordPaint.measureText(word)
        val gap = 20f
        val total = markSize + gap + wordWidth
        val left = (WIDTH - total) / 2
        val top = HEIGHT - 150f - markSize
        val unit = markSize / 120f
        val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
            style = Paint.Style.STROKE
            strokeWidth = 13f * unit
            strokeCap = Paint.Cap.ROUND
        }
        val arch = Path().apply {
            moveTo(left + 32 * unit, top + 92 * unit)
            lineTo(left + 32 * unit, top + 60 * unit)
            arcTo(RectF(left + 32 * unit, top + 32 * unit, left + 88 * unit, top + 88 * unit), 180f, 180f)
            lineTo(left + 88 * unit, top + 92 * unit)
        }
        canvas.drawPath(arch, markPaint)
        canvas.drawCircle(left + 60 * unit, top + 66 * unit, 8 * unit, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = markPaint.color })
        val baseline = top + markSize / 2 - (wordPaint.descent() + wordPaint.ascent()) / 2
        canvas.drawText(word, left + markSize + gap, baseline, wordPaint)
    }

    /** Draws wrapped text centred across the card's width with side margins; returns its height. */
    private fun drawText(canvas: Canvas, text: String, typeface: Typeface, size: Float, color: Int, top: Float, maxLines: Int, align: Layout.Alignment): Float {
        val margin = 96f
        val layout = layout(text, textPaint(typeface, size, color), (WIDTH - 2 * margin).toInt(), maxLines, align)
        canvas.withTranslation(margin, top) { layout.draw(this) }
        return layout.height.toFloat()
    }

    private fun textPaint(typeface: Typeface, size: Float, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        this.color = color
    }

    private fun layout(text: String, paint: TextPaint, width: Int, maxLines: Int, align: Layout.Alignment): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(align)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, 1.08f)
            .build()

    private class Fonts(val display: Typeface, val text: Typeface) {
        companion object {
            fun load(context: Context): Fonts {
                val display = ResourcesCompat.getFont(context, DesignR.font.space_grotesk) ?: Typeface.DEFAULT_BOLD
                val text = ResourcesCompat.getFont(context, DesignR.font.inter) ?: Typeface.DEFAULT
                return Fonts(display.weight(600), text.weight(500))
            }

            private fun Typeface.weight(weight: Int): Typeface =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Typeface.create(this, weight, false) else this
        }
    }

    const val MAX_LYRIC_LINES = 5
    private const val LINE_GAP = 22f
    private const val WASH_WIDTH = 6
    private const val WASH_HEIGHT = 11
    private const val WASH_STEP = 4
}
