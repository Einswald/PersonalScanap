package com.eon.scanapp.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eon.scanapp.data.pdf.PdfGenerator
import com.eon.scanapp.data.storage.FileStorageRepository
import com.eon.scanapp.domain.model.ScanPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    data class Saved(val uri: Uri) : SaveState()
    data class Error(val message: String) : SaveState()
}

class ScanSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = FileStorageRepository(app)
    private val pdfGenerator = PdfGenerator(app)

    private val _pages = MutableStateFlow<List<ScanPage>>(emptyList())
    val pages: StateFlow<List<ScanPage>> = _pages.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun addPage(uri: Uri) = _pages.update { it + ScanPage(uri = uri) }

    fun removePage(id: String) {
        _pages.value.firstOrNull { it.id == id }?.uri?.path?.let { File(it).delete() }
        _pages.update { list -> list.filterNot { it.id == id } }
    }

    fun discardAll() {
        _pages.value.forEach { page -> page.uri.path?.let { File(it).delete() } }
        _pages.value = emptyList()
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    fun generateAndSave() {
        val snapshot = _pages.value
        if (snapshot.isEmpty() || _saveState.value is SaveState.Saving) return
        _saveState.value = SaveState.Saving

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                storage.savePdf { out -> pdfGenerator.writePdf(snapshot.map { it.uri }, out) }
            }
            result
                .onSuccess { uri ->
                    withContext(Dispatchers.IO) { deleteTempFiles(snapshot) }
                    _pages.value = emptyList()
                    _saveState.value = SaveState.Saved(uri)
                }
                .onFailure {
                    _saveState.value = SaveState.Error(it.message ?: "Gagal menyimpan PDF")
                }
        }
    }

    private fun deleteTempFiles(pages: List<ScanPage>) {
        pages.forEach { page -> page.uri.path?.let { File(it).delete() } }
    }
}