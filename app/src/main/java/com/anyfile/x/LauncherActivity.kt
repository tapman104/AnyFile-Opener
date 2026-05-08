package com.anyfile.x

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Modernized LauncherActivity using Jetpack Compose.
 * This activity serves as the primary file picker and manual MIME override dashboard.
 */
class LauncherActivity : AppCompatActivity() {

    private val viewModel: OpenBridgeViewModel by viewModels()
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)

        setContent {
            val themePref by prefs.themePreference.collectAsStateWithLifecycle(ThemePreference.SYSTEM_DEFAULT)
            
            AnyFileOpenerTheme(themePreference = themePref) {
                LauncherScreen(
                    viewModel = viewModel,
                    prefsManager = prefs,
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onOpenAsClick = { uri ->
                        OpenAsBottomSheet.newInstance(uri)
                            .show(supportFragmentManager, "open_as")
                    },
                    onInspectClick = { uri ->
                        InspectBottomSheet.newInstance(uri)
                            .show(supportFragmentManager, "inspect")
                    },
                    onSystemFilesClick = {
                        openSystemFileManager()
                    }
                )
            }
        }
    }

    /**
     * Attempts to open the Android System File Manager (DocumentsUI) directly.
     * This provides a better experience than a simple ACTION_GET_CONTENT.
     */
    private fun openSystemFileManager() {
        val intents = mutableListOf<Intent>()
        val packages = arrayOf("com.google.android.documentsui", "com.android.documentsui")
        
        for (pkg in packages) {
            intents.add(Intent().apply {
                component = ComponentName(pkg, "com.android.documentsui.files.FilesActivity")
            })
            intents.add(Intent().apply {
                component = ComponentName(pkg, "com.android.documentsui.LauncherActivity")
            })
        }

        // Broad fallback for external storage browse
        intents.add(Intent("android.provider.action.BROWSE_DOCUMENT_ROOT").apply {
            data = Uri.parse("content://com.android.externalstorage.documents/root/primary")
        })

        var started = false
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                started = true
                break
            } catch (e: Exception) {}
        }

        if (!started) {
            Toast.makeText(this, "System file manager not found", Toast.LENGTH_SHORT).show()
        }
    }
}
