package com.pdfpocket.lite.features.scanner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfpocket.lite.pdf.ImagePdfCreator
import com.pdfpocket.lite.pdf.ImagePdfOptions
import com.pdfpocket.lite.storage.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

data class ScannerState(
    val pages: List<Uri> = emptyList(),
    val processing: Boolean = false,
    val complete: Boolean = false,
    val error: Boolean = false
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val creator: ImagePdfCreator,
    private val storage: StorageRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    fun newCaptureFile(): File = File(context.cacheDir, "scanner").apply { mkdirs() }
        .let { directory -> File.createTempFile("scan_", ".jpg", directory) }

    fun addCapture(file: File) {
        val uri = storage.fileProviderUri(file)
        _state.value = _state.value.copy(pages = _state.value.pages + uri)
    }

    fun remove(index: Int) {
        _state.value = _state.value.copy(pages = _state.value.pages.toMutableList().apply { if (index in indices) removeAt(index) })
    }

    fun duplicate(index: Int) {
        if (index in _state.value.pages.indices) {
            _state.value = _state.value.copy(pages = _state.value.pages.toMutableList().apply { add(index + 1, this[index]) })
        }
    }

    fun move(index: Int, delta: Int) {
        val list = _state.value.pages.toMutableList()
        val target = index + delta
        if (index in list.indices && target in list.indices) {
            list.add(target, list.removeAt(index))
            _state.value = _state.value.copy(pages = list)
        }
    }

    fun create(output: OutputStream) = viewModelScope.launch {
        _state.value = _state.value.copy(processing = true, complete = false, error = false)
        runCatching { output.use { creator.create(_state.value.pages, it, ImagePdfOptions()) } }.fold(
            onSuccess = { _state.value = _state.value.copy(processing = false, complete = true) },
            onFailure = { _state.value = _state.value.copy(processing = false, error = true) }
        )
    }

    fun dismiss() { _state.value = _state.value.copy(complete = false, error = false) }
}
