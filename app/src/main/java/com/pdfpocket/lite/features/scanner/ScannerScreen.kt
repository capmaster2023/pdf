package com.pdfpocket.lite.features.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfpocket.lite.R
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(onBack: () -> Unit, viewModel: ScannerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var flash by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val outputPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { selected -> context.contentResolver.openOutputStream(selected, "w")?.let(viewModel::create) }
    }

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }
    LaunchedEffect(granted, previewView) {
        val view = previewView
        if (granted && view != null) {
            val provider = awaitCameraProvider(context)
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.scan_document)) }, navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }) }
    ) { padding ->
        if (!granted) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.camera_permission_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.camera_permission_explanation))
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).also { view -> view.scaleType = PreviewView.ScaleType.FILL_CENTER; previewView = view } },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = {
                        flash = !flash
                        imageCapture.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                    }) { Icon(if (flash) Icons.Default.FlashOn else Icons.Default.FlashOff, stringResource(R.string.flash)) }

                    FilledIconButton(onClick = {
                        val file = viewModel.newCaptureFile()
                        val options = ImageCapture.OutputFileOptions.Builder(file).build()
                        imageCapture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) { viewModel.addCapture(file) }
                            override fun onError(exception: ImageCaptureException) { file.delete() }
                        })
                    }) { Icon(Icons.Default.Camera, stringResource(R.string.capture_page)) }

                    Button(
                        onClick = { outputPicker.launch("scan.pdf") },
                        enabled = state.pages.isNotEmpty() && !state.processing
                    ) { Text(stringResource(R.string.finish_scan)) }
                }
                Text(stringResource(R.string.pages_captured, state.pages.size), modifier = Modifier.padding(horizontal = 12.dp))
                LazyRow(contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(state.pages) { index, _ ->
                        Card {
                            Column(Modifier.padding(8.dp)) {
                                Text((index + 1).toString())
                                Row {
                                    IconButton(onClick = { viewModel.move(index, -1) }, enabled = index > 0) { Icon(Icons.Default.ArrowBack, stringResource(R.string.move_up)) }
                                    IconButton(onClick = { viewModel.duplicate(index) }) { Icon(Icons.Default.ContentCopy, stringResource(R.string.duplicate)) }
                                    IconButton(onClick = { viewModel.remove(index) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                                }
                            }
                        }
                    }
                }
                if (state.processing) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }

    if (state.complete) ScannerResultDialog(true, viewModel::dismiss)
    if (state.error) ScannerResultDialog(false, viewModel::dismiss)
}

private suspend fun awaitCameraProvider(context: android.content.Context): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener(
        { runCatching { future.get() }.fold(
            onSuccess = { if (continuation.isActive) continuation.resume(it) },
            onFailure = { if (continuation.isActive) continuation.cancel(it) }
        ) },
        ContextCompat.getMainExecutor(context)
    )
}

@Composable
private fun ScannerResultDialog(success: Boolean, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(if (success) R.string.operation_complete else R.string.operation_failed)) },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) } }
    )
}
