package com.twinndroid.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

@Immutable
data class TwinnExtras(
    val headerBrush: Brush,
    val headerStart: Color,
    val headerEnd: Color,
    val background: BgAnimation = BgAnimation.SPACE,
    val parallax: Boolean = true,
    val outlines: Boolean = true,
    val glassCards: Boolean = true,
    val header: HeaderSettings = HeaderSettings(),
)

val LocalTwinnExtras = staticCompositionLocalOf {
    TwinnExtras(Brush.linearGradient(listOf(Color(0xFF1E1450), Color(0xFF7C4DFF))), Color(0xFF1E1450), Color(0xFF7C4DFF))
}

private val TwinnType = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
)

private fun readableOn(c: Color) = if (c.luminance() > 0.45f) Color(0xFF111111) else Color.White

/** Expands the 7 user-facing colours into a full Material 3 colour scheme. */
fun Palette.toColorScheme(): ColorScheme {
    val p = Color(primary)
    val s = Color(secondary)
    val t = Color(tertiary)
    val bg = Color(background)
    val surf = Color(surface)
    val dark = bg.luminance() < 0.5f
    val onBg = if (dark) Color(0xFFE7E1F3) else Color(0xFF1C1B1F)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val primaryContainer = lerp(p, bg, 0.55f)
    val secondaryContainer = lerp(s, bg, 0.6f)
    val tertiaryContainer = lerp(t, bg, 0.6f)
    return base.copy(
        primary = p,
        onPrimary = readableOn(p),
        primaryContainer = primaryContainer,
        onPrimaryContainer = readableOn(primaryContainer),
        secondary = s,
        onSecondary = readableOn(s),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = readableOn(secondaryContainer),
        tertiary = t,
        onTertiary = readableOn(t),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = readableOn(tertiaryContainer),
        background = bg,
        onBackground = onBg,
        surface = bg,
        onSurface = onBg,
        surfaceVariant = lerp(surf, onBg, 0.08f),
        onSurfaceVariant = lerp(onBg, bg, 0.25f),
        surfaceContainerLowest = lerp(bg, Color.Black, if (dark) 0.3f else 0f),
        surfaceContainerLow = lerp(bg, surf, 0.5f),
        surfaceContainer = surf,
        surfaceContainerHigh = lerp(surf, onBg, 0.05f),
        surfaceContainerHighest = lerp(surf, onBg, 0.1f),
        surfaceTint = p,
        inverseSurface = onBg,
        inverseOnSurface = bg,
        outline = lerp(onBg, bg, 0.45f),
        outlineVariant = lerp(onBg, bg, 0.75f),
    )
}

@Composable
fun TwinnTheme(config: ThemeConfig = ThemeConfig(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val palette = config.palette(systemDark)

    var scheme: ColorScheme
    var extras: TwinnExtras
    if (config.dynamicColor && Build.VERSION.SDK_INT >= 31) {
        val dark = when (config.mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
        scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        val start = lerp(scheme.primary, Color.Black, 0.55f)
        extras = TwinnExtras(Brush.linearGradient(listOf(start, scheme.primary)), start, scheme.primary)
    } else {
        scheme = palette.toColorScheme()
        val start = Color(palette.headerStart)
        val end = Color(palette.headerEnd)
        extras = TwinnExtras(Brush.linearGradient(listOf(start, end)), start, end)
    }

    if (config.pureBlack && scheme.background.luminance() < 0.5f) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = lerp(Color.Black, scheme.surfaceContainer, 0.5f),
            inverseOnSurface = Color.Black,
        )
    }
    extras = extras.copy(
        background = config.background,
        parallax = config.parallax,
        outlines = config.outlines,
        glassCards = config.glassCards,
        header = config.header,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                val lightBg = scheme.background.luminance() > 0.5f
                isAppearanceLightStatusBars = lightBg
                isAppearanceLightNavigationBars = lightBg
            }
        }
    }

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalTwinnExtras provides extras,
        LocalDensity provides Density(density.density, density.fontScale * config.fontScale),
    ) {
        MaterialTheme(colorScheme = scheme, typography = TwinnType, content = content)
    }
}
