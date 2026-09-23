package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val bookTitle: String,
    val pageNumber: Int,
    val chapterTitle: String,
    val quote: String,
    val userNote: String,
    val colorHex: Long = 0xFFFFD54F,
    val createdAt: Long = System.currentTimeMillis()
)
