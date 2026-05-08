package com.anyfile.x

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OpenBridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val _detectionResult = MutableStateFlow<MimeDetector.DetectionResult?>(null)
    val detectionResult: StateFlow<MimeDetector.DetectionResult?> = _detectionResult

    private val _isDetecting = MutableStateFlow(false)
    val isDetecting: StateFlow<Boolean> = _isDetecting

    // Events that should only be handled once (like auto-opening)
    private val _autoOpenEvent = MutableSharedFlow<MimeDetector.DetectionResult>(replay = 0)
    val autoOpenEvent: SharedFlow<MimeDetector.DetectionResult> = _autoOpenEvent

    fun detectMime(uri: Uri, fileName: String?, triggerAutoOpen: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isDetecting.value = true
            try {
                val result = MimeDetector.detect(
                    getApplication<Application>().contentResolver,
                    uri,
                    fileName
                )
                _detectionResult.value = result
                if (triggerAutoOpen) {
                    _autoOpenEvent.emit(result)
                }
            } catch (e: Exception) {
                _detectionResult.value = null
            } finally {
                _isDetecting.value = false
            }
        }
    }
    
    fun resetDetection() {
        _detectionResult.value = null
    }

    fun queryFileName(uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = getApplication<Application>().contentResolver.query(
                uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (col >= 0) name = it.getString(col)
                }
            }
        } catch (_: Exception) {}
        return name
    }

    fun addToRecents(uri: Uri, fileName: String, mime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RecentFileStore.addRecentFile(getApplication(), RecentFile(uri.toString(), fileName, mime))
        }
    }

    /**
     * Proactive caching: Manually set a known MIME result to avoid re-detection.
     */
    fun setDetectedMimeManual(mime: String) {
        // Find the best FileType match for the cached MIME
        val fileType = when {
            mime == "application/vnd.android.package-archive" -> MimeDetector.FileType.APK
            mime == "application/pdf" -> MimeDetector.FileType.DOCUMENT
            mime == "application/zip" || mime.contains("archive") || mime.contains("tar") -> MimeDetector.FileType.ARCHIVE
            mime.startsWith("video/") -> MimeDetector.FileType.VIDEO
            mime.startsWith("audio/") -> MimeDetector.FileType.AUDIO
            mime.startsWith("image/") -> MimeDetector.FileType.IMAGE
            mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") -> MimeDetector.FileType.TEXT
            mime.startsWith("font/") -> MimeDetector.FileType.FONT
            else -> MimeDetector.FileType.UNKNOWN
        }
               
        _detectionResult.value = MimeDetector.DetectionResult(fileType, mime)
    }
}
