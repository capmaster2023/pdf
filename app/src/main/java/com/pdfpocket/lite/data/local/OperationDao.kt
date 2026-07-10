package com.pdfpocket.lite.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationDao {
    @Query("SELECT * FROM operations ORDER BY createdAt DESC LIMIT 100") fun observeRecent(): Flow<List<OperationEntity>>
    @Insert suspend fun insert(entity: OperationEntity)
    @Query("DELETE FROM operations") suspend fun clear()
}
