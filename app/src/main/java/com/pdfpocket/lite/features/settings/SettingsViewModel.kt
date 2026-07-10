package com.pdfpocket.lite.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfpocket.lite.data.repository.*
import com.pdfpocket.lite.security.PinManager
import com.pdfpocket.lite.storage.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pinManager: PinManager,
    private val storage: StorageRepository,
    private val signatures: SignatureRepository
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setTheme(value: ThemeMode) = viewModelScope.launch { settingsRepository.setTheme(value) }
    fun setViewerMode(value: ViewerMode) = viewModelScope.launch { settingsRepository.setViewerMode(value) }
    fun setRemember(value: Boolean) = viewModelScope.launch { settingsRepository.setRememberLastPage(value) }
    fun setBiometric(value: Boolean) = viewModelScope.launch { settingsRepository.setBiometricLock(value) }
    fun setScreenshots(value: Boolean) = viewModelScope.launch { settingsRepository.setAllowScreenshots(value) }
    fun setPin(pin: String) = viewModelScope.launch { pinManager.setPin(pin); settingsRepository.setPinLock(true) }
    fun disablePin() = viewModelScope.launch { pinManager.clear(); settingsRepository.setPinLock(false) }
    fun clearCache() { storage.clearTemporaryFiles() }
    fun deleteSignatures() = viewModelScope.launch { signatures.deleteAll() }
    fun reset() = viewModelScope.launch { settingsRepository.reset(); pinManager.clear() }
}
