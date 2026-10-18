package com.twinndroid.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal const val MESH_COLS = 8
internal const val MESH_ROWS = 12
private const val TAU = 6.2831855f

private fun fract(v: Float) = v - floor(v)

/** Cheap deterministic hash in 0..1, used to give each "life" of a particle a fresh position. */
private fun hash(i: Int, cycle: Float, salt: Float = 0f): Float =
    fract(sin(i * 12.9898f + cycle * 78.233f + salt * 37.719f) * 43758.547f)

private fun smooth(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

/** One frame of one style. Coordinates are in the full-screen [w]×[h] space. */
internal class Scene(
    private val ds: DrawScope,
    private val b: Backdrop,
    private val w: Float,
    private val h: Float,
    private val t: Float,
    private val p: Offset,
    private val c: BackdropColors,
    private val cull: Rect?,
) : DrawScope by ds {
    private val dp = ds.density
    private val k = c.k

    private fun visible(x: Float, y: Float, r: Float): Boolean =
        cull == null || (x + r >= cull.left && x - r <= cull.right && y + r >= cull.top && y - r <= cull.bottom)

    private fun pick(i: Int): Color = when (i % 3) { 0 -> c.c1; 1 -> c.c2; else -> c.c3 }

    // ---- Space: a dense, twinkling dust layer plus a warp layer of stars flying toward you ----
    fun space() {
        val tints = if (c.dark) arrayOf(Color.White, Color(0xFFCAD8FF), Color(0xFFFFE3BD)) else arrayOf(c.ink, Color(0xFF233B70), Color(0xFF5A4022))
        val dust = b.field
        for (i in 0 until dust.count) {
            val d = dust.depth[i]
            val x = dust.x[i] * w + p.x * d * 0.35f
            val y = dust.y[i] * h + p.y * d * 0.35f
            val r = (0.35f + dust.size[i] * 0.85f) * dp
            if (!visible(x, y, r)) continue
            val tw = 0.2f + 0.8f * (0.5f + 0.5f * sin(t * (0.6f + dust.speed[i] * 1.6f) + dust.phase[i]))
            drawCircle(tints[dust.kind[i]], r, Offset(x, y), alpha = tw * 0.9f * k)
        }
        // Warp: each star travels from the far plane (z=1) to the camera (z→0), then starts a new
        // life somewhere else. Projection = centre + direction / z, so they stream outward.
        val warp = b.field2
        val cx = w / 2 + p.x * 0.6f
        val cy = h * 0.45f + p.y * 0.6f
        val reach = max(w, h) * 0.5f
        for (i in 0 until warp.count) {
            val prog = t * 0.05f * warp.speed[i] + warp.phase[i] / TAU
            val life = floor(prog)
            val z = (1f - (prog - life)).coerceAtLeast(0.02f)
            val ang = hash(i, life) * TAU
            val dist = 0.08f + sqrt(hash(i, life, 1f)) * 0.9f
            val persp = 0.14f / z
            val dx = cos(ang) * dist * reach
            val dy = sin(ang) * dist * reach
            val x = cx + dx * persp
            val y = cy + dy * persp
            val near = 1f - z
            val r = (0.45f + warp.size[i] * 0.8f) * dp * (0.5f + near * near * 1.5f)
            if (!visible(x, y, r + 30f)) continue
            val a = smooth(0f, 0.25f, near) * (if (z < 0.06f) z / 0.06f else 1f) * k
            val tint = tints[warp.kind[i]]
            if (z < 0.2f) {
                val tp = 0.14f / (z + 0.02f)
                drawLine(tint, Offset(cx + dx * tp, cy + dy * tp), Offset(x, y), r * 0.7f, StrokeCap.Round, alpha = a * 0.25f)
            }
            drawCircle(tint, r, Offset(x, y), alpha = a)
        }
        meteor(period = 21f, slot = 0)
    }

    /** An occasional shooting star (one every [period] seconds). */
    private fun meteor(period: Float, slot: Int, length: Float = 150f) {
        val shifted = t + slot * period * 0.37f
        val cyc = floor(shifted / period)
        val local = shifted - cyc * period
        val dur = 1.1f
        if (local > dur) return
        val pr = local / dur
        // Heads down and to the left or right, at a shallow angle.
        val left = hash(slot, cyc, 7f) > 0.5f
        val ang = ((if (left) 140f else 25f) + hash(slot, cyc, 5f) * 15f) * (PI.toFloat() / 180f)
        val dx = cos(ang)
        val dy = sin(ang)
        val sx = (if (left) 0.45f + hash(slot, cyc, 3f) * 0.55f else hash(slot, cyc, 3f) * 0.55f) * w
        val sy = hash(slot, cyc, 4f) * h * 0.4f
        val travel = (260f + hash(slot, cyc, 6f) * 160f) * dp
        val hx = sx + dx * travel * pr
        val hy = sy + dy * travel * pr
        val len = length * dp * (1f - abs(pr - 0.5f))
        val tx = hx - dx * len
        val ty = hy - dy * len
        if (!visible((hx + tx) / 2, (hy + ty) / 2, len)) return
        val fade = sin(pr * PI.toFloat())
        drawLine(
            Brush.linearGradient(listOf(Color.Transparent, c.ink.copy(alpha = 0.9f * fade * k)), Offset(tx, ty), Offset(hx, hy)),
            Offset(tx, ty), Offset(hx, hy), 1.6f * dp, StrokeCap.Round,
        )
        drawCircle(c.ink, 1.6f * dp, Offset(hx, hy), alpha = fade * k)
    }

    // ---- Particles (unchanged look): drifting dots joined by lines when close ----
    fun particles() {
        val f = b.field
        val maxD = 130f * dp
        val xs = b.buf
        for (i in 0 until f.count) {
            xs[i * 2] = fract(f.x[i] + f.vx[i] * t * 0.012f) * w + p.x * f.depth[i]
            xs[i * 2 + 1] = fract(f.y[i] + f.vy[i] * t * 0.012f) * h + p.y * f.depth[i]
        }
        for (i in 0 until f.count) {
            val xi = xs[i * 2]
            val yi = xs[i * 2 + 1]
            for (j in i + 1 until f.count) {
                val dx = xi - xs[j * 2]
                val dy = yi - xs[j * 2 + 1]
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < maxD && (visible(xi, yi, maxD) || visible(xs[j * 2], xs[j * 2 + 1], maxD))) {
                    drawLine(c.c1, Offset(xi, yi), Offset(xs[j * 2], xs[j * 2 + 1]), dp, alpha = (1f - dist / maxD) * 0.35f * k)
                }
            }
            val r = (2f + f.size[i] * 2f) * dp
            if (visible(xi, yi, r)) drawCircle(if (f.kind[i] == 0) c.c2 else c.c1, r, Offset(xi, yi), alpha = 0.8f * k)
        }
    }

    // ---- Circles: a few large, flat, very faint discs drifting slowly ----
    private val circleSpec = floatArrayOf(
        // x, y, radiusDp, alpha
        0.18f, 0.20f, 140f, 0.060f,
        0.84f, 0.14f, 96f, 0.050f,
        0.70f, 0.42f, 70f, 0.055f,
        0.86f, 0.74f, 118f, 0.050f,
        0.22f, 0.68f, 82f, 0.055f,
        0.50f, 0.92f, 92f, 0.050f,
        0.44f, 0.46f, 54f, 0.045f,
    )

    fun circles() {
        val boost = if (c.dark) 1f else 1.6f
        for (i in 0 until circleSpec.size / 4) {
            val bx = circleSpec[i * 4]
            val by = circleSpec[i * 4 + 1]
            val r = circleSpec[i * 4 + 2] * dp
            val x = (bx + 0.045f * sin(t * TAU / (7.4f + i * 0.9f) + i)) * w + p.x * (0.4f + i * 0.08f) * 1.8f
            val y = (by + 0.035f * cos(t * TAU / (8.2f + i * 0.7f) + i * 1.3f)) * h + p.y * (0.4f + i * 0.08f) * 1.8f
            if (!visible(x, y, r)) continue
            drawCircle(pick(i), r, Offset(x, y), alpha = circleSpec[i * 4 + 3] * boost)
        }
    }

    // ---- Rings (unchanged look): rings expanding from two corners ----
    fun rings() {
        val maxR = max(w, h) * 0.75f
        for (ci in 0..1) {
            val c0 = if (ci == 0) Offset(w * 0.15f, h * 0.2f) else Offset(w * 0.9f, h * 0.75f)
            val center = c0 + p * (1f + ci * 0.6f)
            for (n in 0 until 6) {
                val frac = fract(t * 0.035f + n / 6f + ci * 0.5f)
                drawCircle(if (ci == 0) c.c1 else c.c2, frac * maxR, center, alpha = (1f - frac) * 0.35f * k, style = Stroke(1.5f * dp))
            }
        }
    }

    // ---- Mesh: a low-poly 3D surface rolling in waves, faces shaded by the light they catch ----
    fun mesh() {
        val n = MESH_COLS * MESH_ROWS
        val sx = b.buf          // 0 until n
        val sy = n               // offsets inside buf
        val wx = n * 2
        val wy = n * 3
        val wz = n * 4
        val buf = b.buf
        val tilt = 0.62f
        val ct = cos(tilt)
        val st = sin(tilt)
        val cam = 2.4f
        val tt = t * 0.32f
        for (r in 0 until MESH_ROWS) for (col in 0 until MESH_COLS) {
            val i = r * MESH_COLS + col
            val bx = col / (MESH_COLS - 1f)
            val by = r / (MESH_ROWS - 1f)
            val x = bx + 0.028f * sin(tt * (1f + bx * 0.5f) + bx * 6.1f + b.field.phase[i])
            val y = by + 0.022f * cos(tt * (1.1f + by * 0.6f) + by * 8.7f)
            val z = 0.13f * sin(tt * 0.9f + bx * 3.1f + by * 2.3f) + 0.07f * sin(tt * 1.6f + by * 5.2f - bx * 2.4f + b.field.phase[i] * 0.3f)
            val X = (x - 0.5f) * 3.6f
            val Y = (y - 0.5f) * 3.2f
            val ry = Y * ct - z * st
            val rz = Y * st + z * ct
            val persp = cam / (cam - rz)
            buf[wx + i] = X; buf[wy + i] = ry; buf[wz + i] = rz
            sx[i] = X * persp * w * 0.42f + w / 2 + p.x * (0.6f + by)
            buf[sy + i] = ry * persp * h * 0.6f + h * 0.48f + p.y * (0.6f + by)
        }
        val path = b.path
        val stroke = 1.1f * dp
        fun tri(a: Int, bI: Int, cI: Int, color: Color) {
            // Face normal from the 3D positions; light comes from the upper left, toward the viewer.
            val ux = buf[wx + bI] - buf[wx + a]; val uy = buf[wy + bI] - buf[wy + a]; val uz = buf[wz + bI] - buf[wz + a]
            val vx = buf[wx + cI] - buf[wx + a]; val vy = buf[wy + cI] - buf[wy + a]; val vz = buf[wz + cI] - buf[wz + a]
            var nx = uy * vz - uz * vy; var ny = uz * vx - ux * vz; var nz = ux * vy - uy * vx
            val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-4f)
            nx /= len; ny /= len; nz /= len
            val light = abs(nx * -0.35f + ny * -0.55f + nz * 0.76f)
            val minX = min(sx[a], min(sx[bI], sx[cI])); val maxX = max(sx[a], max(sx[bI], sx[cI]))
            val minY = min(buf[sy + a], min(buf[sy + bI], buf[sy + cI])); val maxY = max(buf[sy + a], max(buf[sy + bI], buf[sy + cI]))
            if (cull != null && (maxX < cull.left || minX > cull.right || maxY < cull.top || minY > cull.bottom)) return
            path.reset()
            path.moveTo(sx[a], buf[sy + a]); path.lineTo(sx[bI], buf[sy + bI]); path.lineTo(sx[cI], buf[sy + cI]); path.close()
            drawPath(path, color, alpha = (0.025f + light * light * 0.13f) * k)
        }
        for (r in 0 until MESH_ROWS - 1) for (col in 0 until MESH_COLS - 1) {
            val i = r * MESH_COLS + col
            val f = r / (MESH_ROWS - 2f)
            val color = if (f < 0.5f) lerp(c.c1, c.c2, f * 2) else lerp(c.c2, c.c3, (f - 0.5f) * 2)
            tri(i, i + 1, i + MESH_COLS, color)
            tri(i + 1, i + MESH_COLS + 1, i + MESH_COLS, color)
        }
        for (r in 0 until MESH_ROWS) for (col in 0 until MESH_COLS) {
            val i = r * MESH_COLS + col
            val f = (r / (MESH_ROWS - 1f))
            val color = if (f < 0.5f) lerp(c.c1, c.c2, f * 2) else lerp(c.c2, c.c3, (f - 0.5f) * 2)
            val a = (0.10f + 0.12f * f) * k
            val from = Offset(sx[i], buf[sy + i])
            if (!visible(from.x, from.y, 200f)) continue
            if (col < MESH_COLS - 1) drawLine(color, from, Offset(sx[i + 1], buf[sy + i + 1]), stroke, alpha = a)
            if (r < MESH_ROWS - 1) drawLine(color, from, Offset(sx[i + MESH_COLS], buf[sy + i + MESH_COLS]), stroke, alpha = a)
            if (r < MESH_ROWS - 1 && col < MESH_COLS - 1) drawLine(color, Offset(sx[i + 1], buf[sy + i + 1]), Offset(sx[i + MESH_COLS], buf[sy + i + MESH_COLS]), stroke, alpha = a * 0.7f)
            drawCircle(color, 1.4f * dp, from, alpha = a * 1.6f)
        }
    }

    // ---- Shapes: real 3D solids tumbling along their own paths ----
    private val shapeSpec = arrayOf(
        // solid, cx, cy, sizeDp, motion(0 wander,1 orbit,2 drift), colour, depth
        floatArrayOf(3f, 0.22f, 0.16f, 46f, 0f, 0f, 0.8f),
        floatArrayOf(1f, 0.78f, 0.24f, 34f, 1f, 1f, 0.5f),
        floatArrayOf(2f, 0.55f, 0.42f, 28f, 2f, 2f, 0.4f),
        floatArrayOf(0f, 0.16f, 0.52f, 30f, 1f, 2f, 0.6f),
        floatArrayOf(3f, 0.84f, 0.62f, 38f, 0f, 0f, 0.7f),
        floatArrayOf(2f, 0.30f, 0.82f, 40f, 0f, 1f, 0.9f),
        floatArrayOf(1f, 0.70f, 0.88f, 26f, 2f, 0f, 0.3f),
        floatArrayOf(0f, 0.48f, 0.66f, 22f, 1f, 1f, 0.5f),
    )

    fun shapes() {
        val path = b.path
        val rv = b.buf
        shapeSpec.forEachIndexed { i, s ->
            val solid = Solids.all[s[0].toInt()]
            val ph = i * 1.37f
            val (cx, cy) = when (s[4].toInt()) {
                0 -> (s[1] + 0.08f * sin(t * 0.09f + ph)) to (s[2] + 0.05f * sin(t * 0.13f + ph * 1.7f))
                1 -> (s[1] + 0.09f * cos(t * 0.07f + ph)) to (s[2] + 0.055f * sin(t * 0.07f + ph))
                else -> (fract(s[1] + t * 0.006f * (if (i % 2 == 0) 1 else -1)) * 1.3f - 0.15f) to (s[2] + 0.02f * sin(t * 0.3f + ph))
            }
            val breathe = 1f + 0.16f * sin(t * 0.11f + ph)
            val size = s[3] * dp * breathe
            val ox = cx * w + p.x * s[6] * 2f
            val oy = cy * h + p.y * s[6] * 2f
            if (!visible(ox, oy, size * 1.6f)) return@forEachIndexed
            val ax = t * (0.21f + i * 0.02f) + 0.5f * sin(t * 0.17f + ph)
            val ay = t * (0.29f - i * 0.015f) + 0.5f * sin(t * 0.13f + ph * 2)
            val az = t * 0.09f + ph
            val cxr = cos(ax); val sxr = sin(ax); val cyr = cos(ay); val syr = sin(ay); val czr = cos(az); val szr = sin(az)
            val v = solid.verts
            val nv = v.size / 3
            for (j in 0 until nv) {
                var x = v[j * 3]; var y = v[j * 3 + 1]; var z = v[j * 3 + 2]
                var t1 = y * cxr - z * sxr; z = y * sxr + z * cxr; y = t1
                t1 = x * cyr + z * syr; z = -x * syr + z * cyr; x = t1
                t1 = x * czr - y * szr; y = x * szr + y * czr; x = t1
                val f = 3f / (3f + z)
                rv[j * 3] = ox + x * size * f
                rv[j * 3 + 1] = oy + y * size * f
                rv[j * 3 + 2] = z
                // 3D (unprojected) copy for lighting
                rv[200 + j * 3] = x; rv[200 + j * 3 + 1] = y; rv[200 + j * 3 + 2] = z
            }
            val color = pick(s[5].toInt())
            val depthA = (0.55f + 0.45f * s[6]) * (0.8f + 0.2f * breathe) * k
            for (face in solid.faces) {
                var mx = 0f; var my = 0f; var mz = 0f
                for (vi in face) { mx += rv[200 + vi * 3]; my += rv[200 + vi * 3 + 1]; mz += rv[200 + vi * 3 + 2] }
                if (mz >= 0f) continue // faces away from the viewer
                val len = sqrt(mx * mx + my * my + mz * mz)
                val light = ((mx * -0.45f + my * -0.6f + mz * -0.66f) / len).coerceAtLeast(0f)
                path.reset()
                face.forEachIndexed { n, vi -> if (n == 0) path.moveTo(rv[vi * 3], rv[vi * 3 + 1]) else path.lineTo(rv[vi * 3], rv[vi * 3 + 1]) }
                path.close()
                drawPath(path, color, alpha = (0.04f + light * 0.2f) * depthA)
            }
            val e = solid.edges
            for (n in 0 until e.size / 2) {
                val a = e[n * 2]; val bI = e[n * 2 + 1]
                val zAvg = (rv[a * 3 + 2] + rv[bI * 3 + 2]) / 2f
                val alpha = (0.55f - (zAvg + 1f) / 2f * 0.45f).coerceIn(0.08f, 0.6f) * depthA
                drawLine(color, Offset(rv[a * 3], rv[a * 3 + 1]), Offset(rv[bI * 3], rv[bI * 3 + 1]), 1.2f * dp, alpha = alpha)
            }
        }
    }

    // ---- Snow: six-armed flakes in three depths, gusting wind, melting partway down ----
    fun snow() {
        val sprites = Snowflakes.sprites(b, c.ink)
        val f = b.field
        val g = sin(t * 0.11f) * sin(t * 0.047f + 1f)
        val gust = g * abs(g)
        for (i in 0 until f.count) {
            val d = f.depth[i]
            val layer = if (d > 0.75f) 0 else if (d > 0.5f) 1 else 2
            val period = when (layer) { 0 -> 13f; 1 -> 20f; else -> 30f } / (0.8f + f.speed[i] * 0.4f)
            val travel = t / period + f.y[i]
            val life = floor(travel)
            val prog = travel - life
            val meltAt = 0.55f + hash(i, life, 2f) * 0.45f
            if (prog > meltAt) continue
            val fade = smooth(0f, 0.06f, prog) * smooth(0f, 0.08f, meltAt - prog)
            val sway = sin(t * (0.4f + f.speed[i] * 0.5f) + f.phase[i]) * 0.018f
            val x = fract(hash(i, life) + sway + gust * (0.03f + 0.08f * d)) * w + p.x * d * 1.5f
            val y = prog * (h + 60f) - 30f + p.y * d * 1.5f
            if (layer == 2) {
                val r = (1.4f + f.size[i] * 1.8f) * dp
                if (!visible(x, y, r * 2)) continue
                drawCircle(c.ink, r * 2.2f, Offset(x, y), alpha = 0.08f * fade * k)
                drawCircle(c.ink, r, Offset(x, y), alpha = 0.35f * fade * k)
            } else {
                val sz = (if (layer == 0) 18f + f.size[i] * 10f else 10f + f.size[i] * 6f) * dp
                if (!visible(x, y, sz)) continue
                val img = sprites[layer]
                val alpha = (if (layer == 0) 0.85f else 0.6f) * fade * k
                rotate(t * 25f * f.vx[i] + f.phase[i] * 57f, Offset(x, y)) {
                    drawImage(
                        img,
                        dstOffset = IntOffset((x - sz / 2).toInt(), (y - sz / 2).toInt()),
                        dstSize = IntSize(sz.toInt(), sz.toInt()),
                        alpha = alpha,
                    )
                }
            }
        }
    }

    // ---- Grid (unchanged look): dot grid with a ripple travelling outward ----
    fun grid() {
        val step = 30f * dp
        val cx = w / 2 + p.x * 2
        val cy = h * 0.4f + p.y * 2
        var y = (p.y % step) - step
        while (y < h + step) {
            var x = (p.x % step) - step
            if (cull == null || (y >= cull.top - step && y <= cull.bottom + step)) {
                while (x < w + step) {
                    if (visible(x, y, step)) {
                        val dx = x - cx
                        val dy = y - cy
                        val wave = 0.5f + 0.5f * sin(sqrt(dx * dx + dy * dy) / (60f * dp) - t * 1.4f)
                        drawCircle(if (wave > 0.85f) c.c1 else c.ink, (0.8f + wave * 1.6f) * dp, Offset(x, y), alpha = (0.12f + wave * 0.45f) * k)
                    }
                    x += step
                }
            }
            y += step
        }
    }

    // ---- Lava lamp: wax blobs that rise, sink, stretch and merge (metaballs on Android 13+) ----
    fun lava() {
        val n = LavaShader.BLOBS
        val bx = b.buf
        for (i in 0 until n) {
            val ph = i * 1.91f
            val climb = TAU * t / (26f + i * 5.5f) + ph
            val sway = sin(TAU * t / (17f + i * 3f) + ph * 1.3f)
            val breath = sin(TAU * t / (9f + i * 1.7f) + ph)
            val travel = 0.3f + (i % 3) * 0.05f
            bx[i * 4] = (0.12f + (i * 0.37f) % 0.76f + 0.05f * sway) * w + p.x * (1f + i % 3)
            bx[i * 4 + 1] = (0.5f - travel * sin(climb)) * h + p.y * (1f + i % 3)
            bx[i * 4 + 2] = (34f + (i * 17 % 26)) * dp * (1f + 0.1f * breath)
            bx[i * 4 + 3] = 1f + 0.35f * abs(cos(climb))
        }
        val shader = b.lava
        if (shader != null && android.os.Build.VERSION.SDK_INT >= 33) {
            drawRect(shader.brush(bx, c, if (c.dark) 0.3f else 0.22f), size = Size(w, h))
        } else {
            for (i in 0 until n) {
                val o = Offset(bx[i * 4], bx[i * 4 + 1])
                val r = bx[i * 4 + 2] * 1.5f
                if (!visible(o.x, o.y, r)) continue
                val col = pick(i)
                drawCircle(Brush.radialGradient(0f to col.copy(alpha = 0.32f * k), 0.6f to col.copy(alpha = 0.14f * k), 1f to Color.Transparent, center = o, radius = r), r, o)
            }
        }
    }

    // ---- Aurora: soft glowing curtains with faint vertical rays and a bright lower hem ----
    fun aurora() {
        val path = b.path
        val cols = 48
        for (band in 0 until 3) {
            val color = when (band) { 0 -> c.c2; 1 -> c.c1; else -> c.c3 }
            val base = h * (0.26f + band * 0.12f) + p.y * (1 + band)
            val amp = (30f + band * 10f) * dp
            val speed = 0.1f + band * 0.04f
            val strength = if (band == 2) 0.6f else 1f
            fun edge(x: Float) = base + amp * sin(x / w * 2.6f + t * speed + band * 1.7f) + amp * 0.25f * sin(x / w * 5.1f - t * speed * 1.3f)
            fun ray(x: Float) = (150f + band * 30f) * dp * (0.65f + 0.35f * sin(x / w * 4.2f + t * 0.35f + band * 2f))
            path.reset()
            for (s in 0..cols) {
                val x = w * s / cols
                if (s == 0) path.moveTo(x + p.x, edge(x) - ray(x)) else path.lineTo(x + p.x, edge(x) - ray(x))
            }
            for (s in cols downTo 0) {
                val x = w * s / cols
                path.lineTo(x + p.x, edge(x))
            }
            path.close()
            val top = base - amp - (180f + band * 30f) * dp
            drawPath(
                path,
                Brush.verticalGradient(
                    listOf(Color.Transparent, color.copy(alpha = 0.07f * k * strength), color.copy(alpha = 0.22f * k * strength)),
                    startY = top,
                    endY = base + amp,
                ),
            )
            for (s in 0..cols * 2) {
                val x = w * s / (cols * 2)
                val shimmer = 0.5f + 0.5f * sin(x * 0.03f + t * (0.9f + band * 0.3f) + s * 0.45f)
                val y0 = edge(x)
                if (!visible(x, y0, 300f)) continue
                drawLine(color, Offset(x + p.x, y0), Offset(x + p.x, y0 - ray(x) * (0.55f + 0.4f * shimmer)), 1.2f * dp, alpha = 0.06f * shimmer * k * strength)
            }
            path.reset()
            for (s in 0..cols) {
                val x = w * s / cols
                if (s == 0) path.moveTo(x + p.x, edge(x)) else path.lineTo(x + p.x, edge(x))
            }
            drawPath(path, color, alpha = 0.06f * k * strength, style = Stroke(9f * dp, cap = StrokeCap.Round))
            drawPath(path, color, alpha = 0.24f * k * strength, style = Stroke(1.8f * dp, cap = StrokeCap.Round))
        }
    }

    // ---- Waves: the original layered sine bands ----
    fun waves() {
        val path = b.path
        val segments = 36
        for (band in 0 until 3) {
            val color = pick(band)
            val base = h * (0.22f + band * 0.16f) + p.y * (1 + band)
            val amp = (40f + band * 16f) * dp
            path.reset()
            path.moveTo(0f, base)
            for (s in 0..segments) {
                val x = w * s / segments
                val y = base + amp * sin(x / w * 2.4f * PI.toFloat() + t * (0.25f + band * 0.07f) + band) + amp * 0.4f * sin(x / w * 5f + t * 0.4f)
                path.lineTo(x + p.x * (1 + band), y)
            }
            path.lineTo(w, base + amp * 4)
            path.lineTo(0f, base + amp * 4)
            path.close()
            drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f * k), Color.Transparent), startY = base - amp, endY = base + amp * 4))
        }
    }

    // ---- Fireflies: warm points of light that wander and pulse ----
    fun fireflies() {
        val f = b.field
        for (i in 0 until f.count) {
            val sp = f.speed[i]
            val x = (f.x[i] + 0.07f * sin(t * 0.13f * sp + f.phase[i]) + 0.03f * sin(t * 0.37f + f.phase[i] * 2)) * w + p.x * f.depth[i] * 2
            val y = (fract(f.y[i] - t * 0.004f * sp) + 0.05f * cos(t * 0.17f * sp + f.phase[i])) * h + p.y * f.depth[i] * 2
            val pulse = (0.5f + 0.5f * sin(t * (0.9f + sp) + f.phase[i])).let { it * it }
            val r = (10f + f.size[i] * 10f) * dp * (0.6f + pulse * 0.6f)
            if (!visible(x, y, r)) continue
            val col = if (f.kind[i] == 0) c.c2 else c.c3
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.32f * pulse * k), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
            drawCircle(lerp(col, Color.White, 0.5f), 1.6f * dp, Offset(x, y), alpha = (0.3f + 0.7f * pulse) * k)
        }
    }

    // ---- Bokeh: soft out-of-focus lights drifting upward ----
    fun bokeh() {
        val f = b.field
        for (i in 0 until f.count) {
            val r = (26f + f.size[i] * 64f) * dp
            val x = (f.x[i] + 0.04f * sin(t * 0.05f + f.phase[i])) * w + p.x * f.depth[i] * 3
            val y = (1f - fract(f.y[i] + t * 0.006f * f.speed[i])) * (h + 2 * r) - r + p.y * f.depth[i] * 3
            if (!visible(x, y, r)) continue
            val col = pick(f.kind[i])
            val a = (0.5f + 0.5f * sin(t * 0.3f + f.phase[i])) * 0.5f + 0.5f
            drawCircle(col, r, Offset(x, y), alpha = 0.06f * a * k)
            drawCircle(col, r, Offset(x, y), alpha = 0.12f * a * k, style = Stroke(1.5f * dp))
        }
    }

    // ---- Meteors: a still sky with frequent shooting stars ----
    fun meteors() {
        val f = b.field
        for (i in 0 until f.count) {
            val x = f.x[i] * w + p.x * f.depth[i] * 0.5f
            val y = f.y[i] * h + p.y * f.depth[i] * 0.5f
            val r = (0.4f + f.size[i] * 0.9f) * dp
            if (!visible(x, y, r)) continue
            val tw = 0.3f + 0.7f * (0.5f + 0.5f * sin(t * (0.5f + f.speed[i]) + f.phase[i]))
            drawCircle(if (f.kind[i] == 0) c.c1 else c.ink, r, Offset(x, y), alpha = tw * 0.7f * k)
        }
        for (slot in 0 until 4) meteor(period = 4.5f + slot * 1.3f, slot = slot, length = 120f)
    }
}
