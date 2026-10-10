package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val format: String, // PDF, EPUB, TXT, MOBI, FB2
    val category: String,
    val totalPages: Int,
    val currentPage: Int = 1,
    val progressPercent: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val fileSizeBytes: Long = 1024 * 350,
    val coverColorHex: Long = 0xFF059669,
    val coverImageUrl: String? = null,
    val content: String = "",
    val fileUri: String? = null
)
