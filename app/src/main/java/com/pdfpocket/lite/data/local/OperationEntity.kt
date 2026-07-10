package com.pdfpocket.lite.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operations")
data class OperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val sourceName: String,
    val outputName: String,
    val createdAt: Long,
    val successful: Boolean
)
