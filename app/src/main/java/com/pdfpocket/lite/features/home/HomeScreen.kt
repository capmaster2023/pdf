package com.pdfpocket.lite.features.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfpocket.lite.R
import com.pdfpocket.lite.ui.components.DocumentCard

@Composable
fun HomeScreen(
    onOpenDocument: (Uri) -> Unit,
    onScanner: () -> Unit,
    onImages: () -> Unit,
    onTool: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onOpenDocument) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { picker.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.open_pdf))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Modifier.weight(1f), Icons.Default.DocumentScanner, stringResource(R.string.scan_document), onScanner)
                QuickAction(Modifier.weight(1f), Icons.Default.Collections, stringResource(R.string.images_to_pdf), onImages)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Modifier.weight(1f), Icons.Default.CallMerge, stringResource(R.string.merge_pdf)) { onTool("merge") }
                QuickAction(Modifier.weight(1f), Icons.Default.Compress, stringResource(R.string.compress_pdf)) { onTool("compress") }
                QuickAction(Modifier.weight(1f), Icons.Default.Draw, stringResource(R.string.sign_pdf)) { onTool("signature") }
            }
        }
        item { Text(stringResource(R.string.recent_files), style = MaterialTheme.typography.titleLarge) }
        if (documents.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp)) {
                        Text(stringResource(R.string.empty_documents), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.empty_documents_hint))
                    }
                }
            }
        } else {
            items(documents.take(5), key = { it.uri }) { document ->
                DocumentCard(document, onOpen = { onOpenDocument(Uri.parse(document.uri)) })
            }
        }
        if (favorites.isNotEmpty()) {
            item { Text(stringResource(R.string.favorites), style = MaterialTheme.typography.titleLarge) }
            items(favorites.take(4), key = { "fav-${it.uri}" }) { document ->
                DocumentCard(document, onOpen = { onOpenDocument(Uri.parse(document.uri)) })
            }
        }
    }
}

@Composable
private fun QuickAction(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier.heightIn(min = 92.dp)) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = label)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
