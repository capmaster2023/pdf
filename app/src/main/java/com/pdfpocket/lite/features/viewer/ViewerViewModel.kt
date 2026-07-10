package com.pdfpocket.lite.features.viewer

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pdfpocket.lite.core.AppError
import com.pdfpocket.lite.core.queryMetadata
import com.pdfpocket.lite.data.repository.SettingsRepository
import com.pdfpocket.lite.data.repository.ViewerMode
import com.pdfpocket.lite.domain.DocumentRepository
import com.pdfpocket.lite.pdf.PdfEngine
import com.pdfpocket.lite.pdf.PdfRendererSession
import com.pdfpocket.lite.pdf.PdfSearchHit
import com.pdfpocket.lite.storage.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ViewerState(
    val loading: Boolean = true,
    val uri: Uri? = null,
    val title: String = "",
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val favorite: Boolean = false,
    val continuous: Boolean = true,
    val needsPassword: Boolean = false,
    val error: AppError? = null,
    val searchHits: List<PdfSearchHit> = emptyList(),
    val searching: Boolean = false
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val renderer: PdfRendererSession,
    private val pdfEngine: PdfEngine,
    private val documents: DocumentRepository,
    private val storage: StorageRepository,
    private val settings: SettingsRepository
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ViewerState())
    val state: StateFlow<ViewerState> = _state.asStateFlow()
    private var tempFile: File? = null
    private var activePassword: String? = null
    private val rawUri = savedStateHandle.get<String>("uri").orEmpty()

    init {
        if (rawUri.isNotBlank()) open(Uri.parse(rawUri))
        else _state.value = ViewerState(loading = false, error = AppError.INVALID_URI)
    }

    private fun open(uri: Uri, password: String? = null) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, needsPassword = false)
        runCatching {
            storage.persistPermission(uri)
            tempFile?.delete()

            tempFile = if (password == null) {
                activePassword = null

                withContext(Dispatchers.IO) {
                    storage.copyToCache(uri, "viewer", "pdf")
                }
            } else {
                activePassword = password
                pdfEngine.decryptToTemp(uri, password)
            }

            renderer.openFile(requireNotNull(tempFile))
            val metadata = getApplication<Application>().queryMetadata(uri)
            val old = documents.find(uri.toString())
            documents.record(uri.toString(), metadata.name, metadata.size, renderer.pageCount)
            val preferences = settings.settings.first()
            _state.value = ViewerState(
                loading = false,
                uri = uri,
                title = metadata.name,
                pageCount = renderer.pageCount,
                currentPage = if (preferences.rememberLastPage) old?.lastPage?.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0)) ?: 0 else 0,
                favorite = old?.favorite ?: false,
                continuous = preferences.viewerMode == ViewerMode.CONTINUOUS
            )
        }.onFailure { error ->
            val message = error.message.orEmpty()

            val passwordError =
                error.javaClass.simpleName.contains(
                    "Password",
                    ignoreCase = true
                ) ||
                    message.contains(
                        "password",
                        ignoreCase = true
                    ) ||
                    (
                        error is SecurityException &&
                            message.contains(
                                "cannot create document",
                                ignoreCase = true
                            )
                    )

            _state.value = _state.value.copy(
                loading = false,
                needsPassword = passwordError,
                error = if (passwordError) AppError.PASSWORD_REQUIRED else AppError.CORRUPTED_PDF,
                uri = uri
            )
        }
    }

    fun submitPassword(password: String) {
        val uri = _state.value.uri ?: Uri.parse(rawUri)
        open(uri, password)
    }

    suspend fun render(page: Int, width: Int): Bitmap = renderer.render(page, width)

    fun setCurrentPage(page: Int) {
        val valid = page.coerceIn(0, (_state.value.pageCount - 1).coerceAtLeast(0))
        if (valid == _state.value.currentPage) return
        _state.value = _state.value.copy(currentPage = valid)
        _state.value.uri?.let { uri -> viewModelScope.launch { documents.updateLastPage(uri.toString(), valid) } }
    }

    fun toggleMode() { _state.value = _state.value.copy(continuous = !_state.value.continuous) }

    fun toggleFavorite() {
        val uri = _state.value.uri ?: return
        val value = !_state.value.favorite
        _state.value = _state.value.copy(favorite = value)
        viewModelScope.launch { documents.setFavorite(uri.toString(), value) }
    }

    fun search(query: String) {
        val uri = _state.value.uri ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(searching = true)
            _state.value = runCatching { pdfEngine.search(uri, query, activePassword) }.fold(
                onSuccess = { hits -> _state.value.copy(searching = false, searchHits = hits) },
                onFailure = { _state.value.copy(searching = false, error = AppError.UNKNOWN) }
            )
        }
    }

    override fun onCleared() {
        renderer.close()
        tempFile?.delete()
        super.onCleared()
    }
}
