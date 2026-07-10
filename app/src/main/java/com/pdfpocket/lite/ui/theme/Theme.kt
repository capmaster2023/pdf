package com.pdfpocket.lite.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pdfpocket.lite.data.repository.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF246BFE),
    onPrimary = Color.White,
    secondary = Color(0xFF5A5F72),
    surfaceVariant = Color(0xFFE7EAF2),
    background = Color(0xFFF8F9FD)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AB7FF),
    secondary = Color(0xFFC2C6DC),
    background = Color(0xFF101218),
    surface = Color(0xFF171920),
    surfaceVariant = Color(0xFF2B2E38)
)

@Composable
fun PdfPocketTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = Typography(), content = content)
}
