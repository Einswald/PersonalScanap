package com.eon.scanapp.ui.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val steps = listOf(
    "Ketuk Scan Dokumen" to "Izinkan akses kamera saat diminta.",
    "Ambil foto dokumen" to "Letakkan dokumen di permukaan datar dengan cahaya merata, pas di dalam bingkai putih, lalu ketuk tombol kamera.",
    "Potong gambar" to "Tarik sudut kotak untuk memilih area dokumen, atau geser seluruh kotak. Ketuk Potong. Pilih Ulangi untuk memotret ulang.",
    "Atur halaman" to "Di daftar halaman, ketuk + Tambah Halaman untuk dokumen lebih dari satu lembar, atau Hapus untuk membuang halaman.",
    "Simpan sebagai PDF" to "File tersimpan di folder Documents/ScanApp. Dari layar hasil, kamu bisa langsung membuka atau membagikannya."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Cara Pakai") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { index, (title, body) ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${index + 1}. $title", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text(
                "Catatan: aplikasi tidak menyimpan riwayat scan. Halaman yang belum disimpan akan hilang jika kamu keluar dari daftar halaman. Kelola file hasil lewat aplikasi File Manager.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}