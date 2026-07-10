package com.pdfpocket.lite.domain

import com.pdfpocket.lite.data.local.DocumentEntity
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeDocuments(): Flow<List<DocumentEntity>>
    fun observeFavorites(): Flow<List<DocumentEntity>>
    suspend fun record(uri: String, name: String, size: Long, pageCount: Int)
    suspend fun find(uri: String): DocumentEntity?
    suspend fun setFavorite(uri: String, favorite: Boolean)
    suspend fun updateLastPage(uri: String, page: Int)
    suspend fun clearHistory()
}
