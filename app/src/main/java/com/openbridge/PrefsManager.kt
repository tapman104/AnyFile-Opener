package com.openbridge

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_OPEN = "auto_open"
        private const val KEY_DEFAULT_MIME = "default_mime"
    }

    var autoOpen: Boolean
        get() = prefs.getBoolean(KEY_AUTO_OPEN, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_OPEN, value).apply()

    var defaultMime: String
        get() = prefs.getString(KEY_DEFAULT_MIME, "application/octet-stream") ?: "application/octet-stream"
        set(value) = prefs.edit().putString(KEY_DEFAULT_MIME, value).apply()
}
