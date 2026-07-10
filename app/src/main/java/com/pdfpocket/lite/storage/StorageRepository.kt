package com.pdfpocket.lite.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(@ApplicationContext private val context: Context) {
    fun persistPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    fun copyToCache(uri: Uri, prefix: String = "input", extension: String = "pdf"): File {
        val dir = File(context.cacheDir, "working").apply { mkdirs() }
        val file = File.createTempFile(prefix, ".$extension", dir)
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open input")
        input.use { source -> FileOutputStream(file).use { output -> source.copyTo(output) } }
        return file
    }

    fun writeOutput(uri: Uri, writer: (OutputStream) -> Unit) {
        val output = context.contentResolver.openOutputStream(uri, "w") ?: error("Cannot open output")
        output.use(writer)
    }

    fun shareIntent(uri: Uri, mimeType: String = "application/pdf"): Intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun fileProviderUri(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun clearTemporaryFiles() {
        listOf("working", "shared", "pdf-render", "scanner").forEach { File(context.cacheDir, it).deleteRecursively() }
    }
}
