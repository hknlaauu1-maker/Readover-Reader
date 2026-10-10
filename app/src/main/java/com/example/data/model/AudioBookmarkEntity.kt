package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_bookmarks")
data class AudioBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val audiobookId: Long,
    val timestampMs: Long,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
