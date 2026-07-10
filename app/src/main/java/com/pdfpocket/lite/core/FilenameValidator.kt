package com.pdfpocket.lite.core

object FilenameValidator {
    private val invalid = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")
    fun sanitize(value: String, fallback: String = "document"): String {
        val cleaned = value.trim().replace(invalid, "_").trim('.', ' ')
        return cleaned.ifBlank { fallback }.take(120)
    }
    fun ensurePdf(value: String): String = sanitize(value).let { if (it.endsWith(".pdf", true)) it else "$it.pdf" }
}
