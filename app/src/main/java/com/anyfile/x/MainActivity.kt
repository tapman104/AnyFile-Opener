package com.anyfile.x

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anyfile.x.AnyFileOpenerTheme

/**
 * Transparent entry-point activity.
 * Receives ACTION_VIEW, ACTION_SEND, and ACTION_SEND_MULTIPLE intents.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: OpenBridgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = PrefsManager(this)

        setContent {
            val themePref by prefs.themePreference.collectAsStateWithLifecycle(ThemePreference.SYSTEM_DEFAULT)
            
            AnyFileOpenerTheme(themePreference = themePref) {
                // We keep it transparent but need the Theme for the BottomSheets
                Box(modifier = Modifier.fillMaxSize())
            }
        }

        if (savedInstanceState != null) return

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    showOpenAs(uri)
                } else {
                    startLauncher()
                }
            }
            Intent.ACTION_SEND -> {
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    showOpenAs(uri)
                } else {
                    startLauncher()
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) {
                    showOpenAs(uris[0])
                } else {
                    startLauncher()
                }
            }
            else -> startLauncher()
        }
    }

    private fun showOpenAs(uri: Uri) {
        OpenAsBottomSheet.newInstance(uri)
            .show(supportFragmentManager, "openAs")
    }

    private fun startLauncher() {
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }
}
