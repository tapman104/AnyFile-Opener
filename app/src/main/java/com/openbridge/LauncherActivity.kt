package com.openbridge

import android.content.ComponentName
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.openbridge.databinding.ActivityLauncherBinding

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var prefs: PrefsManager
    private var selectedUri: Uri? = null
    private var detectedMime: String? = null
    private var advancedExpanded = false

    private val pickFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not persistable
            }
            selectedUri = uri
            updatePickerUI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnOverflow.setOnClickListener { view ->
            showOverflowMenu(view)
        }

        binding.filePickerArea.setOnClickListener {
            pickFile.launch(arrayOf("*/*"))
        }

        binding.btnSystemFiles.setOnClickListener {
            openSystemFileManager()
        }

        setupChips()

        binding.btnOpenNormal.setOnClickListener {
            val uri = selectedUri
            if (uri == null) {
                Toast.makeText(this, "Select a file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mime = detectedMime ?: prefs.defaultMime
            IntentRouter.open(this, uri, mime)
        }

        binding.btnOpenAs.setOnClickListener {
            val uri = selectedUri
            if (uri == null) {
                Toast.makeText(this, "Select a file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            OpenAsBottomSheet.newInstance(uri)
                .show(supportFragmentManager, "open_as")
        }

        binding.btnAdvancedToggle.setOnClickListener {
            toggleAdvanced(!advancedExpanded)
        }

        binding.btnOpenForce.setOnClickListener {
            val uri = selectedUri
            if (uri == null) {
                Toast.makeText(this, "Select a file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mime = binding.mimeInput.text?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() } ?: prefs.defaultMime
            IntentRouter.open(this, uri, mime)
        }

        binding.btnInspect.setOnClickListener {
            val uri = selectedUri
            if (uri == null) {
                Toast.makeText(this, "Select a file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            InspectBottomSheet.newInstance(uri)
                .show(supportFragmentManager, "inspect")
        }

        // Restore state
        savedInstanceState?.let { bundle ->
            bundle.getString("saved_uri")?.let {
                selectedUri = Uri.parse(it)
                updatePickerUI(selectedUri!!)
            }
            toggleAdvanced(bundle.getBoolean("advanced_expanded", false))
        }
    }

    private fun showOverflowMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("How to Use")
        popup.menu.add("About")
        popup.menu.add("Buy Me a Coffee")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "How to Use" -> {
                    showHelpDialog()
                    true
                }
                "About" -> {
                    showAboutDialog()
                    true
                }
                "Buy Me a Coffee" -> {
                    openUrl("https://buymeacoffee.com/tapman")
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Help & Guide")
            .setMessage("AnyFile Opener is your swiss-army knife for files.\n\n" +
                    "How to use:\n" +
                    "1. Pick a file: Tap the center area or use 'System File Manager' for restricted folders.\n" +
                    "2. Auto-Detect: The app scans the binary header to find the real type.\n" +
                    "3. Open: Use 'Open Normally' or 'Open as...' to pick a category.\n\n" +
                    "Pro Tips:\n" +
                    "• Advanced: Type custom MIMEs like 'text/xml' if the auto-detect isn't specific enough.\n" +
                    "• Inspect: Use 'INSPECT BINARY' to see if a file is corrupted or to read hidden text headers.")
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About AnyFile Opener")
            .setMessage("AnyFile Opener is a versatile utility designed to handle files that Android usually struggles with. It automatically identifies the correct file type using magic-byte detection and lets you override it manually to open files with any app you choose.\n\nCreated with ❤️ by Tapman")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedUri?.let { outState.putString("saved_uri", it.toString()) }
        outState.putBoolean("advanced_expanded", advancedExpanded)
    }

    private fun toggleAdvanced(expand: Boolean) {
        advancedExpanded = expand
        binding.advancedSection.visibility = if (expand) View.VISIBLE else View.GONE
        binding.btnAdvancedToggle.text = if (expand) "Hide advanced" else "Advanced options"
    }

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
            try {
                pickFile.launch(arrayOf("*/*"))
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open file manager", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePickerUI(uri: Uri) {
        val fileName = queryFileName(uri) ?: uri.lastPathSegment ?: "Unknown file"
        binding.fileNameText.text = fileName
        binding.fileIcon.setImageResource(android.R.drawable.ic_menu_agenda)

        try {
            val result = MimeDetector.detect(contentResolver, uri, fileName, prefs.defaultMime)
            detectedMime = result.mime
            binding.mimeInput.setText(result.mime)
            binding.detectedMimeText.text = "Detected: ${result.mime}"
            binding.detectedMimeText.visibility = View.VISIBLE
            
            // Auto-open logic if enabled
            if (prefs.autoOpen && result.fileType != MimeDetector.FileType.UNKNOWN) {
                IntentRouter.open(this, uri, result.mime)
            }
        } catch (e: Exception) {
            detectedMime = null
            binding.detectedMimeText.visibility = View.GONE
        }
        binding.actionButtonsGroup.visibility = View.VISIBLE
    }

    private fun setupChips() {
        val chipMimes = mapOf(
            binding.chipApk to "application/vnd.android.package-archive",
            binding.chipTxt to "text/plain",
            binding.chipJson to "application/json",
            binding.chipPdf to "application/pdf",
            binding.chipJpg to "image/jpeg",
            binding.chipMp4 to "video/mp4",
            binding.chipMp3 to "audio/mpeg",
            binding.chipZip to "application/zip"
        )
        for ((chip, mime) in chipMimes) {
            chip.setOnClickListener {
                binding.mimeInput.setText(mime)
            }
        }
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        try {
            val cursor: Cursor? = contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (col >= 0) name = it.getString(col)
                }
            }
        } catch (_: Exception) {}
        return name
    }
}
