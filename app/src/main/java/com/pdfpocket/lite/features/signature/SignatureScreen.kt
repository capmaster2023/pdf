package com.pdfpocket.lite.features.signature

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfpocket.lite.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(onBack: () -> Unit, viewModel: SignatureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pad by remember { mutableStateOf<SignaturePadView?>(null) }
    var name by remember { mutableStateOf("") }
    var textSignature by remember { mutableStateOf("") }
    var source by remember { mutableStateOf<Uri?>(null) }
    var page by remember { mutableIntStateOf(1) }
    var x by remember { mutableFloatStateOf(0.5f) }
    var y by remember { mutableFloatStateOf(0.8f) }
    var width by remember { mutableFloatStateOf(0.3f) }
    var opacity by remember { mutableFloatStateOf(1f) }

    val importImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.import(name, it) } }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source = it }
    val output = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null && source != null) viewModel.stamp(requireNotNull(source), uri, page, x, y, width, opacity)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.signature)) }, navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(stringResource(R.string.visual_signature_notice), style = MaterialTheme.typography.bodySmall) }
            item {
                AndroidView(
                    factory = { context -> SignaturePadView(context).also { pad = it } },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pad?.undo() }) { Text(stringResource(R.string.undo)) }
                    OutlinedButton(onClick = { pad?.clearPad() }) { Text(stringResource(R.string.delete)) }
                }
            }
            item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.signature_name)) }) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { pad?.let { viewModel.save(name, it.toPng()) } }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.save_signature))
                    }
                    OutlinedButton(onClick = { importImage.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.choose_image))
                    }
                }
            }
            item {
                OutlinedTextField(textSignature, { textSignature = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.signature)) })
                OutlinedButton(onClick = { viewModel.saveText(name, textSignature) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.save_signature))
                }
            }
            item { Text(stringResource(R.string.saved_signatures), style = MaterialTheme.typography.titleLarge) }
            items(state.signatures, key = { it.id }) { signature ->
                Card(
                    onClick = { viewModel.select(signature.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.selectedId == signature.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(signature.name, Modifier.weight(1f))
                        IconButton(onClick = { viewModel.delete(signature) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                    }
                }
            }
            item {
                Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.choose_pdf))
                }
                Text(source?.lastPathSegment.orEmpty())
            }
            item {
                OutlinedTextField(
                    value = page.toString(),
                    onValueChange = { page = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                    label = { Text(stringResource(R.string.signature_page)) }
                )
            }
            item {
                Text(stringResource(R.string.horizontal_position)); Slider(x, { x = it })
                Text(stringResource(R.string.vertical_position)); Slider(y, { y = it })
                Text(stringResource(R.string.signature_size)); Slider(width, { width = it }, valueRange = 0.1f..0.8f)
                Text(stringResource(R.string.opacity)); Slider(opacity, { opacity = it }, valueRange = 0.1f..1f)
            }
            item {
                Button(
                    onClick = { output.launch("signed.pdf") },
                    enabled = source != null && state.selectedId != null && !state.processing,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.choose_output)) }
            }
            if (state.processing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
    }

    if (state.complete) SignatureResultDialog(true, viewModel::dismiss)
    if (state.error) SignatureResultDialog(false, viewModel::dismiss)
}

@Composable
private fun SignatureResultDialog(success: Boolean, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(if (success) R.string.operation_complete else R.string.operation_failed)) },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) } }
    )
}
