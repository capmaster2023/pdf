package com.pdfpocket.lite.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val FILES = "files"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val VIEWER = "viewer/{uri}"
    const val IMAGES = "images"
    const val SCANNER = "scanner"
    const val OCR = "ocr"
    const val SIGNATURE = "signature"
    const val TOOL_OPERATION = "tool/{operation}"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"

    fun viewer(uri: String): String = "viewer/${Uri.encode(uri)}"
    fun tool(operation: String): String = "tool/${Uri.encode(operation)}"
}
