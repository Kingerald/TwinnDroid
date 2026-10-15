package com.twinndroid.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import com.twinndroid.data.CloneConfig
import com.twinndroid.data.IconMode
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.min

/** Renders the clone's launcher icon as a PNG that gets injected into the APK. */
object IconFactory {
    const val SIZE = 192

    /** Returns null for [IconMode.ORIGINAL] (the APK's own icon is kept untouched). */
    fun render(config: CloneConfig, original: Drawable?): Bitmap? = when (config.iconMode) {
        IconMode.ORIGINAL -> null
        IconMode.BADGE -> badged(original, config.badgeColor.toInt(), config.badgeText)
        IconMode.CUSTOM -> config.customIconPath?.let { custom(File(it)) } ?: badged(original, config.badgeColor.toInt(), config.badgeText)
    }

    fun toPng(bitmap: Bitmap): ByteArray =
        ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }

    private fun badged(original: Drawable?, color: Int, text: String): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (original != null) {
            c.drawBitmap(original.toBitmap(SIZE, SIZE), 0f, 0f, null)
        } else {
            c.drawCircle(SIZE / 2f, SIZE / 2f, SIZE / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0xFF444444.toInt() })
        }
        val r = SIZE * 0.2f
        val cx = SIZE - r - SIZE * 0.04f
        val cy = SIZE - r - SIZE * 0.04f
        c.drawCircle(cx, cy, r + SIZE * 0.025f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0xFFFFFFFF.toInt() })
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        val label = text.take(2)
        if (label.isNotEmpty()) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = 0xFFFFFFFF.toInt()
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                textSize = if (label.length == 1) r * 1.25f else r * 0.95f
            }
            val bounds = Rect().also { p.getTextBounds(label, 0, label.length, it) }
            c.drawText(label, cx, cy + bounds.height() / 2f, p)
        }
        return bmp
    }

    private fun custom(file: File): Bitmap? {
        val src = BitmapFactory.decodeFile(file.path) ?: return null
        val side = min(src.width, src.height)
        val crop = Rect((src.width - side) / 2, (src.height - side) / 2, (src.width + side) / 2, (src.height + side) / 2)
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()), SIZE * 0.22f, SIZE * 0.22f, Path.Direction.CW) }
        c.clipPath(clip)
        c.drawBitmap(src, crop, Rect(0, 0, SIZE, SIZE), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return bmp
    }
}
