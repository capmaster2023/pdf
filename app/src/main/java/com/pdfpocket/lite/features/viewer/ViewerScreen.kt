package com.pdfpocket.lite.features.viewer

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfpocket.lite.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(onBack: () -> Unit, viewModel: ViewerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { stringResource(R.string.app_name) }, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    IconButton(onClick = { searchOpen = true }, enabled = !state.loading && !state.needsPassword) {
                        Icon(Icons.Default.Search, stringResource(R.string.find_in_document))
                    }
                    IconButton(onClick = viewModel::toggleMode, enabled = !state.loading) {
                        Icon(if (state.continuous) Icons.Default.ViewAgenda else Icons.Default.ViewCarousel, stringResource(R.string.continuous_mode))
                    }
                    IconButton(onClick = viewModel::toggleFavorite, enabled = state.uri != null) {
                        Icon(if (state.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, stringResource(R.string.favorites))
                    }
                    IconButton(onClick = {
                        state.uri?.let { uri ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                        }
                    }, enabled = state.uri != null) {
                        Icon(Icons.Default.Share, stringResource(R.string.share))
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.needsPassword -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.password_required), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.submitPassword(password) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.confirm))
                }
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.corrupted_pdf), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
            }
            state.continuous -> ContinuousViewer(state, viewModel, Modifier.padding(padding))
            else -> SinglePageViewer(state, viewModel, Modifier.padding(padding))
        }
    }

    if (searchOpen) {
        AlertDialog(
            onDismissRequest = { searchOpen = false },
            title = { Text(stringResource(R.string.find_in_document)) },
            text = {
                Column {
                    OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth())
                    if (state.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else state.searchHits.take(12).forEach { hit ->
                        TextButton(onClick = { viewModel.setCurrentPage(hit.pageIndex); searchOpen = false }) {
                            Text("${hit.pageIndex + 1}: ${hit.excerpt}", maxLines = 2)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.search(query) }) { Text(stringResource(R.string.search)) } },
            dismissButton = { TextButton(onClick = { searchOpen = false }) { Text(stringResource(R.string.close)) } }
        )
    }
}

@Composable
private fun ContinuousViewer(state: ViewerState, viewModel: ViewerViewModel, modifier: Modifier) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentPage)
    LaunchedEffect(listState.firstVisibleItemIndex) { viewModel.setCurrentPage(listState.firstVisibleItemIndex) }
    LaunchedEffect(state.currentPage) {
        if (kotlin.math.abs(listState.firstVisibleItemIndex - state.currentPage) > 1) listState.animateScrollToItem(state.currentPage)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items((0 until state.pageCount).toList(), key = { it }) { page ->
            PdfPage(page, state.pageCount, viewModel)
        }
    }
}

@Composable
private fun SinglePageViewer(state: ViewerState, viewModel: ViewerViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            PdfPage(state.currentPage, state.pageCount, viewModel)
        }
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.setCurrentPage(state.currentPage - 1) }, enabled = state.currentPage > 0) {
                Icon(Icons.Default.NavigateBefore, stringResource(R.string.previous_page))
            }
            Text(stringResource(R.string.page_x_of_y, state.currentPage + 1, state.pageCount))
            IconButton(onClick = { viewModel.setCurrentPage(state.currentPage + 1) }, enabled = state.currentPage < state.pageCount - 1) {
                Icon(Icons.Default.NavigateNext, stringResource(R.string.next_page))
            }
        }
    }
}

@Composable
private fun PdfPage(index: Int, pageCount: Int, viewModel: ViewerViewModel) {
    var widthPx by remember { mutableIntStateOf(1080) }
    var bitmap by remember(index, widthPx) { mutableStateOf<Bitmap?>(null) }
    var scale by remember(index) { mutableFloatStateOf(1f) }
    var offsetX by remember(index) { mutableFloatStateOf(0f) }
    var offsetY by remember(index) { mutableFloatStateOf(0f) }

    LaunchedEffect(index, widthPx) { bitmap = runCatching { viewModel.render(index, widthPx) }.getOrNull() }

    Card(Modifier.fillMaxWidth().onSizeChanged { widthPx = it.width.coerceAtLeast(480) }) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 300.dp)
                .pointerInput(index) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                        if (scale == 1f) { offsetX = 0f; offsetY = 0f }
                    }
                }
                .pointerInput(index) {
                    detectTapGestures(onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        if (scale == 1f) { offsetX = 0f; offsetY = 0f }
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { pageBitmap ->
                androidx.compose.foundation.Image(
                    bitmap = pageBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.page_x_of_y, index + 1, pageCount),
                    modifier = Modifier.fillMaxWidth().graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                    contentScale = ContentScale.FillWidth
                )
            } ?: CircularProgressIndicator()
        }
    }
}
