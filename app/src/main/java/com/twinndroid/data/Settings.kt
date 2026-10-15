package com.twinndroid.data

import android.content.Context
import androidx.core.content.edit

class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Install through Shizuku (no confirmation dialog) whenever it is running and permitted. */
    var useShizuku: Boolean
        get() = prefs.getBoolean("use_shizuku", true)
        set(v) = prefs.edit { putBoolean("use_shizuku", v) }

    /** Rebuild + reinstall clones in the background when their original app updates (needs Shizuku). */
    var autoUpdate: Boolean
        get() = prefs.getBoolean("auto_update", false)
        set(v) = prefs.edit { putBoolean("auto_update", v) }

    var askedNotifications: Boolean
        get() = prefs.getBoolean("asked_notifications", false)
        set(v) = prefs.edit { putBoolean("asked_notifications", v) }
}
