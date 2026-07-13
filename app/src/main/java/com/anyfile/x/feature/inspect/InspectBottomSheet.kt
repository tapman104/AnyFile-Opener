package com.anyfile.x

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.anyfile.x.databinding.BottomSheetInspectBinding
import com.anyfile.x.utils.ByteReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InspectBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetInspectBinding? = null
    private val binding get() = _binding!!
    
    private var currentUri: Uri? = null
    private var totalBytesRead = 0
    private var fullByteArray = ByteArray(0)

    companion object {
        private const val ARG_URI = "uri"

        fun newInstance(uri: Uri): InspectBottomSheet {
            return InspectBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_URI, uri.toString())
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetInspectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uriString = arguments?.getString(ARG_URI) ?: return
        val uri = Uri.parse(uriString)
        currentUri = uri

        val resolver = requireContext().applicationContext.contentResolver

        lifecycleScope.launch {
            // File name
            val fileName = withContext(Dispatchers.IO) { queryFileName(resolver, uri) }
            binding.inspectFileName.text = fileName ?: uri.lastPathSegment ?: "Unknown"

            // File size
            val size = withContext(Dispatchers.IO) { queryFileSize(resolver, uri) }
            binding.inspectSizeText.text = if (size >= 0) formatSize(size) else "Unknown"

            // Initial load
            withContext(Dispatchers.IO) { loadBytesInternal(resolver, uri, 256) }
            updateDumpUI()

            // Show MIME Detector's header-based detection result
            try {
                val result = MimeDetector.detectFromHeader(fullByteArray, fileName ?: "", contentResolver = resolver, uri = uri)
                binding.headerDetectionLabel.visibility = View.VISIBLE
                binding.headerDetectionLabel.text = "Header Scan: ${result.mime}"
                binding.inspectMimeText.text = result.mime
            } catch (e: Exception) {
                binding.inspectMimeText.text = "detection failed"
            }
        }

        binding.btnLoadMore.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { loadBytesInternal(resolver, uri, 512) }
                updateDumpUI()
                // Scroll to bottom after loading more
                binding.hexScrollView.post { binding.hexScrollView.fullScroll(View.FOCUS_DOWN) }
                binding.asciiScrollView.post { binding.asciiScrollView.fullScroll(View.FOCUS_DOWN) }
            }
        }

        binding.btnOpenFolder.setOnClickListener {
            IntentRouter.openFolder(requireContext(), uri)
        }
    }

    private fun loadBytesInternal(resolver: android.content.ContentResolver, uri: Uri, count: Int) {
        val newBytes = ByteReader.readBytes(resolver, uri, totalBytesRead.toLong(), count)
        if (newBytes.isEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                binding.btnLoadMore.isEnabled = false
                binding.btnLoadMore.text = "End of file"
            }
            return
        }

        val updatedArray = ByteArray(fullByteArray.size + newBytes.size)
        System.arraycopy(fullByteArray, 0, updatedArray, 0, fullByteArray.size)
        System.arraycopy(newBytes, 0, updatedArray, fullByteArray.size, newBytes.size)
        fullByteArray = updatedArray
        totalBytesRead += newBytes.size
    }

    private fun updateDumpUI() {
        binding.hexByteCount.text = "OFFSET | HEX DUMP ($totalBytesRead bytes loaded)"
        binding.hexDumpText.text = buildHexDump(fullByteArray)
        binding.asciiDumpText.text = buildAsciiPreview(fullByteArray)
    }

    private fun buildHexDump(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.indices step 16) {
            // Offset ruler column
            sb.append("%08X | ".format(i))
            
            for (j in 0 until 16) {
                if (i + j < bytes.size) {
                    sb.append("%02X ".format(bytes[i + j].toInt() and 0xFF))
                } else {
                    sb.append("   ")
                }
                if (j == 7) sb.append(" ") // Small gap mid-way
            }
            if (i + 16 < bytes.size) sb.append("\n")
        }
        return sb.toString()
    }

    private fun buildAsciiPreview(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.indices step 16) {
            sb.append("%08X | ".format(i))
            val end = minOf(i + 16, bytes.size)
            for (j in i until end) {
                val b = bytes[j].toInt() and 0xFF
                if (b in 32..126) sb.append(b.toChar()) else sb.append(".")
                if (j - i == 7) sb.append(" ")
            }
            if (i + 16 < bytes.size) sb.append("\n")
        }
        return sb.toString()
    }

    private fun queryFileName(resolver: android.content.ContentResolver, uri: Uri): String? {
        var name: String? = null
        try {
            val cursor: Cursor? = resolver.query(
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

    private fun queryFileSize(resolver: android.content.ContentResolver, uri: Uri): Long {
        var size = -1L
        try {
            val cursor: Cursor? = resolver.query(
                uri, arrayOf(OpenableColumns.SIZE), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndex(OpenableColumns.SIZE)
                    if (col >= 0 && !it.isNull(col)) size = it.getLong(col)
                }
            }
        } catch (_: Exception) {}
        return size
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
