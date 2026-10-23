package com.eon.scanapp.ui.pagelist

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.eon.scanapp.data.pdf.BitmapLoader
import com.eon.scanapp.domain.model.ScanPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageListScreen(
    pages: List<ScanPage>,
    isSaving: Boolean,
    onAddPage: () -> Unit,
    onRemovePage: (String) -> Unit,
    onGenerate: () -> Unit,
    onDiscardAll: () -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Back dari daftar = keluar dari sesi scan, jadi harus konfirmasi (hasil belum disimpan)
    BackHandler(enabled = !isSaving) { showDiscardDialog = true }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Halaman (${pages.size})") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                    PageRow(
                        number = index + 1,
                        uri = page.uri,
                        enabled = !isSaving,
                        onRemove = { onRemovePage(page.id) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddPage,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Tambah Halaman") }

                Button(
                    onClick = onGenerate,
                    enabled = pages.isNotEmpty() && !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isSaving) "Menyimpan..." else "Simpan sebagai PDF") }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Buang semua halaman?") },
            text = { Text("Halaman yang belum disimpan akan hilang.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDiscardAll()
                }) { Text("Buang") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun PageRow(
    number: Int,
    uri: Uri,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PageThumbnail(
                uri = uri,
                modifier = Modifier
                    .width(72.dp)
                    .height(96.dp)
            )
            Text(
                text = "Halaman $number",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRemove, enabled = enabled) { Text("Hapus") }
        }
    }
}

@Composable
private fun PageThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { BitmapLoader.load(context, uri, 400) }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Pratinjau halaman",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}