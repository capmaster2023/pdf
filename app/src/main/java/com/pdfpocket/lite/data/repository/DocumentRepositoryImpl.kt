package com.pdfpocket.lite.data.repository

import com.pdfpocket.lite.data.local.DocumentDao
import com.pdfpocket.lite.data.local.DocumentEntity
import com.pdfpocket.lite.domain.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(private val dao: DocumentDao) : DocumentRepository {
    override fun observeDocuments(): Flow<List<DocumentEntity>> = dao.observeAll()
    override fun observeFavorites(): Flow<List<DocumentEntity>> = dao.observeFavorites()
    override suspend fun find(uri: String): DocumentEntity? = dao.find(uri)
    override suspend fun record(uri: String, name: String, size: Long, pageCount: Int) {
        val old = dao.find(uri)
        val now = System.currentTimeMillis()
        dao.upsert(DocumentEntity(uri, name, size, pageCount, old?.firstOpenedAt ?: now, now, old?.lastPage ?: 0, old?.favorite ?: false))
    }
    override suspend fun setFavorite(uri: String, favorite: Boolean) = dao.setFavorite(uri, favorite)
    override suspend fun updateLastPage(uri: String, page: Int) = dao.updateProgress(uri, page, System.currentTimeMillis())
    override suspend fun clearHistory() = dao.clearHistory()
}
