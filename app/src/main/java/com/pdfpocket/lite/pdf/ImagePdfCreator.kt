package com.pdfpocket.lite.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class PaperFormat { AUTO, A4, LETTER }
enum class PageOrientation { PORTRAIT, LANDSCAPE }

data class ImagePdfOptions(
    val format: PaperFormat = PaperFormat.A4,
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val marginPoints: Int = 24,
    val jpegQuality: Int = 85
)

@Singleton
class ImagePdfCreator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun create(images: List<Uri>, output: OutputStream, options: ImagePdfOptions) = withContext(Dispatchers.IO) {
        require(images.isNotEmpty())
        val document = PdfDocument()
        try {
            images.forEachIndexed { index, uri ->
                val bitmap = decodeSampled(uri, 2400) ?: error("Unsupported image")
                try {
                    val (pageWidth, pageHeight) = pageDimensions(bitmap, options)
                    val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                    page.canvas.drawColor(Color.WHITE)
                    val margin = options.marginPoints.coerceAtLeast(0).toFloat()
                    val bounds = RectF(margin, margin, pageWidth - margin, pageHeight - margin)
                    page.canvas.drawBitmap(bitmap, null, fitCenter(bitmap.width, bitmap.height, bounds), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    document.finishPage(page)
                } finally {
                    bitmap.recycle()
                }
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun pageDimensions(bitmap: Bitmap, options: ImagePdfOptions): Pair<Int, Int> {
        var dimensions = when (options.format) {
            PaperFormat.A4 -> 595 to 842
            PaperFormat.LETTER -> 612 to 792
            PaperFormat.AUTO -> bitmap.width.coerceIn(320, 2000) to bitmap.height.coerceIn(320, 2800)
        }
        if (options.orientation == PageOrientation.LANDSCAPE && dimensions.first < dimensions.second) dimensions = dimensions.second to dimensions.first
        if (options.orientation == PageOrientation.PORTRAIT && dimensions.first > dimensions.second) dimensions = dimensions.second to dimensions.first
        return dimensions
    }

    private fun fitCenter(sourceWidth: Int, sourceHeight: Int, bounds: RectF): RectF {
        val scale = minOf(bounds.width() / sourceWidth, bounds.height() / sourceHeight)
        val width = sourceWidth * scale
        val height = sourceHeight * scale
        val left = bounds.left + (bounds.width() - width) / 2f
        val top = bounds.top + (bounds.height() - height) / 2f
        return RectF(left, top, left + width, top + height)
    }

    private fun decodeSampled(uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maxDimension) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
