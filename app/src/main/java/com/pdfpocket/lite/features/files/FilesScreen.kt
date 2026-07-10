package com.pdfpocket.lite.features.files

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfpocket.lite.R
import com.pdfpocket.lite.ui.components.DocumentCard

@Composable
fun FilesScreen(onOpen: (Uri) -> Unit, viewModel: FilesViewModel = hiltViewModel()) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.documents), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = viewModel::clearHistory) {
                Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.clear_history))
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { value -> query = value; viewModel.setQuery(value) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        Box {
            TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.sort)) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.sort_name)) }, onClick = { viewModel.setSort(FileSort.NAME); expanded = false })
                DropdownMenuItem(text = { Text(stringResource(R.string.sort_date)) }, onClick = { viewModel.setSort(FileSort.DATE); expanded = false })
                DropdownMenuItem(text = { Text(stringResource(R.string.sort_size)) }, onClick = { viewModel.setSort(FileSort.SIZE); expanded = false })
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(documents, key = { it.uri }) { document ->
                DocumentCard(
                    document = document,
                    onOpen = { onOpen(Uri.parse(document.uri)) },
                    onFavorite = { viewModel.setFavorite(document) }
                )
            }
        }
    }
}
