package com.pdfpocket.lite.features.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfpocket.lite.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(onBack: () -> Unit, viewModel: OcrViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(1) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::processImage) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.processPdf(it, page) } }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { selected -> context.contentResolver.openOutputStream(selected, "w")?.use { it.write(state.text.toByteArray()) } }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.ocr)) }, navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.ocr_disclaimer), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.choose_image)) }
                Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.choose_pdf)) }
            }
            OutlinedTextField(
                value = page.toString(),
                onValueChange = { page = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                label = { Text(stringResource(R.string.signature_page)) }
            )
            if (state.processing) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::edit,
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text(stringResource(R.string.recognized_text)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.recognized_text), state.text))
                    },
                    enabled = state.text.isNotBlank()
                ) { Text(stringResource(R.string.copy_text)) }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, state.text) }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_text)))
                    },
                    enabled = state.text.isNotBlank()
                ) { Text(stringResource(R.string.share_text)) }
                OutlinedButton(onClick = { save.launch("ocr.txt") }, enabled = state.text.isNotBlank()) {
                    Text(stringResource(R.string.save_text))
                }
            }
        }
    }
}
