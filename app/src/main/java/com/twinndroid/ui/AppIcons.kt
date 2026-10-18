package com.twinndroid.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.twinndroid.MainActivity
import com.twinndroid.R
import kotlin.math.min

/** A launcher icon for TwinnDroid itself, backed by an <activity-alias> in the manifest. */
class AppIcon(
    val alias: String,
    val name: String,
    val foreground: Int,
    val bgStart: Color,
    val bgEnd: Color,
)

object AppIcons {
    private const val PREFIX = "com.twinndroid.Launcher"
    private const val PREF = "launcher_icon"

    val all = listOf(
        AppIcon("Twinn", "TwinnDroid", R.drawable.ic_launcher_foreground, Color(0xFF1E1450), Color(0xFF7C4DFF)),
        AppIcon("Midnight", "Midnight", R.drawable.ic_launcher_midnight_foreground, Color(0xFF000000), Color(0xFF0D1B3D)),
        AppIcon("Mint", "Mint", R.drawable.ic_launcher_mint_foreground, Color(0xFF2EE6B6), Color(0xFF00BFA5)),
        AppIcon("Ocean", "Ocean", R.drawable.ic_launcher_ocean_foreground, Color(0xFF01579B), Color(0xFF00ACC1)),
        AppIcon("Sunset", "Sunset", R.drawable.ic_launcher_sunset_foreground, Color(0xFFB71C1C), Color(0xFFFF6F00)),
        AppIcon("Rose", "Rose", R.drawable.ic_launcher_rose_foreground, Color(0xFF880E4F), Color(0xFFEC407A)),
        AppIcon("Mono", "Mono", R.drawable.ic_launcher_mono_foreground, Color(0xFFFFFFFF), Color(0xFFE0E0E0)),
        AppIcon("Lavender", "Lavender", R.drawable.ic_launcher_lavender_foreground, Color(0xFF0B0B12), Color(0xFF2A2540)),
        AppIcon("Cyberpunk", "Cyberpunk", R.drawable.ic_launcher_cyberpunk_foreground, Color(0xFF0D0221), Color(0xFF3A0CA3)),
        AppIcon("Nord", "Nord", R.drawable.ic_launcher_nord_foreground, Color(0xFF2E3440), Color(0xFF3B4252)),
        AppIcon("Dracula", "Dracula", R.drawable.ic_launcher_dracula_foreground, Color(0xFF282A36), Color(0xFF44475A)),
        AppIcon("Matcha", "Matcha", R.drawable.ic_launcher_matcha_foreground, Color(0xFF7A9A55), Color(0xFFA8C686)),
    )

    fun byAlias(alias: String): AppIcon? = all.firstOrNull { it.alias == alias }

    private fun component(context: Context, icon: AppIcon) = ComponentName(context.packageName, PREFIX + icon.alias)

    private fun prefs(context: Context) = context.getSharedPreferences("theme", Context.MODE_PRIVATE)

    private fun enabled(context: Context): AppIcon {
        val pm = context.packageManager
        return all.firstOrNull { icon ->
            when (pm.getComponentEnabledSetting(component(context, icon))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon === all.first() // enabled in the manifest
                else -> false
            }
        } ?: all.first()
    }

    /** The chosen icon (it may not be applied yet; see [applyPending]). */
    fun current(context: Context): AppIcon =
        prefs(context).getString(PREF, null)?.let { a -> all.firstOrNull { it.alias == a } } ?: enabled(context)

    /**
     * Saves the choice. Switching launcher entries closes any task started from the old one,
     * so the actual switch happens in [applyPending] once TwinnDroid is in the background.
     */
    fun choose(context: Context, icon: AppIcon) {
        prefs(context).edit().putString(PREF, icon.alias).apply()
    }

    /** Called from MainActivity.onStop. Enables the chosen entry first so there is never zero launchers. */
    fun applyPending(context: Context) {
        val icon = current(context)
        if (icon === enabled(context)) return
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component(context, icon), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        for (other in all) {
            if (other === icon) continue
            pm.setComponentEnabledSetting(component(context, other), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }

    /**
     * Android doesn't let an app swap its own launcher icon for an arbitrary image, so a custom
     * picture becomes a pinned home-screen shortcut that opens TwinnDroid.
     */
    fun pinCustomShortcut(context: Context, image: Uri, label: String): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val bitmap = loadAdaptive(context, image) ?: return false
        val info = ShortcutInfoCompat.Builder(context, "twinn-custom-${System.currentTimeMillis()}")
            .setShortLabel(label.ifBlank { "TwinnDroid" })
            .setIcon(IconCompat.createWithAdaptiveBitmap(bitmap))
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    /** Centre-crops to a 432px square; launchers mask the outer edge of adaptive bitmaps. */
    fun loadAdaptive(context: Context, image: Uri): Bitmap? {
        val src = context.contentResolver.openInputStream(image)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val size = 432
        val side = min(src.width, src.height)
        val crop = Rect((src.width - side) / 2, (src.height - side) / 2, (src.width + side) / 2, (src.height + side) / 2)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, crop, Rect(0, 0, size, size), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }
}
