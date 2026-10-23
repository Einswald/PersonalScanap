package com.eon.scanapp.ui.crop

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    sourceUri: Uri,
    onCropped: (Uri) -> Unit,
    onCancel: () -> Unit,
    viewModel: CropViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()

    var crop by remember(sourceUri) { mutableStateOf(Rect(0.05f, 0.05f, 0.95f, 0.95f)) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(sourceUri) { viewModel.load(sourceUri) }
    BackHandler(enabled = !isSaving) { onCancel() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Potong Dokumen") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (val s = state) {
                    CropLoadState.Loading -> CircularProgressIndicator()
                    is CropLoadState.Error -> Text(s.message)
                    is CropLoadState.Ready -> CropArea(
                        bitmap = s.bitmap,
                        crop = crop,
                        onCropChange = { crop = it }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) { Text("Ulangi") }

                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            viewModel.cropToCache(crop)
                                .onSuccess { onCropped(it) }
                                .onFailure {
                                    isSaving = false
                                    Toast.makeText(
                                        context, it.message ?: "Gagal memotong", Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    },
                    enabled = state is CropLoadState.Ready && !isSaving,
                    modifier = Modifier.weight(1f)
                ) { Text(if (isSaving) "Memproses..." else "Potong") }
            }
        }
    }
}

private enum class Handle { TL, TR, BL, BR, MOVE, NONE }

private const val MIN_SIZE = 0.1f

@Composable
private fun CropArea(
    bitmap: Bitmap,
    crop: Rect,
    onCropChange: (Rect) -> Unit
) {
    val padPx = with(LocalDensity.current) { 20.dp.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val accent = MaterialTheme.colorScheme.primary

    // Area gambar yang tampil (fit-center) di dalam canvas, dalam piksel
    val imageRect = remember(canvasSize, bitmap) {
        if (canvasSize == IntSize.Zero) Rect.Zero else {
            val availW = canvasSize.width - padPx * 2
            val availH = canvasSize.height - padPx * 2
            val scale = min(availW / bitmap.width, availH / bitmap.height)
            val w = bitmap.width * scale
            val h = bitmap.height * scale
            val left = (canvasSize.width - w) / 2f
            val top = (canvasSize.height - h) / 2f
            Rect(left, top, left + w, top + h)
        }
    }

    val currentCrop by rememberUpdatedState(crop)
    val currentOnChange by rememberUpdatedState(onCropChange)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(imageRect) {
                if (imageRect.width <= 0f) return@pointerInput
                val radius = 40.dp.toPx()
                var active = Handle.NONE
                detectDragGestures(
                    onDragStart = { pos ->
                        active = hitTest(pos, currentCrop.toPx(imageRect), radius)
                    },
                    onDragEnd = { active = Handle.NONE },
                    onDragCancel = { active = Handle.NONE },
                    onDrag = { change, drag ->
                        change.consume()
                        if (active != Handle.NONE) {
                            currentOnChange(
                                applyDrag(
                                    currentCrop, active,
                                    drag.x / imageRect.width,
                                    drag.y / imageRect.height
                                )
                            )
                        }
                    }
                )
            }
    ) {
        if (imageRect.width <= 0f) return@Canvas

        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(imageRect.left.toInt(), imageRect.top.toInt()),
            dstSize = IntSize(imageRect.width.toInt(), imageRect.height.toInt())
        )

        val c = crop.toPx(imageRect)
        val dim = Color.Black.copy(alpha = 0.55f)
        // 4 persegi gelap di luar area crop
        drawRect(
            dim,
            Offset(imageRect.left, imageRect.top),
            Size(imageRect.width, c.top - imageRect.top)
        )
        drawRect(
            dim,
            Offset(imageRect.left, c.bottom),
            Size(imageRect.width, imageRect.bottom - c.bottom)
        )
        drawRect(dim, Offset(imageRect.left, c.top), Size(c.left - imageRect.left, c.height))
        drawRect(dim, Offset(c.right, c.top), Size(imageRect.right - c.right, c.height))

        drawRect(accent, c.topLeft, c.size, style = Stroke(width = 2.dp.toPx()))

        val r = 10.dp.toPx()
        listOf(c.topLeft, c.topRight, c.bottomLeft, c.bottomRight).forEach {
            drawCircle(Color.White, radius = r, center = it)
            drawCircle(accent, radius = r, center = it, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

private fun Rect.toPx(img: Rect) = Rect(
    img.left + left * img.width,
    img.top + top * img.height,
    img.left + right * img.width,
    img.top + bottom * img.height
)

private fun hitTest(pos: Offset, c: Rect, radius: Float): Handle {
    val corners = listOf(
        Handle.TL to c.topLeft, Handle.TR to c.topRight,
        Handle.BL to c.bottomLeft, Handle.BR to c.bottomRight
    )
    val nearest = corners.minByOrNull { (_, p) -> hypot(pos.x - p.x, pos.y - p.y) }!!
    if (hypot(pos.x - nearest.second.x, pos.y - nearest.second.y) <= radius) return nearest.first
    return if (c.contains(pos)) Handle.MOVE else Handle.NONE
}

private fun applyDrag(r: Rect, h: Handle, dx: Float, dy: Float): Rect = when (h) {
    Handle.TL -> Rect(
        (r.left + dx).coerceIn(0f, r.right - MIN_SIZE),
        (r.top + dy).coerceIn(0f, r.bottom - MIN_SIZE),
        r.right, r.bottom
    )

    Handle.TR -> Rect(
        r.left, (r.top + dy).coerceIn(0f, r.bottom - MIN_SIZE),
        (r.right + dx).coerceIn(r.left + MIN_SIZE, 1f), r.bottom
    )

    Handle.BL -> Rect(
        (r.left + dx).coerceIn(0f, r.right - MIN_SIZE), r.top,
        r.right, (r.bottom + dy).coerceIn(r.top + MIN_SIZE, 1f)
    )

    Handle.BR -> Rect(
        r.left, r.top,
        (r.right + dx).coerceIn(r.left + MIN_SIZE, 1f),
        (r.bottom + dy).coerceIn(r.top + MIN_SIZE, 1f)
    )

    Handle.MOVE -> r.translate(
        dx.coerceIn(-r.left, 1f - r.right),
        dy.coerceIn(-r.top, 1f - r.bottom)
    )

    Handle.NONE -> r
}