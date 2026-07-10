package com.pdfpocket.lite.features.tools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfpocket.lite.pdf.ImagePdfCreator
import com.pdfpocket.lite.pdf.ImagePdfOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import javax.inject.Inject

data class ImagesState(
    val images: List<Uri> = emptyList(),
    val processing: Boolean = false,
    val complete: Boolean = false,
    val error: Boolean = false
)

@HiltViewModel
class ImagesToPdfViewModel @Inject constructor(private val creator: ImagePdfCreator) : ViewModel() {
    private val _state = MutableStateFlow(ImagesState())
    val state: StateFlow<ImagesState> = _state.asStateFlow()

    fun add(uris: List<Uri>) { _state.value = _state.value.copy(images = (_state.value.images + uris).distinct()) }
    fun remove(index: Int) { _state.value = _state.value.copy(images = _state.value.images.toMutableList().apply { if (index in indices) removeAt(index) }) }
    fun move(index: Int, delta: Int) {
        val list = _state.value.images.toMutableList()
        val target = index + delta
        if (index in list.indices && target in list.indices) {
            list.add(target, list.removeAt(index))
            _state.value = _state.value.copy(images = list)
        }
    }

    fun create(output: OutputStream, options: ImagePdfOptions) = viewModelScope.launch {
        _state.value = _state.value.copy(processing = true, complete = false, error = false)
        runCatching { output.use { creator.create(_state.value.images, it, options) } }.fold(
            onSuccess = { _state.value = _state.value.copy(processing = false, complete = true) },
            onFailure = { _state.value = _state.value.copy(processing = false, error = true) }
        )
    }

    fun dismiss() { _state.value = _state.value.copy(complete = false, error = false) }
}
