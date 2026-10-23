package com.eon.scanapp.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Locale

class PdfGenerator(private val context: Context) {

    fun writePdf(
        pageUris: List<Uri>,
        out: OutputStream,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY,
        maxDim: Int = DEFAULT_MAX_DIM
    ) {
        require(pageUris.isNotEmpty()) { "Tidak ada halaman" }

        val w = CountingOutputStream(BufferedOutputStream(out))
        val n = pageUris.size
        // Objek: 1 = Catalog, 2 = Pages, lalu per halaman: Page, Content, Image
        val offsets = LongArray(3 + 3 * n)

        w.writeAscii("%PDF-1.4\n")
        w.write(byteArrayOf(0x25, 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), 0x0A))

        offsets[1] = w.count
        w.writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        offsets[2] = w.count
        val kids = (0 until n).joinToString(" ") { "${3 + it * 3} 0 R" }
        w.writeAscii("2 0 obj\n<< /Type /Pages /Kids [$kids] /Count $n >>\nendobj\n")

        pageUris.forEachIndexed { i, uri ->
            val bitmap = BitmapLoader.load(context, uri, maxDim)
                ?: throw IOException("Gagal membaca gambar halaman ${i + 1}")

            val imgW = bitmap.width
            val imgH = bitmap.height
            val jpeg = ByteArrayOutputStream().also {
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, it)
            }.toByteArray()
            bitmap.recycle() // hanya 1 bitmap di memori pada satu waktu

            val pageW = PAGE_WIDTH
            val pageH = (PAGE_WIDTH.toFloat() * imgH / imgW).toInt().coerceAtLeast(1)

            val pageObj = 3 + i * 3
            val contentObj = pageObj + 1
            val imageObj = pageObj + 2

            offsets[pageObj] = w.count
            w.writeAscii(
                "$pageObj 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageW $pageH] " +
                        "/Resources << /XObject << /Im0 $imageObj 0 R >> >> /Contents $contentObj 0 R >>\nendobj\n"
            )

            val content = "q $pageW 0 0 $pageH 0 0 cm /Im0 Do Q"
            offsets[contentObj] = w.count
            w.writeAscii(
                "$contentObj 0 obj\n<< /Length ${content.length} >>\nstream\n$content\nendstream\nendobj\n"
            )

            offsets[imageObj] = w.count
            w.writeAscii(
                "$imageObj 0 obj\n<< /Type /XObject /Subtype /Image /Width $imgW /Height $imgH " +
                        "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode " +
                        "/Length ${jpeg.size} >>\nstream\n"
            )
            w.write(jpeg)
            w.writeAscii("\nendstream\nendobj\n")
        }

        val xrefOffset = w.count
        val size = offsets.size
        w.writeAscii("xref\n0 $size\n")
        w.writeAscii("0000000000 65535 f \n")
        for (k in 1 until size) {
            w.writeAscii(String.format(Locale.US, "%010d 00000 n \n", offsets[k]))
        }
        w.writeAscii("trailer\n<< /Size $size /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
        w.flush()
    }

    private class CountingOutputStream(private val inner: OutputStream) : OutputStream() {
        var count = 0L
            private set

        override fun write(b: Int) {
            inner.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            inner.write(b, off, len)
            count += len
        }

        override fun flush() = inner.flush()

        fun writeAscii(s: String) = write(s.toByteArray(Charsets.ISO_8859_1))
    }

    companion object {
        const val PAGE_WIDTH = 595
        const val DEFAULT_JPEG_QUALITY = 80
        const val DEFAULT_MAX_DIM = 1600
    }
}