package com.pdfpocket.lite.navigation

object Routes {
    const val HOME = "home"
    const val FILES = "files"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val MERGE = "tools/merge"
    const val SPLIT = "tools/split"
    const val IMAGES_TO_PDF = "tools/images_to_pdf"
    const val VIEWER = "viewer/{uri}"

    fun viewer(uriString: String): String = "viewer/" + android.net.Uri.encode(uriString)
}
