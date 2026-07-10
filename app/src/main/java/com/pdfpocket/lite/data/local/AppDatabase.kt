package com.pdfpocket.lite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DocumentEntity::class, OperationEntity::class, SignatureEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun operationDao(): OperationDao
    abstract fun signatureDao(): SignatureDao
}
