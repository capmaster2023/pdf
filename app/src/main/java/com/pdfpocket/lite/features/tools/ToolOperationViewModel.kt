package com.pdfpocket.lite.features.tools

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfpocket.lite.core.AppError
import com.pdfpocket.lite.pdf.CompressionLevel
import com.pdfpocket.lite.pdf.PdfEngine
import com.pdfpocket.lite.pdf.PdfFormField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolOperationState(
    val processing: Boolean = false,
    val complete: Boolean = false,
    val error: AppError? = null,
    val formFields: List<PdfFormField> = emptyList(),
    val extractedCount: Int? = null
)

@HiltViewModel
class ToolOperationViewModel @Inject constructor(private val engine: PdfEngine) : ViewModel() {
    private val _state = MutableStateFlow(ToolOperationState())
    val state: StateFlow<ToolOperationState> = _state.asStateFlow()

    fun loadForm(source: Uri, password: String? = null) = launch(
        block = { engine.formFields(source, password) },
        onSuccess = { fields -> _state.value = _state.value.copy(formFields = fields) }
    )

    fun execute(
        operation: String,
        source: Uri?,
        inputs: List<Uri>,
        output: Uri,
        ranges: String,
        text: String,
        password: String,
        confirmPassword: String,
        angle: Int,
        opacity: Float,
        startNumber: Int,
        prefix: String,
        suffix: String,
        level: CompressionLevel,
        formValues: Map<String, String>,
        flatten: Boolean
    ) = launch {
        when (operation) {
            "merge" -> engine.merge(inputs, output)
            "split" -> engine.extract(requireNotNull(source), ranges, output, password.ifBlank { null })
            "rotate" -> engine.rotate(requireNotNull(source), ranges.ifBlank { null }, angle, output, password.ifBlank { null })
            "watermark" -> engine.watermark(requireNotNull(source), text, output, opacity, ranges.ifBlank { null }, password.ifBlank { null })
            "numbers" -> engine.addPageNumbers(requireNotNull(source), output, startNumber, prefix, suffix, password.ifBlank { null })
            "protect" -> {
                require(password == confirmPassword)
                engine.protect(requireNotNull(source), output, password)
            }
            "unlock" -> engine.unlock(requireNotNull(source), output, password)
            "compress" -> engine.compress(requireNotNull(source), output, level)
            "forms" -> engine.fillForm(requireNotNull(source), output, formValues, flatten, password.ifBlank { null })
            else -> error("Unsupported operation")
        }
    }

    fun executeFolder(operation: String, source: Uri, folder: Uri, password: String = "") = launch {
        when (operation) {
            "pdf_to_images" -> engine.pdfToImages(source, folder, Bitmap.CompressFormat.JPEG, 90)
            "extract_images" -> {
                val count = engine.extractEmbeddedImages(source, folder, password.ifBlank { null })
                _state.value = _state.value.copy(extractedCount = count)
            }
            else -> error("Unsupported folder operation")
        }
    }

    private fun <T> launch(block: suspend () -> T, onSuccess: (T) -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(processing = true, complete = false, error = null)
            runCatching { block() }.fold(
                onSuccess = { result -> onSuccess(result); _state.value = _state.value.copy(processing = false, complete = true) },
                onFailure = { _state.value = _state.value.copy(processing = false, error = AppError.UNKNOWN) }
            )
        }
    }

    fun dismissResult() { _state.value = _state.value.copy(complete = false, error = null) }
}
