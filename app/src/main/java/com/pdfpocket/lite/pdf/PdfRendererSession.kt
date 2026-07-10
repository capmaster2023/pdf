package com.pdfpocket.lite.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class PdfRendererSession @Inject constructor(
    @ApplicationContext private val context: Context
) : AutoCloseable {
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val mutex = Mutex()
    private val cache = object : LruCache<String, Bitmap>(32 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    val pageCount: Int get() = renderer?.pageCount ?: 0

    suspend fun open(uri: Uri) = withContext(Dispatchers.IO) {
        close()
        descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Cannot open PDF")
        renderer = PdfRenderer(requireNotNull(descriptor))
    }

    suspend fun openFile(file: File) = withContext(Dispatchers.IO) {
        close()
        descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(requireNotNull(descriptor))
    }

    suspend fun render(pageIndex: Int, targetWidth: Int, scale: Float = 1f): Bitmap = withContext(Dispatchers.IO) {
        mutex.withLock {
            val active = renderer ?: error("Renderer is closed")
            require(pageIndex in 0 until active.pageCount)
            val width = (targetWidth.coerceAtLeast(480) * scale.coerceIn(0.5f, 3f)).toInt().coerceAtMost(2600)
            val key = "$pageIndex:$width"
            cache.get(key)?.let { return@withLock it }
            active.openPage(pageIndex).use { page ->
                val height = (width * page.height.toFloat() / page.width).toInt().coerceIn(1, 4000)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                cache.put(key, bitmap)
                bitmap
            }
        }
    }

    override fun close() {
        cache.evictAll()
        renderer?.close()
        renderer = null
        descriptor?.close()
        descriptor = null
    }
}
