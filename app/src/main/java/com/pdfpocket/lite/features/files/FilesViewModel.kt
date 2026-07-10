package com.pdfpocket.lite.features.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfpocket.lite.data.local.DocumentEntity
import com.pdfpocket.lite.domain.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FileSort { NAME, DATE, SIZE }

@HiltViewModel
class FilesViewModel @Inject constructor(private val repository: DocumentRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(FileSort.DATE)

    val documents: StateFlow<List<DocumentEntity>> = combine(repository.observeDocuments(), query, sort) { documents, search, order ->
        val filtered = documents.filter { it.name.contains(search, ignoreCase = true) }
        when (order) {
            FileSort.NAME -> filtered.sortedBy { it.name.lowercase() }
            FileSort.DATE -> filtered.sortedByDescending { it.lastOpenedAt }
            FileSort.SIZE -> filtered.sortedByDescending { it.size }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { query.value = value }
    fun setSort(value: FileSort) { sort.value = value }
    fun setFavorite(document: DocumentEntity) = viewModelScope.launch { repository.setFavorite(document.uri, !document.favorite) }
    fun clearHistory() = viewModelScope.launch { repository.clearHistory() }
}
