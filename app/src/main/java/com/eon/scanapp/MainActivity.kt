package com.eon.scanapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.eon.scanapp.ui.SaveState
import com.eon.scanapp.ui.ScanSessionViewModel
import com.eon.scanapp.ui.capture.CaptureScreen
import com.eon.scanapp.ui.crop.CropScreen
import com.eon.scanapp.ui.pagelist.PageListScreen
import java.io.File

class MainActivity : ComponentActivity() {

    private val session: ScanSessionViewModel by viewModels()

    private enum class Screen { CAPTURE, CROP, LIST }

    private val legacyStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            legacyStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val pages by session.pages.collectAsState()
                val saveState by session.saveState.collectAsState()

                var screen by rememberSaveable { mutableStateOf(Screen.CAPTURE) }
                var capturedUri by rememberSaveable { mutableStateOf<Uri?>(null) }

                LaunchedEffect(saveState) {
                    when (val s = saveState) {
                        is SaveState.Saved -> {
                            Toast.makeText(context, "Tersimpan di Documents/ScanApp", Toast.LENGTH_LONG).show()
                            session.resetSaveState()
                            screen = Screen.CAPTURE
                        }
                        is SaveState.Error -> {
                            Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                            session.resetSaveState()
                        }
                        else -> Unit
                    }
                }

                // Layar efektif: tahan state yang tidak valid (mis. daftar kosong setelah proses app dimatikan sistem)
                val pending = capturedUri
                val current = when {
                    screen == Screen.CROP && pending != null -> Screen.CROP
                    screen == Screen.LIST && pages.isNotEmpty() -> Screen.LIST
                    else -> Screen.CAPTURE
                }

                when (current) {
                    Screen.CAPTURE -> {
                        BackHandler(enabled = pages.isNotEmpty()) { screen = Screen.LIST }
                        CaptureScreen(
                            onPhotoCaptured = { uri ->
                                capturedUri = uri
                                screen = Screen.CROP
                            },
                            onNavigateBack = { finish() }
                        )
                    }

                    Screen.CROP -> CropScreen(
                        sourceUri = pending!!,
                        onCropped = { cropped ->
                            pending.path?.let { File(it).delete() } // foto asli tidak diperlukan lagi
                            session.addPage(cropped)
                            capturedUri = null
                            screen = Screen.LIST
                        },
                        onCancel = {
                            pending.path?.let { File(it).delete() }
                            capturedUri = null
                            screen = Screen.CAPTURE
                        }
                    )

                    Screen.LIST -> PageListScreen(
                        pages = pages,
                        isSaving = saveState is SaveState.Saving,
                        onAddPage = { screen = Screen.CAPTURE },
                        onRemovePage = session::removePage,
                        onGenerate = session::generateAndSave,
                        onDiscardAll = {
                            session.discardAll()
                            screen = Screen.CAPTURE
                        }
                    )
                }
            }
        }
    }
}