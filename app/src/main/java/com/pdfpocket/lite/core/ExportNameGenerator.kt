package com.pdfpocket.lite.core

object ExportNameGenerator {
    fun generate(original: String, suffix: String): String {
        val base = original.substringBeforeLast('.').ifBlank { "document" }
        return FilenameValidator.ensurePdf("${FilenameValidator.sanitize(base)}_${FilenameValidator.sanitize(suffix)}")
    }
}
