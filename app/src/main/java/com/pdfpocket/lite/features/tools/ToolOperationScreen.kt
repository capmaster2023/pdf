package com.pdfpocket.lite.features.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.pdfpocket.lite.pdf.CompressionLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolOperationScreen(operation: String, onBack: () -> Unit, viewModel: ToolOperationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<Uri?>(null) }
    var inputs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var ranges by remember { mutableStateOf("1") }
    var text by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var angle by remember { mutableIntStateOf(90) }
    var opacity by remember { mutableFloatStateOf(0.2f) }
    var startNumber by remember { mutableIntStateOf(1) }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(CompressionLevel.BALANCED) }
    var flatten by remember { mutableStateOf(false) }
    var formValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val sourcePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source = it }
    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { inputs = it }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        if (folder != null && source != null) viewModel.executeFolder(operation, requireNotNull(source), folder, password)
    }
    val outputPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { output ->
        if (output != null) {
            viewModel.execute(
                operation, source, inputs, output, ranges, text, password, confirmPassword,
                angle, opacity, startNumber, prefix, suffix, level, formValues, flatten
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(titleFor(operation))) }, navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (operation == "merge") {
                    Button(onClick = { multiPicker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.add_file))
                    }
                    Text(inputs.joinToString("\n") { it.lastPathSegment ?: it.toString() }, style = MaterialTheme.typography.bodySmall)
                } else {
                    Button(onClick = { sourcePicker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.choose_pdf))
                    }
                    Text(source?.lastPathSegment.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (operation in listOf("split", "rotate", "watermark")) {
                item {
                    OutlinedTextField(
                        value = ranges,
                        onValueChange = { ranges = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.page_ranges)) },
                        supportingText = { Text(stringResource(R.string.page_ranges_hint)) }
                    )
                }
            }

            if (operation == "rotate") {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(90, 180, 270).forEach { value ->
                            FilterChip(selected = angle == value, onClick = { angle = value }, label = { Text("$value°") })
                        }
                    }
                }
            }

            if (operation == "watermark") {
                item { OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.watermark_text)) }) }
                item { Text(stringResource(R.string.opacity)); Slider(opacity, { opacity = it }, valueRange = 0.05f..1f) }
            }

            if (operation == "numbers") {
                item { OutlinedTextField(startNumber.toString(), { startNumber = it.toIntOrNull() ?: 1 }, label = { Text(stringResource(R.string.start_number)) }) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(prefix, { prefix = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.prefix)) })
                        OutlinedTextField(suffix, { suffix = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.suffix)) })
                    }
                }
            }

            if (operation in listOf("protect", "unlock", "split", "rotate", "watermark", "numbers", "forms", "extract_images")) {
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.password)) },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }

            if (operation == "protect") {
                item {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.confirm_password)) },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Text(stringResource(R.string.password_warning), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (operation == "compress") {
                item {
                    Column {
                        CompressionLevel.entries.forEach { value ->
                            val label = when (value) {
                                CompressionLevel.LIGHT -> R.string.compression_light
                                CompressionLevel.BALANCED -> R.string.compression_balanced
                                CompressionLevel.STRONG -> R.string.compression_strong
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                RadioButton(selected = level == value, onClick = { level = value })
                                Text(stringResource(label))
                            }
                        }
                        Text(stringResource(R.string.compression_warning), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (operation == "forms") {
                item {
                    OutlinedButton(
                        onClick = { source?.let { viewModel.loadForm(it, password.ifBlank { null }) } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.load_form)) }
                }
                if (state.formFields.isEmpty()) {
                    item { Text(stringResource(R.string.no_form), style = MaterialTheme.typography.bodySmall) }
                }
                state.formFields.forEach { field ->
                    item(key = field.name) {
                        OutlinedTextField(
                            value = formValues[field.name] ?: field.value,
                            onValueChange = { value -> formValues = formValues + (field.name to value) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(field.name) }
                        )
                    }
                }
                item {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(checked = flatten, onCheckedChange = { flatten = it })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.flatten_form))
                    }
                }
            }

            item {
                val folderOperation = operation in listOf("pdf_to_images", "extract_images")
                val ready = if (operation == "merge") inputs.size >= 2 else source != null
                Button(
                    onClick = {
                        if (folderOperation) folderPicker.launch(null)
                        else outputPicker.launch("pdf_pocket_${operation}.pdf")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ready && !state.processing
                ) {
                    Text(stringResource(if (folderOperation) R.string.choose_folder else R.string.choose_output))
                }
            }

            if (state.processing) {
                item {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.processing))
                }
            }
            item { Text(stringResource(R.string.technical_limit), style = MaterialTheme.typography.bodySmall) }
        }
    }

    if (state.complete) OperationResultDialog(true, viewModel::dismissResult)
    if (state.error != null) OperationResultDialog(false, viewModel::dismissResult)
}

@Composable
private fun OperationResultDialog(success: Boolean, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(if (success) R.string.operation_complete else R.string.operation_failed)) },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) } }
    )
}

private fun titleFor(operation: String): Int = when (operation) {
    "merge" -> R.string.merge_pdf
    "split" -> R.string.split_pdf
    "rotate" -> R.string.rotate_pdf
    "compress" -> R.string.compress_pdf
    "watermark" -> R.string.watermark
    "numbers" -> R.string.page_numbers
    "protect" -> R.string.protect_pdf
    "unlock" -> R.string.unlock_pdf
    "pdf_to_images" -> R.string.pdf_to_images
    "extract_images" -> R.string.extract_images
    "forms" -> R.string.forms
    else -> R.string.tools
}
