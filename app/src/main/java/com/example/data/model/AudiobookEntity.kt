package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audiobooks")
data class AudiobookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val narrator: String = "Seslendiren Bilinmiyor",
    val coverImageUrl: String? = null,
    val coverColorHex: Long = 0xFF4F46E5,
    val audioSourceType: String = "LOCAL_FILE", // LOCAL_FILE, WEB_STREAM, YOUTUBE, SAMPLE
    val audioUriOrUrl: String,
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val category: String = "Sesli Kitap",
    val description: String = "",
    val playbackSpeed: Float = 1.0f,
    val lastListenedTimestamp: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
