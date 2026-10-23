package com.eon.scanapp.data.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileStorageRepository(private val context: Context) {

    fun savePdf(write: (OutputStream) -> Unit): Result<Uri> = runCatching {
        val name =
            "Scan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveScoped(name, write)
        else saveLegacy(name, write)
    }

    private fun saveScoped(name: String, write: (OutputStream) -> Unit): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/$FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: throw IOException("Gagal membuat file")
        try {
            resolver.openOutputStream(uri)?.use(write) ?: throw IOException("Gagal membuka file")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null) // jangan tinggalkan file setengah jadi
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(name: String, write: (OutputStream) -> Unit): Uri {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            FOLDER
        )
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Gagal membuat folder")
        val file = File(dir, name)
        try {
            FileOutputStream(file).use(write)
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("application/pdf"),
            null
        )
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)    }

    private companion object {
        const val FOLDER = "ScanApp"
    }
}