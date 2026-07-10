package com.pdfpocket.lite.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfpocket.lite.R
import com.pdfpocket.lite.data.repository.ThemeMode
import com.pdfpocket.lite.data.repository.ViewerMode

@Composable
fun SettingsScreen(
    onPrivacy: () -> Unit,
    onAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var pinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium) }
        item { SectionTitle(R.string.theme) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    }
                    FilterChip(settings.theme == mode, { viewModel.setTheme(mode) }, { Text(stringResource(label)) })
                }
            }
        }
        item { SectionTitle(R.string.default_view_mode) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewerMode.entries.forEach { mode ->
                    val label = if (mode == ViewerMode.CONTINUOUS) R.string.continuous_mode else R.string.single_page_mode
                    FilterChip(settings.viewerMode == mode, { viewModel.setViewerMode(mode) }, { Text(stringResource(label)) })
                }
            }
        }
        item { SettingSwitch(R.string.remember_last_page, settings.rememberLastPage, viewModel::setRemember) }
        item { SectionTitle(R.string.app_lock) }
        item { SettingSwitch(R.string.biometric_lock, settings.biometricLock, viewModel::setBiometric) }
        item { SettingSwitch(R.string.pin_lock, settings.pinLock) { enabled -> if (enabled) pinDialog = true else viewModel.disablePin() } }
        item { SectionTitle(R.string.screenshots) }
        item { SettingSwitch(R.string.allow_screenshots, settings.allowScreenshots, viewModel::setScreenshots) }
        item { OutlinedButton(onClick = viewModel::clearCache, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.clear_cache)) } }
        item { OutlinedButton(onClick = viewModel::deleteSignatures, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.delete_signatures)) } }
        item { OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_preferences)) } }
        item { TextButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.privacy)) } }
        item { TextButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.about)) } }
    }

    if (pinDialog) {
        var pin by remember { mutableStateOf("") }
        var confirmation by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pinDialog = false },
            title = { Text(stringResource(R.string.set_pin)) },
            text = {
                Column {
                    OutlinedTextField(
                        pin,
                        { value -> if (value.length <= 12 && value.all(Char::isDigit)) pin = value },
                        label = { Text(stringResource(R.string.enter_pin)) },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        confirmation,
                        { value -> if (value.length <= 12 && value.all(Char::isDigit)) confirmation = value },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.setPin(pin); pinDialog = false },
                    enabled = pin.length >= 4 && pin == confirmation
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { pinDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun SectionTitle(id: Int) { Text(stringResource(id), style = MaterialTheme.typography.titleMedium) }

@Composable
private fun SettingSwitch(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
