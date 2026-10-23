package com.eon.scanapp.domain.model

import android.net.Uri
import java.util.UUID

data class ScanPage(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val timestamp: Long = System.currentTimeMillis()
)