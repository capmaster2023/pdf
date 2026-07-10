package com.pdfpocket.lite.features.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfpocket.lite.R
import com.pdfpocket.lite.pdf.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesToPdfScreen(onBack: () -> Unit, viewModel: ImagesToPdfViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var format by remember { mutableStateOf(PaperFormat.A4) }
    var orientation by remember { mutableStateOf(PageOrientation.PORTRAIT) }
    var margin by remember { mutableIntStateOf(24) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { viewModel.add(it) }
    val output = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { selected ->
            context.contentResolver.openOutputStream(selected, "w")?.let { stream ->
                viewModel.create(stream, ImagePdfOptions(format, orientation, margin))
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.images_to_pdf)) }, navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_file))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaperFormat.entries.forEach { value ->
                    val label = when (value) {
                        PaperFormat.AUTO -> R.string.paper_auto
                        PaperFormat.A4 -> R.string.paper_a4
                        PaperFormat.LETTER -> R.string.paper_letter
                    }
                    FilterChip(selected = format == value, onClick = { format = value }, label = { Text(stringResource(label)) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PageOrientation.entries.forEach { value ->
                    val label = if (value == PageOrientation.PORTRAIT) R.string.orientation_portrait else R.string.orientation_landscape
                    FilterChip(selected = orientation == value, onClick = { orientation = value }, label = { Text(stringResource(label)) })
                }
            }
            Text(stringResource(R.string.margins))
            Slider(value = margin.toFloat(), onValueChange = { margin = it.toInt() }, valueRange = 0f..72f)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.images, key = { _, uri -> uri.toString() }) { index, uri ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(uri.lastPathSegment ?: uri.toString(), Modifier.weight(1f), maxLines = 2)
                            IconButton(onClick = { viewModel.move(index, -1) }, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.move_up)) }
                            IconButton(onClick = { viewModel.move(index, 1) }, enabled = index < state.images.lastIndex) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.move_down)) }
                            IconButton(onClick = { viewModel.remove(index) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                        }
                    }
                }
            }
            Button(
                onClick = { output.launch("images_pdf.pdf") },
                enabled = state.images.isNotEmpty() && !state.processing,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.create_pdf)) }
            if (state.processing) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    if (state.complete) ResultDialog(true, viewModel::dismiss)
    if (state.error) ResultDialog(false, viewModel::dismiss)
}

@Composable
private fun ResultDialog(success: Boolean, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(if (success) R.string.operation_complete else R.string.operation_failed)) },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) } }
    )
}
