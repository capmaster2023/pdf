package com.pdfpocket.lite.features.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pdfpocket.lite.R

data class ToolItem(val operation: String, val title: Int, val icon: ImageVector)

@Composable
fun ToolsScreen(
    onTool: (String) -> Unit,
    onImages: () -> Unit,
    onScanner: () -> Unit,
    onOcr: () -> Unit,
    onSignature: () -> Unit
) {
    val tools = listOf(
        ToolItem("merge", R.string.merge_pdf, Icons.Default.CallMerge),
        ToolItem("split", R.string.split_pdf, Icons.Default.CallSplit),
        ToolItem("rotate", R.string.rotate_pdf, Icons.Default.RotateRight),
        ToolItem("compress", R.string.compress_pdf, Icons.Default.Compress),
        ToolItem("watermark", R.string.watermark, Icons.Default.BrandingWatermark),
        ToolItem("numbers", R.string.page_numbers, Icons.Default.FormatListNumbered),
        ToolItem("protect", R.string.protect_pdf, Icons.Default.Lock),
        ToolItem("unlock", R.string.unlock_pdf, Icons.Default.LockOpen),
        ToolItem("pdf_to_images", R.string.pdf_to_images, Icons.Default.Image),
        ToolItem("extract_images", R.string.extract_images, Icons.Default.Collections),
        ToolItem("forms", R.string.forms, Icons.Default.ListAlt)
    )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.tools), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onScanner, label = { Text(stringResource(R.string.scan_document)) }, leadingIcon = { Icon(Icons.Default.DocumentScanner, null) })
            AssistChip(onClick = onImages, label = { Text(stringResource(R.string.images_to_pdf)) }, leadingIcon = { Icon(Icons.Default.Collections, null) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onOcr, label = { Text(stringResource(R.string.ocr)) }, leadingIcon = { Icon(Icons.Default.TextFields, null) })
            AssistChip(onClick = onSignature, label = { Text(stringResource(R.string.signature)) }, leadingIcon = { Icon(Icons.Default.Draw, null) })
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tools, key = { it.operation }) { tool ->
                ElevatedCard(onClick = { onTool(tool.operation) }, modifier = Modifier.height(110.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Icon(tool.icon, contentDescription = stringResource(tool.title))
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(tool.title), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
