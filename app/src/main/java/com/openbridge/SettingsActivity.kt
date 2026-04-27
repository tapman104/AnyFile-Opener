package com.openbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = PrefsManager(this)

        setContent {
            val themePref by prefs.themePreference.collectAsStateWithLifecycle(ThemePreference.SYSTEM_DEFAULT)
            
            AnyFileOpenerTheme(themePreference = themePref) {
                SettingsScreen(
                    prefsManager = prefs,
                    onBackClick = { finish() }
                )
            }
        }
    }
}
