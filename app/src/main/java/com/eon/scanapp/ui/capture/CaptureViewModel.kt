package com.eon.scanapp.ui.capture

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class CaptureUiState {
    object Idle : CaptureUiState()
    object Capturing : CaptureUiState()
    data class Success(val uri: Uri) : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
}

class CaptureViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun capturePhoto(
        context: Context,
        imageCapture: ImageCapture,
        onCaptured: (Uri) -> Unit
    ) {
        _uiState.value = CaptureUiState.Capturing

        // Simpan ke cache dulu, bukan storage final — user masih akan crop
        val photoFile = File(
            context.cacheDir,
            "scan_temp_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    _uiState.value = CaptureUiState.Success(savedUri)
                    onCaptured(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    _uiState.value = CaptureUiState.Error(
                        exception.message ?: "Gagal mengambil gambar"
                    )
                }
            }
        )
    }

    fun resetState() {
        _uiState.value = CaptureUiState.Idle
    }

    fun reportError(message: String) {
        _uiState.value = CaptureUiState.Error(message)
    }

}