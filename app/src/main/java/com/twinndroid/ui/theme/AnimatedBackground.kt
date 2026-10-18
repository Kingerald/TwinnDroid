package com.twinndroid.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Seeded per-element values, stored in flat arrays so drawing allocates nothing per frame. */
private class Field(n: Int, seed: Int) {
    val count = n
    private val rnd = Random(seed)
    val x = FloatArray(n) { rnd.nextFloat() }
    val y = FloatArray(n) { rnd.nextFloat() }
    val size = FloatArray(n) { rnd.nextFloat() }
    val speed = FloatArray(n) { 0.4f + rnd.nextFloat() * 0.6f }
    val phase = FloatArray(n) { rnd.nextFloat() * 2f * PI.toFloat() }
    val depth = FloatArray(n) { 0.3f + rnd.nextFloat() * 0.7f }
    val vx = FloatArray(n) { rnd.nextFloat() - 0.5f }
    val vy = FloatArray(n) { rnd.nextFloat() - 0.5f }
    val kind = IntArray(n) { rnd.nextInt(3) }
}

private fun wrap(v: Float): Float = v - kotlin.math.floor(v)

/**
 * The full-screen animated backdrop behind every screen. Animation time is read only inside
 * the draw lambda, so each frame is a redraw — no recomposition, no layout. Frames only run
 * while the app is resumed, and the system "Remove animations" setting freezes it.
 */
@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val extras = LocalTwinnExtras.current
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val style = remember(extras.background) {
        if (extras.background == BgAnimation.RANDOM) {
            BgAnimation.entries.filter { it != BgAnimation.NONE && it != BgAnimation.RANDOM }.random()
        } else {
            extras.background
        }
    }
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val time = remember { mutableFloatStateOf(17f) }
    val lifecycle = LocalLifecycleOwner.current
    if (style != BgAnimation.NONE && !reduceMotion) {
        LaunchedEffect(style, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var last = 0L
                while (true) {
                    androidx.compose.runtime.withFrameNanos { now ->
                        if (last != 0L) time.floatValue += ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                        last = now
                    }
                }
            }
        }
    }
    val tilt = rememberTilt(extras.parallax && style != BgAnimation.NONE && !reduceMotion)

    val field = remember(style) {
        when (style) {
            BgAnimation.SPACE -> Field(110, 7)
            BgAnimation.SNOW -> Field(80, 11)
            BgAnimation.PARTICLES -> Field(34, 3)
            BgAnimation.SHAPES -> Field(14, 5)
            BgAnimation.CIRCLES -> Field(8, 13)
            BgAnimation.LAVA -> Field(6, 17)
            else -> Field(4, 19)
        }
    }
    val path = remember { Path() }

    val bg = scheme.background
    val dark = bg.luminance() < 0.5f
    val ink = if (dark) Color.White else Color.Black
    val c1 = scheme.primary
    val c2 = scheme.secondary
    val c3 = scheme.tertiary
    val strength = if (dark) 1f else 0.6f

    Spacer(
        modifier.fillMaxSize().drawBehind {
            drawRect(bg)
            if (style == BgAnimation.NONE) return@drawBehind
            val t = time.floatValue
            val p = tilt.value * 28.dp.toPx()
            when (style) {
                BgAnimation.SPACE -> space(field, t, p, ink, c1, strength)
                BgAnimation.SNOW -> snow(field, t, p, ink, strength)
                BgAnimation.PARTICLES -> particles(field, t, p, c1, c2, strength)
                BgAnimation.CIRCLES -> circles(field, t, p, c1, c2, c3, strength)
                BgAnimation.RINGS -> rings(t, p, c1, c2, strength)
                BgAnimation.MESH -> mesh(t, p, c1, c2, c3, strength)
                BgAnimation.SHAPES -> shapes(field, t, p, path, c1, c2, c3, strength)
                BgAnimation.GRID -> grid(t, p, c1, ink, strength)
                BgAnimation.LAVA -> lava(field, t, p, c1, c2, c3, strength)
                BgAnimation.AURORA -> aurora(t, p, path, c1, c2, c3, strength)
                else -> Unit
            }
        },
    )
}

/** Smoothed device tilt in roughly -1..1, from the gravity sensor; only listens while resumed. */
@Composable
private fun rememberTilt(enabled: Boolean): MutableState<Offset> {
    val state = remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(enabled, lifecycle) {
        if (!enabled) {
            state.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val tx = (-e.values[0] / 9.81f).coerceIn(-1f, 1f)
                val ty = ((e.values[1] - 6f) / 9.81f).coerceIn(-1f, 1f)
                val cur = state.value
                state.value = Offset(cur.x + (tx - cur.x) * 0.08f, cur.y + (ty - cur.y) * 0.08f)
            }

            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
                Lifecycle.Event.ON_PAUSE -> sm.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            sm.unregisterListener(listener)
        }
    }
    return state
}

private fun DrawScope.space(f: Field, t: Float, p: Offset, ink: Color, accent: Color, k: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until f.count) {
        val d = f.depth[i]
        val x = wrap(f.x[i] + t * 0.0025f * d) * w + p.x * d
        val y = f.y[i] * h + p.y * d
        val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * f.speed[i] * 1.6f + f.phase[i]))
        val r = (0.5f + f.size[i] * 1.6f) * d * density
        val c = if (f.kind[i] == 0) accent else ink
        drawCircle(c, r, Offset(x, y), alpha = twinkle * 0.85f * k)
    }
}

private fun DrawScope.snow(f: Field, t: Float, p: Offset, ink: Color, k: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until f.count) {
        val d = f.depth[i]
        val y = wrap(f.y[i] + t * 0.03f * f.speed[i] * d) * (h + 20f) - 10f + p.y * d
        val x = f.x[i] * w + sin(t * 0.6f * f.speed[i] + f.phase[i]) * 18.dp.toPx() * d + p.x * d
        drawCircle(ink, (1f + f.size[i] * 2.4f) * d * density, Offset(x, y), alpha = (0.35f + 0.5f * d) * k)
    }
}

private fun DrawScope.particles(f: Field, t: Float, p: Offset, c1: Color, c2: Color, k: Float) {
    val w = size.width
    val h = size.height
    val maxD = 130.dp.toPx()
    val xs = FloatArray(f.count)
    val ys = FloatArray(f.count)
    for (i in 0 until f.count) {
        xs[i] = wrap(f.x[i] + f.vx[i] * t * 0.012f) * w + p.x * f.depth[i]
        ys[i] = wrap(f.y[i] + f.vy[i] * t * 0.012f) * h + p.y * f.depth[i]
    }
    for (i in 0 until f.count) {
        for (j in i + 1 until f.count) {
            val dx = xs[i] - xs[j]
            val dy = ys[i] - ys[j]
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < maxD) drawLine(c1, Offset(xs[i], ys[i]), Offset(xs[j], ys[j]), 1.dp.toPx(), alpha = (1f - dist / maxD) * 0.35f * k)
        }
        drawCircle(if (f.kind[i] == 0) c2 else c1, (2f + f.size[i] * 2f) * density, Offset(xs[i], ys[i]), alpha = 0.8f * k)
    }
}

private fun DrawScope.circles(f: Field, t: Float, p: Offset, c1: Color, c2: Color, c3: Color, k: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until f.count) {
        val cx = (f.x[i] + 0.12f * sin(t * 0.07f * f.speed[i] + f.phase[i])) * w + p.x * f.depth[i] * 1.5f
        val cy = (f.y[i] + 0.08f * cos(t * 0.05f * f.speed[i] + f.phase[i])) * h + p.y * f.depth[i] * 1.5f
        val r = (70f + f.size[i] * 120f) * density
        val c = when (f.kind[i]) { 0 -> c1; 1 -> c2; else -> c3 }
        drawCircle(c, r, Offset(cx, cy), alpha = 0.09f * k)
        drawCircle(c, r, Offset(cx, cy), alpha = 0.22f * k, style = Stroke(1.dp.toPx()))
    }
}

private fun DrawScope.rings(t: Float, p: Offset, c1: Color, c2: Color, k: Float) {
    val centers = arrayOf(Offset(size.width * 0.15f, size.height * 0.2f), Offset(size.width * 0.9f, size.height * 0.75f))
    val maxR = size.maxDimension * 0.75f
    centers.forEachIndexed { ci, c0 ->
        val c = c0 + p * (1f + ci * 0.6f)
        for (n in 0 until 6) {
            val frac = wrap(t * 0.035f + n / 6f + ci * 0.5f)
            drawCircle(if (ci == 0) c1 else c2, frac * maxR, c, alpha = (1f - frac) * 0.35f * k, style = Stroke(1.5f.dp.toPx()))
        }
    }
}

private fun DrawScope.mesh(t: Float, p: Offset, c1: Color, c2: Color, c3: Color, k: Float) {
    val colors = arrayOf(c1, c2, c3, c1)
    val r = size.maxDimension * 0.55f
    for (i in 0 until 4) {
        val cx = size.width * (0.5f + 0.42f * sin(t * (0.05f + i * 0.013f) + i * 1.7f)) + p.x * (1 + i)
        val cy = size.height * (0.5f + 0.38f * cos(t * (0.04f + i * 0.011f) + i * 2.3f)) + p.y * (1 + i)
        val c = Offset(cx, cy)
        drawCircle(Brush.radialGradient(listOf(colors[i].copy(alpha = 0.32f * k), Color.Transparent), c, r), r, c)
    }
}

private fun DrawScope.shapes(f: Field, t: Float, p: Offset, path: Path, c1: Color, c2: Color, c3: Color, k: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until f.count) {
        val d = f.depth[i]
        val cx = f.x[i] * w + sin(t * 0.2f + f.phase[i]) * 12.dp.toPx() + p.x * d * 1.5f
        val cy = wrap(f.y[i] - t * 0.008f * f.speed[i]) * (h + 120f) - 60f + p.y * d * 1.5f
        val r = (14f + f.size[i] * 26f) * density
        val sides = 3 + (i % 4)
        path.reset()
        for (s in 0..sides) {
            val a = (s * 2 * PI / sides).toFloat()
            val px = cx + r * cos(a)
            val py = cy + r * sin(a)
            if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val c = when (f.kind[i]) { 0 -> c1; 1 -> c2; else -> c3 }
        rotate(t * 12f * f.vx[i] * 2f + f.phase[i] * 57f, Offset(cx, cy)) {
            drawPath(path, c, alpha = 0.45f * d * k, style = Stroke(1.5f.dp.toPx()))
        }
    }
}

private fun DrawScope.grid(t: Float, p: Offset, c1: Color, ink: Color, k: Float) {
    val step = 30.dp.toPx()
    val cx = size.width / 2 + p.x * 2
    val cy = size.height * 0.4f + p.y * 2
    var y = (p.y % step) - step
    while (y < size.height + step) {
        var x = (p.x % step) - step
        while (x < size.width + step) {
            val dx = x - cx
            val dy = y - cy
            val wave = 0.5f + 0.5f * sin(sqrt(dx * dx + dy * dy) / (60f * density) - t * 1.4f)
            drawCircle(if (wave > 0.85f) c1 else ink, (0.8f + wave * 1.6f) * density, Offset(x, y), alpha = (0.12f + wave * 0.45f) * k)
            x += step
        }
        y += step
    }
}

private fun DrawScope.lava(f: Field, t: Float, p: Offset, c1: Color, c2: Color, c3: Color, k: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until f.count) {
        val r = (90f + f.size[i] * 110f) * density * (0.85f + 0.15f * sin(t * 0.6f + f.phase[i]))
        val cy = (1f - wrap(f.y[i] + t * 0.012f * f.speed[i])) * (h + 2 * r) - r + p.y * (1 + i % 3)
        val cx = (f.x[i] * 0.7f + 0.15f + 0.1f * sin(t * 0.15f + f.phase[i])) * w + p.x * (1 + i % 3)
        val c = when (f.kind[i]) { 0 -> c1; 1 -> c2; else -> c3 }
        val o = Offset(cx, cy)
        drawCircle(Brush.radialGradient(0f to c.copy(alpha = 0.42f * k), 0.6f to c.copy(alpha = 0.18f * k), 1f to Color.Transparent, center = o, radius = r), r, o)
    }
}

private fun DrawScope.aurora(t: Float, p: Offset, path: Path, c1: Color, c2: Color, c3: Color, k: Float) {
    val colors = arrayOf(c1, c2, c3)
    val w = size.width
    val segments = 36
    for (b in 0 until 3) {
        val base = size.height * (0.22f + b * 0.16f) + p.y * (1 + b)
        val amp = (40f + b * 16f) * density
        path.reset()
        path.moveTo(0f, base)
        for (s in 0..segments) {
            val x = w * s / segments
            val y = base + amp * sin(x / w * 2.4f * PI.toFloat() + t * (0.25f + b * 0.07f) + b) +
                amp * 0.4f * sin(x / w * 5f + t * 0.4f)
            path.lineTo(x + p.x * (1 + b), y)
        }
        path.lineTo(w, base + amp * 4)
        path.lineTo(0f, base + amp * 4)
        path.close()
        drawPath(
            path,
            Brush.verticalGradient(listOf(colors[b].copy(alpha = 0.28f * k), Color.Transparent), startY = base - amp, endY = base + amp * 4),
        )
    }
}
