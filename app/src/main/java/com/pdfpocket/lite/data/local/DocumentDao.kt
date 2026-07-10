package com.pdfpocket.lite.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY lastOpenedAt DESC") fun observeAll(): Flow<List<DocumentEntity>>
    @Query("SELECT * FROM documents WHERE favorite = 1 ORDER BY lastOpenedAt DESC") fun observeFavorites(): Flow<List<DocumentEntity>>
    @Query("SELECT * FROM documents WHERE uri = :uri LIMIT 1") suspend fun find(uri: String): DocumentEntity?
    @Upsert suspend fun upsert(entity: DocumentEntity)
    @Query("UPDATE documents SET favorite = :favorite WHERE uri = :uri") suspend fun setFavorite(uri: String, favorite: Boolean)
    @Query("UPDATE documents SET lastPage = :page, lastOpenedAt = :openedAt WHERE uri = :uri") suspend fun updateProgress(uri: String, page: Int, openedAt: Long)
    @Query("DELETE FROM documents") suspend fun clearHistory()
}
