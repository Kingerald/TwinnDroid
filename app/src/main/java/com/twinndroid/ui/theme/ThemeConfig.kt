package com.twinndroid.ui.theme

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The user-editable colour "aspects" of the app. Values are ARGB. */
@Serializable
data class Palette(
    val primary: Long,
    val secondary: Long,
    val tertiary: Long,
    val background: Long,
    val surface: Long,
    val headerStart: Long,
    val headerEnd: Long,
)

enum class Aspect(val label: String, val description: String) {
    PRIMARY("Accent", "Buttons, switches and highlights"),
    SECONDARY("Secondary", "Installed status and success states"),
    TERTIARY("Tertiary", "Update badges"),
    BACKGROUND("Background", "Behind everything"),
    SURFACE("Cards", "Clone cards and settings panels"),
    HEADER_START("Header start", "Left side of the header gradient"),
    HEADER_END("Header end", "Right side of the header gradient");

    fun get(p: Palette): Long = when (this) {
        PRIMARY -> p.primary
        SECONDARY -> p.secondary
        TERTIARY -> p.tertiary
        BACKGROUND -> p.background
        SURFACE -> p.surface
        HEADER_START -> p.headerStart
        HEADER_END -> p.headerEnd
    }

    fun set(p: Palette, v: Long): Palette = when (this) {
        PRIMARY -> p.copy(primary = v)
        SECONDARY -> p.copy(secondary = v)
        TERTIARY -> p.copy(tertiary = v)
        BACKGROUND -> p.copy(background = v)
        SURFACE -> p.copy(surface = v)
        HEADER_START -> p.copy(headerStart = v)
        HEADER_END -> p.copy(headerEnd = v)
    }
}

class ThemePreset(val id: String, val name: String, val dark: Palette, val light: Palette)

object ThemePresets {
    val all = listOf(
        ThemePreset(
            "twinn", "TwinnDroid",
            dark = Palette(0xFFB9A3FF, 0xFF2EE6B6, 0xFFFFB0CB, 0xFF0F0D1A, 0xFF1C1830, 0xFF1E1450, 0xFF7C4DFF),
            light = Palette(0xFF5B30E0, 0xFF006B54, 0xFFB4245E, 0xFFFCF8FF, 0xFFF0EBF8, 0xFF1E1450, 0xFF7C4DFF),
        ),
        ThemePreset(
            "midnight", "Midnight AMOLED",
            dark = Palette(0xFF82B1FF, 0xFF64FFDA, 0xFFFF8A80, 0xFF000000, 0xFF121212, 0xFF0D1B3D, 0xFF2962FF),
            light = Palette(0xFF2962FF, 0xFF00897B, 0xFFD32F2F, 0xFFF7F9FF, 0xFFE8EEFB, 0xFF0D1B3D, 0xFF2962FF),
        ),
        ThemePreset(
            "ocean", "Ocean",
            dark = Palette(0xFF4FC3F7, 0xFF80CBC4, 0xFFFFCC80, 0xFF06131C, 0xFF10242F, 0xFF01579B, 0xFF00ACC1),
            light = Palette(0xFF0277BD, 0xFF00796B, 0xFFEF6C00, 0xFFF3FAFE, 0xFFE1F1F8, 0xFF01579B, 0xFF00ACC1),
        ),
        ThemePreset(
            "forest", "Forest",
            dark = Palette(0xFF81C784, 0xFFDCE775, 0xFFFFB74D, 0xFF0B140C, 0xFF172419, 0xFF1B5E20, 0xFF689F38),
            light = Palette(0xFF2E7D32, 0xFF827717, 0xFFE65100, 0xFFF5FAF3, 0xFFE4EFE1, 0xFF1B5E20, 0xFF689F38),
        ),
        ThemePreset(
            "sunset", "Sunset",
            dark = Palette(0xFFFF9E80, 0xFFFFD180, 0xFFF48FB1, 0xFF1A0E0E, 0xFF2A1818, 0xFFB71C1C, 0xFFFF6F00),
            light = Palette(0xFFD84315, 0xFFB26A00, 0xFFC2185B, 0xFFFFF8F4, 0xFFFBE9E0, 0xFFB71C1C, 0xFFFF6F00),
        ),
        ThemePreset(
            "rose", "Rose",
            dark = Palette(0xFFF8BBD0, 0xFFCE93D8, 0xFFFFE082, 0xFF180C12, 0xFF28161F, 0xFF880E4F, 0xFFEC407A),
            light = Palette(0xFFC2185B, 0xFF7B1FA2, 0xFFB28704, 0xFFFFF7FA, 0xFFFBE4EC, 0xFF880E4F, 0xFFEC407A),
        ),
        ThemePreset(
            "mono", "High contrast",
            dark = Palette(0xFFFFFFFF, 0xFFBDBDBD, 0xFFFFEB3B, 0xFF000000, 0xFF1A1A1A, 0xFF000000, 0xFF424242),
            light = Palette(0xFF000000, 0xFF424242, 0xFF8D6E00, 0xFFFFFFFF, 0xFFEEEEEE, 0xFF212121, 0xFF616161),
        ),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()
}

@Serializable
data class ThemeConfig(
    val presetId: String = "twinn",
    val mode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You: colours from the wallpaper (Android 12+). Overrides preset and custom. */
    val dynamicColor: Boolean = false,
    /** When set, replaces the preset entirely (the palette's own background decides light/dark). */
    val custom: Palette? = null,
    val fontScale: Float = 1f,
) {
    fun palette(systemDark: Boolean): Palette {
        custom?.let { return it }
        val preset = ThemePresets.byId(presetId)
        val dark = when (mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
        return if (dark) preset.dark else preset.light
    }
}

class ThemeRepository private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _config = MutableStateFlow(load())
    val config: StateFlow<ThemeConfig> = _config.asStateFlow()

    private fun load(): ThemeConfig = runCatching {
        prefs.getString("config", null)?.let { json.decodeFromString<ThemeConfig>(it) }
    }.getOrNull() ?: ThemeConfig()

    fun update(transform: (ThemeConfig) -> ThemeConfig) {
        val next = transform(_config.value)
        prefs.edit { putString("config", json.encodeToString(next)) }
        _config.value = next
    }

    companion object {
        @Volatile private var instance: ThemeRepository? = null
        fun get(context: Context): ThemeRepository =
            instance ?: synchronized(this) {
                instance ?: ThemeRepository(context.applicationContext).also { instance = it }
            }
    }
}
