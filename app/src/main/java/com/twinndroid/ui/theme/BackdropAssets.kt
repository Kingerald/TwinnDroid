package com.twinndroid.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A convex polyhedron centred on the origin, radius about 1. Edges are derived from the faces. */
internal class Solid(val verts: FloatArray, val faces: Array<IntArray>) {
    val edges: IntArray = run {
        val set = LinkedHashSet<Long>()
        for (f in faces) for (i in f.indices) {
            val a = f[i]
            val b = f[(i + 1) % f.size]
            set += if (a < b) (a.toLong() shl 32) or b.toLong() else (b.toLong() shl 32) or a.toLong()
        }
        IntArray(set.size * 2).also { out ->
            set.forEachIndexed { n, key -> out[n * 2] = (key shr 32).toInt(); out[n * 2 + 1] = key.toInt() }
        }
    }
}

internal object Solids {
    private val tetra = Solid(
        floatArrayOf(1f, 1f, 1f, 1f, -1f, -1f, -1f, 1f, -1f, -1f, -1f, 1f).map { it * 0.62f }.toFloatArray(),
        arrayOf(intArrayOf(0, 1, 2), intArrayOf(0, 1, 3), intArrayOf(0, 2, 3), intArrayOf(1, 2, 3)),
    )
    private val cube = Solid(
        FloatArray(24) { i -> if ((i / 3) shr (2 - i % 3) and 1 == 1) 0.62f else -0.62f },
        arrayOf(
            intArrayOf(0, 1, 3, 2), intArrayOf(4, 6, 7, 5), intArrayOf(0, 4, 5, 1),
            intArrayOf(2, 3, 7, 6), intArrayOf(0, 2, 6, 4), intArrayOf(1, 5, 7, 3),
        ),
    )
    private val octa = Solid(
        floatArrayOf(1f, 0f, 0f, -1f, 0f, 0f, 0f, 1f, 0f, 0f, -1f, 0f, 0f, 0f, 1f, 0f, 0f, -1f),
        arrayOf(
            intArrayOf(0, 2, 4), intArrayOf(0, 4, 3), intArrayOf(0, 3, 5), intArrayOf(0, 5, 2),
            intArrayOf(1, 4, 2), intArrayOf(1, 3, 4), intArrayOf(1, 5, 3), intArrayOf(1, 2, 5),
        ),
    )
    private val icosa = run {
        val g = (1f + sqrt(5f)) / 2f
        val s = 1f / sqrt(1f + g * g)
        Solid(
            floatArrayOf(
                -1f, g, 0f, 1f, g, 0f, -1f, -g, 0f, 1f, -g, 0f,
                0f, -1f, g, 0f, 1f, g, 0f, -1f, -g, 0f, 1f, -g,
                g, 0f, -1f, g, 0f, 1f, -g, 0f, -1f, -g, 0f, 1f,
            ).map { it * s }.toFloatArray(),
            arrayOf(
                intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7), intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
                intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
                intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
                intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10), intArrayOf(8, 6, 7), intArrayOf(9, 8, 1),
            ),
        )
    }

    /** Index order is referenced by the Shapes layout: tetrahedron, cube, octahedron, icosahedron. */
    val all = arrayOf(tetra, cube, octa, icosa)
}

/** Six-armed snowflake sprites (near: detailed, middle: simple), drawn once per ink colour. */
internal object Snowflakes {
    fun sprites(b: Backdrop, ink: Color): Array<ImageBitmap> {
        b.sprites?.takeIf { b.spriteInk == ink }?.let { return it }
        val out = arrayOf(flake(64, ink, detailed = true), flake(40, ink, detailed = false))
        b.sprites = out
        b.spriteInk = ink
        return out
    }

    private fun flake(size: Int, ink: Color, detailed: Boolean): ImageBitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink.toArgb()
            strokeWidth = size * (if (detailed) 0.05f else 0.07f)
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val cx = size / 2f
        val arm = size * 0.44f
        for (k in 0 until 6) {
            val a = Math.toRadians(k * 60.0 - 90.0)
            val ux = cos(a).toFloat()
            val uy = sin(a).toFloat()
            c.drawLine(cx, cx, cx + ux * arm, cx + uy * arm, paint)
            val branches = if (detailed) floatArrayOf(0.45f, 0.7f) else floatArrayOf(0.6f)
            for (pos in branches) {
                val bx = cx + ux * arm * pos
                val by = cx + uy * arm * pos
                val len = arm * (if (pos < 0.5f) 0.32f else 0.22f)
                for (side in intArrayOf(-1, 1)) {
                    val ba = a + side * Math.toRadians(45.0)
                    c.drawLine(bx, by, bx + cos(ba).toFloat() * len, by + sin(ba).toFloat() * len, paint)
                }
            }
        }
        if (detailed) c.drawCircle(cx, cx, size * 0.06f, paint.apply { style = Paint.Style.FILL })
        return bmp.asImageBitmap()
    }
}

/** Metaball shader for the lava lamp: blobs sum into a field; above 1 is wax, so close blobs merge. */
@RequiresApi(33)
internal class LavaShader {
    private val shader = RuntimeShader(
        """
        uniform float4 blobs[$BLOBS];
        uniform float4 tints[$BLOBS];
        uniform float strength;
        half4 main(float2 pos) {
            float field = 0.0;
            float3 acc = float3(0.0);
            for (int i = 0; i < $BLOBS; i++) {
                float2 d = pos - blobs[i].xy;
                d.y = d.y / blobs[i].w;
                float v = (blobs[i].z * blobs[i].z) / max(dot(d, d), 1.0);
                field += v;
                acc += tints[i].rgb * v;
            }
            float3 col = acc / max(field, 0.0001);
            float core = smoothstep(0.88, 1.12, field);
            float rim = smoothstep(0.35, 0.95, field) * 0.3;
            float shine = smoothstep(1.4, 3.5, field) * 0.3;
            col = col + (float3(1.0) - col) * shine * core;
            float a = max(core, rim) * strength;
            return half4(col * a, a);
        }
        """.trimIndent(),
    )
    private val brush = ShaderBrush(shader)
    private val tints = FloatArray(BLOBS * 4)

    fun brush(blobs: FloatArray, colors: BackdropColors, strength: Float): Brush {
        shader.setFloatUniform("blobs", blobs.copyOf(BLOBS * 4))
        for (i in 0 until BLOBS) {
            val c = when (i % 3) { 0 -> colors.c1; 1 -> colors.c2; else -> colors.c3 }
            tints[i * 4] = c.red; tints[i * 4 + 1] = c.green; tints[i * 4 + 2] = c.blue; tints[i * 4 + 3] = 1f
        }
        shader.setFloatUniform("tints", tints)
        shader.setFloatUniform("strength", strength)
        return brush
    }

    companion object {
        const val BLOBS = 7
    }
}
