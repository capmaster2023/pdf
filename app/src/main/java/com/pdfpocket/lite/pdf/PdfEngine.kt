package com.pdfpocket.lite.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pdfpocket.lite.core.PageRangeParser
import com.pdfpocket.lite.storage.StorageRepository
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.util.Matrix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

data class PdfSearchHit(val pageIndex: Int, val excerpt: String)
data class PdfFormField(val name: String, val type: String, val value: String)
enum class CompressionLevel(val renderScale: Float) { LIGHT(1.25f), BALANCED(0.9f), STRONG(0.65f) }

@Singleton
class PdfEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: StorageRepository
) {
    suspend fun pageCount(uri: Uri, password: String? = null): Int = withContext(Dispatchers.IO) {
        load(uri, password).use { it.numberOfPages }
    }

    suspend fun decryptToTemp(source: Uri, password: String): File = withContext(Dispatchers.IO) {
        load(source, password).use { document ->
            document.isAllSecurityToBeRemoved = true
            val directory = File(context.cacheDir, "pdf-render").apply { mkdirs() }
            File.createTempFile("unlocked_", ".pdf", directory).also { document.save(it) }
        }
    }

    suspend fun merge(inputs: List<Uri>, output: Uri) = withContext(Dispatchers.IO) {
        require(inputs.size >= 2)
        val cached = inputs.mapIndexed { index, uri -> storage.copyToCache(uri, "merge_$index") }
        try {
            storage.writeOutput(output) { stream ->
                val merger = PDFMergerUtility()
                cached.forEach { merger.addSource(it) }
                merger.destinationStream = stream
                merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
            }
        } finally {
            cached.forEach(File::delete)
        }
    }

    suspend fun extract(source: Uri, ranges: String, output: Uri, password: String? = null) = withContext(Dispatchers.IO) {
        load(source, password).use { input ->
            val selected = PageRangeParser.parse(ranges, input.numberOfPages).getOrThrow()
            PDDocument().use { result ->
                selected.forEach { pageIndex -> result.importPage(input.getPage(pageIndex)) }
                storage.writeOutput(output) { result.save(it) }
            }
        }
    }

    suspend fun reorder(source: Uri, order: List<Int>, output: Uri, password: String? = null) = withContext(Dispatchers.IO) {
        load(source, password).use { input ->
            require(order.isNotEmpty() && order.all { it in 0 until input.numberOfPages })
            PDDocument().use { result ->
                order.forEach { pageIndex -> result.importPage(input.getPage(pageIndex)) }
                storage.writeOutput(output) { result.save(it) }
            }
        }
    }

    suspend fun rotate(source: Uri, ranges: String?, angle: Int, output: Uri, password: String? = null) = withContext(Dispatchers.IO) {
        require(angle in setOf(90, 180, 270))
        load(source, password).use { document ->
            val selected = if (ranges.isNullOrBlank()) (0 until document.numberOfPages).toList() else PageRangeParser.parse(ranges, document.numberOfPages).getOrThrow()
            selected.forEach { index ->
                val page = document.getPage(index)
                page.rotation = (page.rotation + angle) % 360
            }
            storage.writeOutput(output) { document.save(it) }
        }
    }

    suspend fun watermark(
        source: Uri,
        text: String,
        output: Uri,
        opacity: Float = 0.18f,
        ranges: String? = null,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        val printable = text.filter { it.code in 32..255 }.take(120)
        require(printable.isNotBlank())
        load(source, password).use { document ->
            val selected = if (ranges.isNullOrBlank()) (0 until document.numberOfPages).toList() else PageRangeParser.parse(ranges, document.numberOfPages).getOrThrow()
            selected.forEach { index ->
                val page = document.getPage(index)
                val box = page.mediaBox
                val fontSize = (box.width / printable.length.coerceAtLeast(6) * 1.4f).coerceIn(24f, 72f)
                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                    val graphicsState = PDExtendedGraphicsState()
                    graphicsState.nonStrokingAlphaConstant = opacity.coerceIn(0.05f, 1f)
                    stream.setGraphicsStateParameters(graphicsState)
                    stream.setNonStrokingColor(120, 120, 120)
                    stream.beginText()
                    stream.setFont(PDType1Font.HELVETICA_BOLD, fontSize)
                    stream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(35.0), box.width * 0.18f, box.height * 0.35f))
                    stream.showText(printable)
                    stream.endText()
                }
            }
            storage.writeOutput(output) { document.save(it) }
        }
    }

    suspend fun addPageNumbers(
        source: Uri,
        output: Uri,
        startAt: Int = 1,
        prefix: String = "",
        suffix: String = "",
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        load(source, password).use { document ->
            for (index in 0 until document.numberOfPages) {
                val page = document.getPage(index)
                val box = page.mediaBox
                val text = "$prefix${startAt + index}$suffix".filter { it.code in 32..255 }.take(80)
                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                    stream.beginText()
                    stream.setFont(PDType1Font.HELVETICA, 10f)
                    val width = PDType1Font.HELVETICA.getStringWidth(text) / 1000f * 10f
                    stream.newLineAtOffset((box.width - width) / 2f, 18f)
                    stream.showText(text)
                    stream.endText()
                }
            }
            storage.writeOutput(output) { document.save(it) }
        }
    }

    suspend fun protect(source: Uri, output: Uri, userPassword: String, ownerPassword: String = userPassword) = withContext(Dispatchers.IO) {
        require(userPassword.length >= 8)
        load(source, null).use { document ->
            val policy = StandardProtectionPolicy(ownerPassword, userPassword, AccessPermission())
            policy.encryptionKeyLength = 128
            document.protect(policy)
            storage.writeOutput(output) { document.save(it) }
        }
    }

    suspend fun unlock(source: Uri, output: Uri, password: String) = withContext(Dispatchers.IO) {
        load(source, password).use { document ->
            document.isAllSecurityToBeRemoved = true
            storage.writeOutput(output) { document.save(it) }
        }
    }

    suspend fun search(source: Uri, query: String, password: String? = null): List<PdfSearchHit> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        load(source, password).use { document ->
            val stripper = PDFTextStripper()
            buildList {
                for (pageNumber in 1..document.numberOfPages) {
                    coroutineContext.ensureActive()
                    stripper.startPage = pageNumber
                    stripper.endPage = pageNumber
                    val pageText = stripper.getText(document)
                    var position = pageText.indexOf(query, ignoreCase = true)
                    while (position >= 0 && count { it.pageIndex == pageNumber - 1 } < 10) {
                        val start = (position - 35).coerceAtLeast(0)
                        val end = (position + query.length + 55).coerceAtMost(pageText.length)
                        add(PdfSearchHit(pageNumber - 1, pageText.substring(start, end).replace('\n', ' ')))
                        position = pageText.indexOf(query, position + query.length, ignoreCase = true)
                    }
                }
            }
        }
    }

    suspend fun stampSignature(
        source: Uri,
        output: Uri,
        png: ByteArray,
        pageIndex: Int,
        xRatio: Float,
        yRatio: Float,
        widthRatio: Float,
        opacity: Float = 1f,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        load(source, password).use { document ->
            require(pageIndex in 0 until document.numberOfPages)
            val page = document.getPage(pageIndex)
            val box = page.mediaBox
            val image = PDImageXObject.createFromByteArray(document, png, "signature")
            val width = box.width * widthRatio.coerceIn(0.1f, 0.8f)
            val height = width * image.height / image.width.toFloat()
            val x = (box.width - width) * xRatio.coerceIn(0f, 1f)
            val y = (box.height - height) * (1f - yRatio.coerceIn(0f, 1f))
            PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                val state = PDExtendedGraphicsState()
                state.nonStrokingAlphaConstant = opacity.coerceIn(0.1f, 1f)
                stream.setGraphicsStateParameters(state)
                stream.drawImage(image, x, y, width, height)
            }
            storage.writeOutput(output) { document.save(it) }
        }
    }

    suspend fun formFields(source: Uri, password: String? = null): List<PdfFormField> = withContext(Dispatchers.IO) {
        load(source, password).use { document ->
            val form = document.documentCatalog.acroForm ?: return@withContext emptyList()
            form.fieldTree.map { field ->
                PdfFormField(
                    name = field.fullyQualifiedName ?: field.partialName ?: "field",
                    type = field.javaClass.simpleName,
                    value = field.valueAsString ?: ""
                )
            }
        }
    }

    suspend fun fillForm(
        source: Uri,
        output: Uri,
        values: Map<String, String>,
        flatten: Boolean,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        load(source, password).use { document ->
            val form = document.documentCatalog.acroForm ?: error("No AcroForm")
            values.forEach { (name, value) -> form.getField(name)?.value = value }
            if (flatten) form.flatten()
            storage.writeOutput(output) { document.save(it) }
        }
    }

    suspend fun pdfToImages(
        source: Uri,
        folder: Uri,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 90
    ) = withContext(Dispatchers.IO) {
        val target = DocumentFile.fromTreeUri(context, folder) ?: error("Invalid folder")
        val session = PdfRendererSession(context)
        session.open(source)
        try {
            for (index in 0 until session.pageCount) {
                coroutineContext.ensureActive()
                val bitmap = session.render(index, 1600)
                val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
                val mime = if (extension == "png") "image/png" else "image/jpeg"
                val file = target.createFile(mime, "page_${index + 1}.$extension") ?: error("Cannot create image")
                context.contentResolver.openOutputStream(file.uri)?.use { bitmap.compress(format, quality.coerceIn(30, 100), it) }
                    ?: error("Cannot write image")
            }
        } finally {
            session.close()
        }
    }

    suspend fun extractEmbeddedImages(source: Uri, folder: Uri, password: String? = null): Int = withContext(Dispatchers.IO) {
        val target = DocumentFile.fromTreeUri(context, folder) ?: error("Invalid folder")
        var count = 0
        load(source, password).use { document ->
            for (pageIndex in 0 until document.numberOfPages) {
                coroutineContext.ensureActive()
                val resources = document.getPage(pageIndex).resources ?: continue
                for (name in resources.xObjectNames) {
                    val image = resources.getXObject(name) as? PDImageXObject ?: continue
                    val bitmap = image.image ?: continue
                    val file = target.createFile("image/png", "image_${++count}.png") ?: continue
                    context.contentResolver.openOutputStream(file.uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
        }
        count
    }

    suspend fun compress(source: Uri, output: Uri, level: CompressionLevel) = withContext(Dispatchers.IO) {
        val session = PdfRendererSession(context)
        session.open(source)
        val result = PdfDocument()
        try {
            for (index in 0 until session.pageCount) {
                coroutineContext.ensureActive()
                val bitmap = session.render(index, (1200 * level.renderScale).toInt())
                val page = result.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                page.canvas.drawColor(Color.WHITE)
                page.canvas.drawBitmap(bitmap, null, fit(bitmap.width, bitmap.height, 595f, 842f), Paint(Paint.FILTER_BITMAP_FLAG))
                result.finishPage(page)
            }
            storage.writeOutput(output) { result.writeTo(it) }
        } finally {
            result.close()
            session.close()
        }
    }

    private fun fit(width: Int, height: Int, pageWidth: Float, pageHeight: Float): RectF {
        val scale = minOf(pageWidth / width, pageHeight / height)
        val targetWidth = width * scale
        val targetHeight = height * scale
        return RectF(
            (pageWidth - targetWidth) / 2f,
            (pageHeight - targetHeight) / 2f,
            (pageWidth + targetWidth) / 2f,
            (pageHeight + targetHeight) / 2f
        )
    }

    private fun load(uri: Uri, password: String?): PDDocument {
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open PDF")
        return try {
            if (password.isNullOrEmpty()) PDDocument.load(input) else PDDocument.load(input, password)
        } finally {
            input.close()
        }
    }
}
