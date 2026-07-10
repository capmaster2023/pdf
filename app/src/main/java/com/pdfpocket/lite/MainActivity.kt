package com.pdfpocket.lite

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfpocket.lite.data.repository.AppSettings
import com.pdfpocket.lite.data.repository.SettingsRepository
import com.pdfpocket.lite.navigation.AppNavigation
import com.pdfpocket.lite.security.PinManager
import com.pdfpocket.lite.ui.components.LockScreen
import com.pdfpocket.lite.ui.theme.PdfPocketTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var pinManager: PinManager

    private var incomingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        incomingUri = extractUri(intent)
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            var unlocked by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(settings.biometricLock, settings.pinLock) {
                unlocked = !(settings.biometricLock || settings.pinLock)
            }
            LaunchedEffect(settings.allowScreenshots) {
                if (settings.allowScreenshots) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            PdfPocketTheme(settings.theme) {
                if (unlocked) {
                    AppNavigation(incomingUri = incomingUri, onIncomingConsumed = { incomingUri = null })
                } else {
                    LockScreen(
                        biometricAvailable = settings.biometricLock && biometricAvailable(),
                        pinEnabled = settings.pinLock && pinManager.hasPin(),
                        onBiometric = { authenticate { unlocked = true } },
                        onPin = { pin -> pinManager.verify(pin).also { valid -> if (valid) unlocked = true } }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingUri = extractUri(intent)
    }

    private fun extractUri(source: Intent?): Uri? = when (source?.action) {
        Intent.ACTION_VIEW -> source.data
        Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= 33) {
            source.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            source.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        else -> null
    }

    private fun biometricAvailable(): Boolean = BiometricManager.from(this).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS

    private fun authenticate(onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        )
    }
}
