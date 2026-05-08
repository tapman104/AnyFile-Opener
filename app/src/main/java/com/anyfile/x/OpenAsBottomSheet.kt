package com.anyfile.x

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.anyfile.x.databinding.BottomSheetOpenAsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bottom sheet that shows the "Open as" type chooser.
 * Auto-detects the file type from magic bytes and pre-selects the matching category.
 */
class OpenAsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOpenAsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PrefsManager

    companion object {
        private const val ARG_URI = "arg_uri"

        fun newInstance(uri: Uri): OpenAsBottomSheet =
            OpenAsBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_URI, uri.toString()) }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetOpenAsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())

        val uriString = arguments?.getString(ARG_URI)
        val uri = uriString?.let { Uri.parse(it) }

        if (uri == null) {
            dismiss()
            if (activity is MainActivity) {
                activity?.finish()
            }
            return
        }

        val resolver = requireContext().contentResolver
        val context = requireContext().applicationContext
        
        lifecycleScope.launch {
            val fileName = withContext(Dispatchers.IO) {
                resolveFileName(resolver, uri) ?: uri.lastPathSegment
            }
            
            val detectedResult = withContext(Dispatchers.IO) {
                MimeDetector.detect(
                    resolver,
                    uri,
                    fileName,
                    prefs.defaultMime
                )
            }
            
            val detectedType = detectedResult.fileType

            binding.titleText.text = when {
                fileName != null && fileName.length <= 44 -> fileName
                fileName != null -> "${fileName.take(41)}…"
                else -> "Open as…"
            }
            binding.subtitleText.text = "Detected: ${detectedType.emoji} ${detectedType.label}"

            setupLastAppChip(uri, detectedResult.mime)

            val types = MimeDetector.FileType.entries.filter { it != MimeDetector.FileType.UNKNOWN }
            val preSelected = if (detectedType == MimeDetector.FileType.UNKNOWN) -1 else types.indexOf(detectedType)

            binding.typeList.layoutManager = GridLayoutManager(context, 3)
            binding.typeList.adapter = FileTypeAdapter(types, preSelected) { selectedType ->
                val mime = if (selectedType == detectedType) detectedResult.mime else MimeDetector.mimeOf(selectedType, prefs.defaultMime)
                IntentRouter.open(context, uri, mime)
                dismiss()
                if (activity is MainActivity) {
                    activity?.finish()
                }
            }
        }
    }

    private fun setupLastAppChip(uri: Uri, mime: String) {
        val appContext = requireContext().applicationContext
        val lastPackage = IntentRouter.getLastApp(appContext, mime) ?: return
        try {
            val pm = appContext.packageManager
            val appInfo = pm.getApplicationInfo(lastPackage, 0)
            val appLabel = pm.getApplicationLabel(appInfo)
            val appIcon = pm.getApplicationIcon(appInfo)

            binding.lastAppChip.apply {
                visibility = View.VISIBLE
                text = "Open again with $appLabel"
                chipIcon = appIcon
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        setPackage(lastPackage)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        startActivity(intent)
                        dismiss()
                        if (activity is MainActivity) activity?.finish()
                    } catch (e: Exception) {
                        // Package might have been uninstalled
                        IntentRouter.open(appContext, uri, mime)
                        dismiss()
                    }
                }
            }
        } catch (e: Exception) {
            binding.lastAppChip.visibility = View.GONE
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (activity is MainActivity) {
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun resolveFileName(resolver: android.content.ContentResolver, uri: Uri): String? =
        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) cursor.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            Log.e("OpenAsBottomSheet", "Could not resolve display name for $uri", e)
            null
        }
}
