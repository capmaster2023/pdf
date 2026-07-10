package com.pdfpocket.lite.features.signature

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfpocket.lite.R
import com.pdfpocket.lite.data.local.SignatureEntity
import com.pdfpocket.lite.data.repository.SignatureRepository
import com.pdfpocket.lite.pdf.PdfEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class SignatureState(
    val signatures: List<SignatureEntity> = emptyList(),
    val selectedId: Long? = null,
    val processing: Boolean = false,
    val complete: Boolean = false,
    val error: Boolean = false
)

@HiltViewModel
class SignatureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SignatureRepository,
    private val engine: PdfEngine
) : ViewModel() {
    private val selected = MutableStateFlow<Long?>(null)
    private val status = MutableStateFlow(SignatureState())

    val state: StateFlow<SignatureState> = combine(repository.observe(), selected, status) { signatures, id, current ->
        current.copy(signatures = signatures, selectedId = id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SignatureState())

    fun select(id: Long) { selected.value = id }

    fun save(name: String, png: ByteArray) = viewModelScope.launch {
        runCatching { repository.save(name.ifBlank { context.getString(R.string.signature) }, png) }
            .onSuccess { selected.value = it }
    }

    fun import(name: String, uri: Uri) = viewModelScope.launch {
        context.contentResolver.openInputStream(uri)?.use { input ->
            selected.value = repository.save(name.ifBlank { context.getString(R.string.signature) }, input.readBytes())
        }
    }

    fun saveText(name: String, text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        val bitmap = Bitmap.createBitmap(1200, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 120f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        canvas.drawText(text.take(40), 30f, 190f, paint)
        val bytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
        selected.value = repository.save(name.ifBlank { text.take(40) }, bytes)
    }

    fun delete(entity: SignatureEntity) = viewModelScope.launch {
        repository.delete(entity)
        if (selected.value == entity.id) selected.value = null
    }

    fun stamp(source: Uri, output: Uri, page: Int, x: Float, y: Float, width: Float, opacity: Float) = viewModelScope.launch {
        val id = selected.value ?: return@launch
        status.value = status.value.copy(processing = true, complete = false, error = false)
        runCatching { engine.stampSignature(source, output, repository.bytes(id), page - 1, x, y, width, opacity) }.fold(
            onSuccess = { status.value = status.value.copy(processing = false, complete = true) },
            onFailure = { status.value = status.value.copy(processing = false, error = true) }
        )
    }

    fun dismiss() { status.value = status.value.copy(complete = false, error = false) }
}
