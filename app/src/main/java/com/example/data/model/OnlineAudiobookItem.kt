package com.example.data.model

data class OnlineAudiobookItem(
    val id: String,
    val title: String,
    val author: String,
    val narrator: String = "LibriVox Volunteers",
    val coverImageUrl: String? = null,
    val audioUrl: String, // MP3 stream URL
    val source: String, // "LibriVox", "Internet Archive", "Loyal Books"
    val description: String = "",
    val durationMs: Long = 0L,
    val category: String = "Public Domain"
)
