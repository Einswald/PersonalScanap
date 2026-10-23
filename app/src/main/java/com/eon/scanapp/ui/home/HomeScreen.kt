package com.eon.scanapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onGuide: () -> Unit
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PersonalScan", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan dokumen jadi PDF.\nFile tersimpan langsung di perangkatmu.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text("Scan Dokumen") }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text("Cara Pakai") }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text("Kompres (segera hadir)") }
        }
    }
}