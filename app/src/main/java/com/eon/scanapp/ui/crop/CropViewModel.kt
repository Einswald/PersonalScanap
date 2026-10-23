package com.eon.scanapp.ui.crop

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eon.scanapp.data.pdf.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

sealed class CropLoadState {
    object Loading : CropLoadState()
    data class Ready(val bitmap: Bitmap) : CropLoadState()
    data class Error(val message: String) : CropLoadState()
}

class CropViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<CropLoadState>(CropLoadState.Loading)
    val state: StateFlow<CropLoadState> = _state.asStateFlow()

    private var loadedUri: Uri? = null

    // ViewModel ini hidup selama Activity, jadi wajib reset saat URI berganti
    fun load(uri: Uri) {
        if (loadedUri == uri) return
        loadedUri = uri
        _state.value = CropLoadState.Loading
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) { BitmapLoader.load(getApplication(), uri) }
            _state.value = if (bmp != null) CropLoadState.Ready(bmp)
            else CropLoadState.Error("Gagal membuka gambar")
        }
    }

    /** region = fraksi 0..1 terhadap gambar (left, top, right, bottom). */
    suspend fun cropToCache(region: Rect): Result<Uri> {
        val ready = _state.value as? CropLoadState.Ready
            ?: return Result.failure(IllegalStateException("Gambar belum siap"))

        return withContext(Dispatchers.IO) {
            runCatching {
                val src = ready.bitmap
                val x = (region.left * src.width).roundToInt().coerceIn(0, src.width - 1)
                val y = (region.top * src.height).roundToInt().coerceIn(0, src.height - 1)
                val w = (region.width * src.width).roundToInt().coerceIn(1, src.width - x)
                val h = (region.height * src.height).roundToInt().coerceIn(1, src.height - y)

                val cropped = Bitmap.createBitmap(src, x, y, w, h)
                val file = File(
                    getApplication<Application>().cacheDir,
                    "scan_crop_${System.currentTimeMillis()}.jpg"
                )
                try {
                    val ok = FileOutputStream(file).use {
                        cropped.compress(Bitmap.CompressFormat.JPEG, 90, it)
                    }
                    if (!ok) throw IOException("Gagal menyimpan hasil crop")
                } catch (e: Exception) {
                    file.delete()
                    throw e
                } finally {
                    if (cropped !== src) cropped.recycle()
                }
                Uri.fromFile(file)
            }
        }
    }
}