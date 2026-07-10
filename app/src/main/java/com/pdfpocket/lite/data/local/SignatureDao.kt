package com.pdfpocket.lite.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SignatureDao {
    @Query("SELECT * FROM signatures ORDER BY createdAt DESC") fun observeAll(): Flow<List<SignatureEntity>>
    @Query("SELECT * FROM signatures WHERE id = :id LIMIT 1") suspend fun find(id: Long): SignatureEntity?
    @Insert suspend fun insert(entity: SignatureEntity): Long
    @Delete suspend fun delete(entity: SignatureEntity)
    @Query("DELETE FROM signatures") suspend fun deleteAll()
}
