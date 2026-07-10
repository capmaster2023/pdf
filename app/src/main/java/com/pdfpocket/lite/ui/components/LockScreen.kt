package com.pdfpocket.lite.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pdfpocket.lite.R

@Composable
fun LockScreen(
    biometricAvailable: Boolean,
    pinEnabled: Boolean,
    onBiometric: () -> Unit,
    onPin: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        if (pinEnabled) {
            OutlinedTextField(
                value = pin,
                onValueChange = { value -> if (value.length <= 12 && value.all(Char::isDigit)) { pin = value; invalid = false } },
                label = { Text(stringResource(R.string.enter_pin)) },
                visualTransformation = PasswordVisualTransformation(),
                isError = invalid,
                modifier = Modifier.fillMaxWidth()
            )
            if (invalid) Text(stringResource(R.string.invalid_pin), color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { if (!onPin(pin)) invalid = true },
                enabled = pin.length >= 4,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.unlock)) }
        }
        if (biometricAvailable) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.biometric_lock))
            }
        }
    }
}
