package com.openbridge

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
}
