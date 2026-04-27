package com.openbridge

import android.content.ComponentName
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.openbridge.databinding.ActivityLauncherBinding
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var prefs: PrefsManager
    private val viewModel: OpenBridgeViewModel by viewModels()
    
    private var selectedUri: Uri? = null
    private var detectedMime: String? = null
    private var advancedExpanded = false
    
    private val ALL_MIME = "*${'/'}*"
    private lateinit var recentAdapter: RecentFileAdapter

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

        setupRecentFiles()
        observeViewModel()

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.filePickerArea.setOnClickListener {
            pickFile.launch(arrayOf(ALL_MIME))
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
            // Use current text from input or default
            val mime = binding.mimeInput.text?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() } ?: detectedMime ?: "application/octet-stream"
            addToRecents(uri, mime)
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
                ?.takeIf { it.isNotEmpty() } ?: "application/octet-stream"
            addToRecents(uri, mime)
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

    private fun toggleAdvanced(expand: Boolean) {
        advancedExpanded = expand
        binding.advancedSection.visibility = if (expand) View.VISIBLE else View.GONE
        binding.btnAdvancedToggle.text = if (expand) "Hide Advanced" else "Show Advanced Options"
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detectionResult.collect { result ->
                    if (result != null) {
                        detectedMime = result.mime
                        binding.mimeInput.setText(result.mime)
                        binding.detectedMimeText.text = "Detected: ${result.mime}"
                        binding.detectedMimeText.visibility = View.VISIBLE
                        
                        // Auto-open logic can't easily access flow-based autoOpen here without a collector
                    } else {
                        detectedMime = null
                        binding.detectedMimeText.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupRecentFiles() {
        recentAdapter = RecentFileAdapter(emptyList()) { recent ->
            val uri = Uri.parse(recent.uri)
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            
            selectedUri = uri
            updatePickerUI(uri)
        }
        binding.recentFilesList.apply {
            layoutManager = LinearLayoutManager(this@LauncherActivity)
            adapter = recentAdapter
        }

        lifecycleScope.launch {
            RecentFileStore.getRecentFiles(this@LauncherActivity).collect { files ->
                if (files.isEmpty()) {
                    binding.recentFilesSection.visibility = View.GONE
                } else {
                    binding.recentFilesSection.visibility = View.VISIBLE
                    recentAdapter.updateItems(files)
                }
            }
        }
    }

    private fun addToRecents(uri: Uri, mime: String) {
        val name = queryFileName(uri) ?: uri.lastPathSegment ?: "Unknown file"
        lifecycleScope.launch {
            RecentFileStore.addRecentFile(this@LauncherActivity, RecentFile(uri.toString(), name, mime))
        }
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
                pickFile.launch(arrayOf(ALL_MIME))
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open file manager", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePickerUI(uri: Uri) {
        val fileName = queryFileName(uri) ?: uri.lastPathSegment ?: "Unknown file"
        binding.fileNameText.text = fileName
        binding.fileIcon.setImageResource(android.R.drawable.ic_menu_agenda)
        binding.actionButtonsGroup.visibility = View.VISIBLE

        viewModel.detectMime(uri, fileName)
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
