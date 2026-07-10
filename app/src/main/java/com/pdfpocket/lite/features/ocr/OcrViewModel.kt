package com.pdfpocket.lite.features.ocr

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfpocket.lite.ocr.OcrService
import com.pdfpocket.lite.pdf.PdfRendererSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OcrState(val processing: Boolean = false, val text: String = "", val error: Boolean = false)

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val ocr: OcrService,
    private val renderer: PdfRendererSession
) : ViewModel() {
    private val _state = MutableStateFlow(OcrState())
    val state: StateFlow<OcrState> = _state.asStateFlow()

    fun processImage(uri: Uri) = execute { ocr.recognize(uri) }

    fun processPdf(uri: Uri, page: Int) = execute {
        renderer.open(uri)
        try {
            val index = (page - 1).coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
            ocr.recognize(renderer.render(index, 1800))
        } finally {
            renderer.close()
        }
    }

    private fun execute(block: suspend () -> String) = viewModelScope.launch {
        _state.value = OcrState(processing = true)
        runCatching { block() }.fold(
            onSuccess = { _state.value = OcrState(text = it) },
            onFailure = { _state.value = OcrState(error = true) }
        )
    }

    fun edit(value: String) { _state.value = _state.value.copy(text = value) }
}
