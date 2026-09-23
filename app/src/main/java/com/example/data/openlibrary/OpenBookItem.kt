package com.example.data.openlibrary

data class OpenBookItem(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val category: String,
    val language: String,
    val format: String, // EPUB, TXT, PDF
    val source: String, // Internet Archive, Project Gutenberg, Standart Ebooks, Open Library
    val description: String,
    val downloadUrl: String? = null,
    val directContent: String? = null,
    val downloadCount: Int = 0,
    val estimatedPages: Int = 120
)
