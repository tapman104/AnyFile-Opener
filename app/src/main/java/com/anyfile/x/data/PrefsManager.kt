package com.anyfile.x

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PrefsManager(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_OPEN = "auto_open"
        private const val KEY_DEFAULT_MIME = "default_mime"
        private val KEY_THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }

    var autoOpen: Boolean
        get() = sharedPrefs.getBoolean(KEY_AUTO_OPEN, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_AUTO_OPEN, value).apply()

    var defaultMime: String
        get() = sharedPrefs.getString(KEY_DEFAULT_MIME, "application/octet-stream") ?: "application/octet-stream"
        set(value) = sharedPrefs.edit().putString(KEY_DEFAULT_MIME, value).apply()

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_THEME_PREFERENCE] ?: ThemePreference.SYSTEM_DEFAULT.name
        try {
            ThemePreference.valueOf(name)
        } catch (e: Exception) {
            ThemePreference.SYSTEM_DEFAULT
        }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_PREFERENCE] = theme.name
        }
    }
}
