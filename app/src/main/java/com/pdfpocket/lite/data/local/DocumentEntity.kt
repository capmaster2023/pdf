package com.pdfpocket.lite.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val size: Long,
    val pageCount: Int,
    val firstOpenedAt: Long,
    val lastOpenedAt: Long,
    val lastPage: Int,
    val favorite: Boolean
)
