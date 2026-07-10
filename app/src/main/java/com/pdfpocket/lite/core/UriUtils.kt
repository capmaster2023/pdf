package com.pdfpocket.lite.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class UriMetadata(val name: String, val size: Long)

fun Context.queryMetadata(uri: Uri): UriMetadata {
    var name = uri.lastPathSegment ?: "document.pdf"
    var size = -1L
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) ?: name }
            cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { if (!cursor.isNull(it)) size = cursor.getLong(it) }
        }
    }
    return UriMetadata(name, size)
}
