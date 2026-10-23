package com.eon.scanapp.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eon.scanapp.ui.SaveState
import com.eon.scanapp.ui.ScanSessionViewModel
import com.eon.scanapp.ui.capture.CaptureScreen
import com.eon.scanapp.ui.crop.CropScreen
import com.eon.scanapp.ui.guide.GuideScreen
import com.eon.scanapp.ui.home.HomeScreen
import com.eon.scanapp.ui.pagelist.PageListScreen
import com.eon.scanapp.ui.result.SaveResultScreen
import java.io.File

private object Routes {
    const val HOME = "home"
    const val GUIDE = "guide"
    const val CAPTURE = "capture"
    const val CROP = "crop"
    const val LIST = "list"
    const val RESULT = "result"
}

@Composable
fun AppNavHost(session: ScanSessionViewModel) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val pages by session.pages.collectAsState()
    val saveState by session.saveState.collectAsState()

    LaunchedEffect(saveState) {
        when (val s = saveState) {
            is SaveState.Saved -> {
                // Guard: setelah rotasi layar efek ini jalan lagi, jangan buat layar hasil ganda
                if (nav.currentDestination?.route != Routes.RESULT) {
                    nav.navigate(Routes.RESULT) { popUpTo(Routes.HOME) }
                }
            }

            is SaveState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                session.resetSaveState()
            }

            else -> Unit
        }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onScan = { nav.navigate(Routes.CAPTURE) },
                onGuide = { nav.navigate(Routes.GUIDE) }
            )
        }

        composable(Routes.GUIDE) {
            GuideScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.CAPTURE) {
            CaptureScreen(
                onPhotoCaptured = { uri ->
                    nav.navigate("${Routes.CROP}/${Uri.encode(uri.toString())}")
                },
                onNavigateBack = { nav.popBackStack() }
            )
        }

        composable(
            route = "${Routes.CROP}/{uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { entry ->
            val uri = Uri.parse(entry.arguments?.getString("uri").orEmpty())
            CropScreen(
                sourceUri = uri,
                onCropped = { cropped ->
                    uri.path?.let { File(it).delete() } // foto asli tidak diperlukan lagi
                    session.addPage(cropped)
                    // Datang dari daftar ("+ Tambah")? kembali ke sana. Kalau tidak, buat daftar baru.
                    if (!nav.popBackStack(Routes.LIST, inclusive = false)) {
                        nav.navigate(Routes.LIST) {
                            popUpTo(Routes.CAPTURE) { inclusive = true }
                        }
                    }
                },
                onCancel = {
                    uri.path?.let { File(it).delete() }
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.LIST) {
            // Daftar kosong (dibuang semua / dihapus semua / state pulih setelah proses mati) -> beranda.
            // Dikecualikan saat Saved: daftar memang dikosongkan oleh proses simpan.
            LaunchedEffect(pages.isEmpty(), saveState) {
                if (pages.isEmpty() && saveState !is SaveState.Saved) {
                    nav.popBackStack(Routes.HOME, inclusive = false)
                }
            }
            PageListScreen(
                pages = pages,
                isSaving = saveState is SaveState.Saving,
                onAddPage = { nav.navigate(Routes.CAPTURE) },
                onRemovePage = session::removePage,
                onGenerate = session::generateAndSave,
                onDiscardAll = session::discardAll
            )
        }

        composable(Routes.RESULT) {
            val saved = saveState as? SaveState.Saved
            if (saved != null) {
                SaveResultScreen(uri = saved.uri, onDone = session::resetSaveState)
            } else {
                // State direset (Scan Lagi / Back) -> kembali ke beranda
                LaunchedEffect(Unit) { nav.popBackStack(Routes.HOME, inclusive = false) }
            }
        }
    }
}