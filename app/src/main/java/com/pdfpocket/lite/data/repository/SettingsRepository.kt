package com.pdfpocket.lite.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pdfPocketDataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ViewerMode { CONTINUOUS, SINGLE }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val viewerMode: ViewerMode = ViewerMode.CONTINUOUS,
    val rememberLastPage: Boolean = true,
    val biometricLock: Boolean = false,
    val pinLock: Boolean = false,
    val allowScreenshots: Boolean = true,
    val exportQuality: Int = 85,
    val scannerQuality: Int = 85
)

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val viewerMode = stringPreferencesKey("viewer_mode")
        val rememberLastPage = booleanPreferencesKey("remember_last_page")
        val biometric = booleanPreferencesKey("biometric_lock")
        val pin = booleanPreferencesKey("pin_lock")
        val screenshots = booleanPreferencesKey("allow_screenshots")
        val exportQuality = intPreferencesKey("export_quality")
        val scannerQuality = intPreferencesKey("scanner_quality")
    }

    val settings: Flow<AppSettings> = context.pdfPocketDataStore.data.map { p ->
        AppSettings(
            theme = runCatching { ThemeMode.valueOf(p[Keys.theme] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            viewerMode = runCatching { ViewerMode.valueOf(p[Keys.viewerMode] ?: ViewerMode.CONTINUOUS.name) }.getOrDefault(ViewerMode.CONTINUOUS),
            rememberLastPage = p[Keys.rememberLastPage] ?: true,
            biometricLock = p[Keys.biometric] ?: false,
            pinLock = p[Keys.pin] ?: false,
            allowScreenshots = p[Keys.screenshots] ?: true,
            exportQuality = p[Keys.exportQuality] ?: 85,
            scannerQuality = p[Keys.scannerQuality] ?: 85
        )
    }

    suspend fun setTheme(value: ThemeMode) = context.pdfPocketDataStore.edit { it[Keys.theme] = value.name }
    suspend fun setViewerMode(value: ViewerMode) = context.pdfPocketDataStore.edit { it[Keys.viewerMode] = value.name }
    suspend fun setRememberLastPage(value: Boolean) = context.pdfPocketDataStore.edit { it[Keys.rememberLastPage] = value }
    suspend fun setBiometricLock(value: Boolean) = context.pdfPocketDataStore.edit { it[Keys.biometric] = value }
    suspend fun setPinLock(value: Boolean) = context.pdfPocketDataStore.edit { it[Keys.pin] = value }
    suspend fun setAllowScreenshots(value: Boolean) = context.pdfPocketDataStore.edit { it[Keys.screenshots] = value }
    suspend fun setExportQuality(value: Int) = context.pdfPocketDataStore.edit { it[Keys.exportQuality] = value.coerceIn(30, 100) }
    suspend fun setScannerQuality(value: Int) = context.pdfPocketDataStore.edit { it[Keys.scannerQuality] = value.coerceIn(30, 100) }
    suspend fun reset() = context.pdfPocketDataStore.edit { it.clear() }
}
