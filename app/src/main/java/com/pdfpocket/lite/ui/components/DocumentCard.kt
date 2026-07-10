package com.pdfpocket.lite.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pdfpocket.lite.R
import com.pdfpocket.lite.core.SizeFormatter
import com.pdfpocket.lite.data.local.DocumentEntity

@Composable
fun DocumentCard(document: DocumentEntity, onOpen: () -> Unit, onFavorite: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(document.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                val size = if (document.size >= 0) SizeFormatter.format(document.size) else stringResource(R.string.unknown)
                Text("$size • ${document.pageCount.coerceAtLeast(0)}", style = MaterialTheme.typography.bodySmall)
            }
            if (onFavorite != null) {
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (document.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorites)
                    )
                }
            }
        }
    }
}
