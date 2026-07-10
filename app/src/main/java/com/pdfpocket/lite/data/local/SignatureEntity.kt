package com.pdfpocket.lite.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val encryptedFileName: String,
    val createdAt: Long
)
