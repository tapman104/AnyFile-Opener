package com.openbridge

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.openbridge.databinding.BottomSheetOpenAsBinding

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
            return
        }

        val fileName = resolveFileName(uri) ?: uri.lastPathSegment
        val detectedResult = MimeDetector.detect(
            requireContext().contentResolver, 
            uri, 
            fileName, 
            prefs.defaultMime
        )
        val detectedType  = detectedResult.fileType

        // Implementation of Auto-Open preference:
        // If detection is not UNKNOWN and autoOpen is enabled, we could skip the sheet.
        // However, usually users want to see the choice once. 
        // Let's implement it such that if they pick "Open Normally" it uses this logic.

        binding.titleText.text = when {
            fileName != null && fileName.length <= 44 -> fileName
            fileName != null -> "${fileName.take(41)}…"
            else -> "Open as…"
        }
        binding.subtitleText.text = "Detected: ${detectedType.emoji} ${detectedType.label}"

        val types = MimeDetector.FileType.entries.filter { it != MimeDetector.FileType.UNKNOWN }
        val preSelected = if (detectedType == MimeDetector.FileType.UNKNOWN) -1 else types.indexOf(detectedType)

        binding.typeList.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.typeList.adapter = FileTypeAdapter(types, preSelected) { selectedType ->
            val mime = if (selectedType == detectedType) detectedResult.mime else MimeDetector.mimeOf(selectedType, prefs.defaultMime)
            IntentRouter.open(requireContext(), uri, mime)
            dismiss()
            if (activity is MainActivity) {
                activity?.finish()
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (activity is MainActivity) {
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun resolveFileName(uri: Uri): String? =
        try {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
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
