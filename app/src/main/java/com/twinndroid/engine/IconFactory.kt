package com.twinndroid.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import com.twinndroid.data.CloneConfig
import com.twinndroid.data.IconAdjust
import com.twinndroid.data.IconMode
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Renders the clone's launcher icon as a PNG that gets injected into the APK. */
object IconFactory {
    const val SIZE = 192

    /**
     * Returns null when the APK's own icon can be kept untouched (original icon, no adjustments),
     * which preserves adaptive-icon shapes and animations.
     */
    fun render(config: CloneConfig, original: Drawable?): Bitmap? {
        val adjust = config.iconAdjust
        val base: Bitmap = when (config.iconMode) {
            IconMode.ORIGINAL -> if (adjust.isIdentity) return null else originalBitmap(original)
            IconMode.BADGE -> originalBitmap(original)
            IconMode.CUSTOM -> config.customIconPath?.let { custom(File(it)) } ?: originalBitmap(original)
        }
        val adjusted = if (adjust.isIdentity) base else adjust(base, adjust)
        return if (config.iconMode == IconMode.BADGE) badge(adjusted, config.badgeColor.toInt(), config.badgeText) else adjusted
    }

    fun toPng(bitmap: Bitmap): ByteArray =
        ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }

    private fun originalBitmap(original: Drawable?): Bitmap {
        if (original != null) return original.toBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true)
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawCircle(SIZE / 2f, SIZE / 2f, SIZE / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF444444.toInt() })
        return bmp
    }

    fun colorMatrix(a: IconAdjust): ColorMatrix {
        val m = ColorMatrix()
        m.setSaturation(a.saturation)
        if (a.hue != 0f) m.postConcat(hueMatrix(a.hue))
        if (a.contrast != 0f) {
            val scale = 1f + a.contrast * 2f
            val t = 128f * (1f - scale)
            m.postConcat(ColorMatrix(floatArrayOf(scale, 0f, 0f, 0f, t, 0f, scale, 0f, 0f, t, 0f, 0f, scale, 0f, t, 0f, 0f, 0f, 1f, 0f)))
        }
        if (a.brightness != 0f) {
            val b = a.brightness * 255f
            m.postConcat(ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, b, 0f, 1f, 0f, 0f, b, 0f, 0f, 1f, 0f, b, 0f, 0f, 0f, 1f, 0f)))
        }
        if (a.invert) {
            m.postConcat(ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f)))
        }
        return m
    }

    /** Luminance-preserving hue rotation. */
    private fun hueMatrix(degrees: Float): ColorMatrix {
        val r = Math.toRadians(degrees.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        val lr = 0.213f
        val lg = 0.715f
        val lb = 0.072f
        return ColorMatrix(
            floatArrayOf(
                lr + c * (1 - lr) + s * (-lr), lg + c * (-lg) + s * (-lg), lb + c * (-lb) + s * (1 - lb), 0f, 0f,
                lr + c * (-lr) + s * 0.143f, lg + c * (1 - lg) + s * 0.140f, lb + c * (-lb) + s * (-0.283f), 0f, 0f,
                lr + c * (-lr) + s * (-(1 - lr)), lg + c * (-lg) + s * lg, lb + c * (1 - lb) + s * lb, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    private fun adjust(src: Bitmap, a: IconAdjust): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.save()
        c.rotate(a.rotation.toFloat(), SIZE / 2f, SIZE / 2f)
        if (a.flip) c.scale(-1f, 1f, SIZE / 2f, SIZE / 2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(colorMatrix(a)) }
        c.drawBitmap(src, null, Rect(0, 0, SIZE, SIZE), paint)
        c.restore()
        a.tint?.let { tint ->
            // SRC_ATOP keeps the icon's own shape/transparency and colours only its pixels.
            val alpha = (a.tintStrength.coerceIn(0f, 1f) * 255).toInt()
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (tint.toInt() and 0x00FFFFFF) or (alpha shl 24)
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            }
            c.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), p)
        }
        return out
    }

    private fun badge(src: Bitmap, color: Int, text: String): Bitmap {
        val bmp = src.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(bmp)
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
