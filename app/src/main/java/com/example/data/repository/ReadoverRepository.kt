package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.cover.OpenCoverFetcher
import com.example.data.dao.AudiobookDao
import com.example.data.dao.BookDao
import com.example.data.dao.BookmarkDao
import com.example.data.model.AudioBookmarkEntity
import com.example.data.model.AudiobookEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.openlibrary.OpenBookItem
import com.example.data.openlibrary.OpenLibraryService
import com.example.data.sample.SampleAudiobooks
import com.example.data.sample.SampleBooks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ReadoverRepository(
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val audiobookDao: AudiobookDao,
    private val context: Context
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Books
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val recentBooks: Flow<List<BookEntity>> = bookDao.getRecentBooks()
    val favoriteBooks: Flow<List<BookEntity>> = bookDao.getFavoriteBooks()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    // Audiobooks
    val allAudiobooks: Flow<List<AudiobookEntity>> = audiobookDao.getAllAudiobooks()
    val favoriteAudiobooks: Flow<List<AudiobookEntity>> = audiobookDao.getFavoriteAudiobooks()

    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        bookDao.deleteSampleBooks()
        audiobookDao.deleteSampleAudiobooks()
    }

    // -------------------------------------------------------------
    // BOOK OPERATIONS
    // -------------------------------------------------------------
    fun getBookById(id: Long): Flow<BookEntity?> = bookDao.getBookById(id)

    suspend fun getBookByIdSync(id: Long): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookByIdSync(id)
    }

    suspend fun updateReadingProgress(id: Long, page: Int, progress: Float) = withContext(Dispatchers.IO) {
        bookDao.updateReadingProgress(id, page, progress, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        bookDao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.deleteBook(book)
    }

    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksForBook(bookId)

    suspend fun addBookmark(
        bookId: Long,
        bookTitle: String,
        pageNumber: Int,
        chapterTitle: String,
        quote: String,
        note: String,
        colorHex: Long = 0xFFFFD54F
    ) = withContext(Dispatchers.IO) {
        val bookmark = BookmarkEntity(
            bookId = bookId,
            bookTitle = bookTitle,
            pageNumber = pageNumber,
            chapterTitle = chapterTitle,
            quote = quote,
            userNote = note,
            colorHex = colorHex,
            createdAt = System.currentTimeMillis()
        )
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun importFileFromUri(uri: Uri): Long = withContext(Dispatchers.IO) {
        var fileName = "Bilinmeyen Belge"
        var fileSize = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex) ?: "Belge"
                }
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        val extension = fileName.substringAfterLast('.', "").uppercase()
        val format = when (extension) {
            "PDF" -> "PDF"
            "EPUB" -> "EPUB"
            "MOBI" -> "MOBI"
            "FB2" -> "FB2"
            "DOC", "DOCX" -> "DOCX"
            "HTML", "HTM" -> "HTML"
            else -> "TXT"
        }

        val textContent = com.example.util.DocumentExtractor.extractText(context, uri, fileName)
        val embeddedCoverUrl = com.example.util.DocumentExtractor.extractEmbeddedCover(context, uri, fileName)

        val estimatedPages = (textContent.length / 1200).coerceAtLeast(1)

        val cleanTitle = fileName.substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()

        val newBook = BookEntity(
            title = cleanTitle.ifBlank { "İçe Aktarılan Belge" },
            author = "Yerel Belge",
            format = format,
            category = "İçe Aktarılan",
            coverImageUrl = embeddedCoverUrl,
            totalPages = estimatedPages,
            currentPage = 1,
            progressPercent = 0f,
            lastReadTimestamp = System.currentTimeMillis(),
            isFavorite = false,
            fileSizeBytes = if (fileSize > 0) fileSize else textContent.length.toLong(),
            coverColorHex = 0xFF0D9488,
            content = textContent,
            fileUri = uri.toString()
        )

        val insertedId = bookDao.insertBook(newBook)

        // If no embedded cover was found, automatically fetch open source cover image from open libraries
        if (embeddedCoverUrl == null) {
            repositoryScope.launch {
                try {
                    val coverUrl = OpenCoverFetcher.fetchCoverUrl(cleanTitle, null)
                    if (coverUrl != null) {
                        bookDao.updateCoverImageUrl(insertedId, coverUrl)
                    }
                } catch (e: Exception) {
                    Log.e("ReadoverRepo", "Failed to auto fetch cover for book: ${e.message}")
                }
            }
        }

        insertedId
    }

    suspend fun importFilesFromUris(uris: List<Uri>): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        for (uri in uris) {
            try {
                val id = importFileFromUri(uri)
                ids.add(id)
            } catch (e: Exception) {
                Log.e("ReadoverRepo", "Error importing uri $uri: ${e.message}")
            }
        }
        ids
    }

    suspend fun importFolderTree(treeUri: Uri): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        try {
            val rootDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null && rootDoc.isDirectory) {
                val supportedExtensions = setOf("pdf", "epub", "mobi", "fb2", "txt", "doc", "docx", "htm", "html")
                fun traverse(doc: androidx.documentfile.provider.DocumentFile) {
                    val files = doc.listFiles()
                    for (file in files) {
                        if (file.isDirectory) {
                            traverse(file)
                        } else if (file.isFile) {
                            val name = file.name?.lowercase() ?: ""
                            val ext = name.substringAfterLast('.', "")
                            if (supportedExtensions.contains(ext)) {
                                try {
                                    val id = kotlinx.coroutines.runBlocking { importFileFromUri(file.uri) }
                                    ids.add(id)
                                } catch (e: Exception) {
                                    Log.e("ReadoverRepo", "Failed to import folder file: ${file.name}")
                                }
                            }
                        }
                    }
                }
                traverse(rootDoc)
            }
        } catch (e: Exception) {
            Log.e("ReadoverRepo", "Error traversing folder tree: ${e.message}")
        }
        ids
    }

    suspend fun downloadAndInsertOpenBook(item: OpenBookItem): Long = withContext(Dispatchers.IO) {
        var textContent = item.directContent

        // If directContent is not set or empty, try downloading from downloadUrl
        if (textContent.isNullOrBlank() && !item.downloadUrl.isNullOrBlank()) {
            val downloaded = OpenLibraryService.downloadBookText(item.downloadUrl)
            if (!downloaded.isNullOrBlank()) {
                textContent = downloaded
            }
        }

        val finalText = textContent?.ifBlank { null }
            ?: "Kitap: ${item.title}\nYazar: ${item.author}\nKaynak: ${item.source}\n\n${item.description}\n\nBu açık kaynak eser Readover kütüphanesine başarıyla eklendi."

        val estimatedPages = (finalText.length / 1100).coerceAtLeast(1)

        val newBook = BookEntity(
            title = item.title,
            author = item.author,
            format = item.format,
            category = item.category,
            coverImageUrl = item.coverUrl,
            totalPages = estimatedPages,
            currentPage = 1,
            progressPercent = 0f,
            lastReadTimestamp = System.currentTimeMillis(),
            isFavorite = false,
            fileSizeBytes = finalText.length.toLong(),
            coverColorHex = 0xFF0284C7,
            content = finalText,
            fileUri = item.downloadUrl
        )

        val insertedId = bookDao.insertBook(newBook)

        // If cover was not present, try fetching it
        if (item.coverUrl == null) {
            repositoryScope.launch {
                try {
                    val cover = OpenCoverFetcher.fetchCoverUrl(item.title, item.author)
                    if (cover != null) {
                        bookDao.updateCoverImageUrl(insertedId, cover)
                    }
                } catch (e: Exception) {
                    Log.e("ReadoverRepo", "Cover fetch failed: ${e.message}")
                }
            }
        }

        insertedId
    }

    // -------------------------------------------------------------
    // AUDIOBOOK OPERATIONS
    // -------------------------------------------------------------
    fun getAudiobookById(id: Long): Flow<AudiobookEntity?> = audiobookDao.getAudiobookById(id)

    suspend fun getAudiobookByIdSync(id: Long): AudiobookEntity? = withContext(Dispatchers.IO) {
        audiobookDao.getAudiobookByIdSync(id)
    }

    suspend fun updateAudioProgress(id: Long, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        audiobookDao.updatePlaybackProgress(id, positionMs, durationMs, System.currentTimeMillis())
    }

    suspend fun toggleFavoriteAudiobook(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        audiobookDao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteAudiobook(audiobook: AudiobookEntity) = withContext(Dispatchers.IO) {
        audiobookDao.deleteAudiobook(audiobook)
    }

    fun getAudioBookmarks(audiobookId: Long): Flow<List<AudioBookmarkEntity>> =
        audiobookDao.getBookmarksForAudiobook(audiobookId)

    suspend fun addAudioBookmark(audiobookId: Long, timestampMs: Long, title: String, note: String) = withContext(Dispatchers.IO) {
        audiobookDao.insertAudioBookmark(
            AudioBookmarkEntity(
                audiobookId = audiobookId,
                timestampMs = timestampMs,
                title = title,
                note = note
            )
        )
    }

    suspend fun deleteAudioBookmark(bookmark: AudioBookmarkEntity) = withContext(Dispatchers.IO) {
        audiobookDao.deleteAudioBookmark(bookmark)
    }

    // Import Local Audio File (MP3, M4A, AAC, WAV, etc.)
    suspend fun importAudioFile(uri: Uri): Long = withContext(Dispatchers.IO) {
        var fileName = "Ses Dosyası"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex) ?: "Ses Dosyası"
                }
            }
        }

        var detectedTitle = fileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
        var detectedArtist = "Yerel Seslendirme"
        var durationMs = 0L

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
            val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            if (!metaTitle.isNullOrBlank()) detectedTitle = metaTitle.trim()
            if (!metaArtist.isNullOrBlank()) detectedArtist = metaArtist.trim()
            if (!metaDuration.isNullOrBlank()) durationMs = metaDuration.toLongOrNull() ?: 0L

            retriever.release()
        } catch (e: Exception) {
            Log.e("ReadoverRepo", "MediaMetadataRetriever error: ${e.message}")
        }

        val newAudiobook = AudiobookEntity(
            title = detectedTitle.ifBlank { "İçe Aktarılan Sesli Kitap" },
            author = detectedArtist,
            narrator = detectedArtist,
            coverColorHex = 0xFF4F46E5,
            audioSourceType = "LOCAL_FILE",
            audioUriOrUrl = uri.toString(),
            durationMs = durationMs,
            currentPositionMs = 0L,
            isFavorite = false,
            category = "Yerel Dosya",
            description = "Cihazınızdan aktarılan sesli kitap kaydı.",
            lastListenedTimestamp = System.currentTimeMillis()
        )

        val insertedId = audiobookDao.insertAudiobook(newAudiobook)

        // Auto fetch open-source cover art based on extracted title
        repositoryScope.launch {
            try {
                val coverUrl = OpenCoverFetcher.fetchCoverUrl(detectedTitle, detectedArtist)
                if (coverUrl != null) {
                    audiobookDao.updateCoverImageUrl(insertedId, coverUrl)
                }
            } catch (e: Exception) {
                Log.e("ReadoverRepo", "Auto audio cover fetch error: ${e.message}")
            }
        }

        insertedId
    }

    // Add Web Stream or YouTube link
    suspend fun addWebOrYoutubeAudio(
        inputUrl: String,
        customTitle: String? = null,
        customAuthor: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val trimmedUrl = inputUrl.trim()
        val isYouTube = trimmedUrl.contains("youtube.com") || trimmedUrl.contains("youtu.be")

        var title = customTitle?.trim()?.ifBlank { null }
        var author = customAuthor?.trim()?.ifBlank { null } ?: if (isYouTube) "YouTube İçeriği" else "Çevrimiçi Ses Akışı"
        var coverUrl: String? = null
        var resolvedUrl = trimmedUrl

        if (isYouTube) {
            // Extract YouTube video id if possible
            val videoId = extractYouTubeVideoId(trimmedUrl)
            if (title == null) {
                title = if (videoId != null) "YouTube Sesli Kitap (#$videoId)" else "YouTube Ses Kaydı"
            }
            if (videoId != null) {
                // YouTube HD thumbnail as cover
                coverUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            }
        } else {
            if (title == null) {
                val lastSegment = trimmedUrl.substringAfterLast('/').substringBefore('?')
                title = if (lastSegment.isNotBlank()) {
                    lastSegment.substringBeforeLast('.').replace('_', ' ').replace('-', ' ')
                } else {
                    "Çevrimiçi Sesli Kitap"
                }
            }
        }

        val audiobook = AudiobookEntity(
            title = title ?: "Sesli Kitap",
            author = author,
            narrator = if (isYouTube) "YouTube Audio" else "Web Stream",
            coverImageUrl = coverUrl,
            coverColorHex = if (isYouTube) 0xFFDC2626 else 0xFF2563EB,
            audioSourceType = if (isYouTube) "YOUTUBE" else "WEB_STREAM",
            audioUriOrUrl = resolvedUrl,
            durationMs = 0L, // will be resolved when player loads
            currentPositionMs = 0L,
            isFavorite = false,
            category = if (isYouTube) "YouTube" else "İnternet Akışı",
            description = if (isYouTube) "YouTube üzerinden eklenen sesli kitap bağlantısı ($trimmedUrl)" else "İnternet üzerinden eklenen ses akışı ($trimmedUrl)",
            lastListenedTimestamp = System.currentTimeMillis()
        )

        val insertedId = audiobookDao.insertAudiobook(audiobook)

        // If cover was not already found, query open library
        if (coverUrl == null) {
            repositoryScope.launch {
                try {
                    val foundCover = OpenCoverFetcher.fetchCoverUrl(title ?: "", author)
                    if (foundCover != null) {
                        audiobookDao.updateCoverImageUrl(insertedId, foundCover)
                    }
                } catch (e: Exception) {
                    Log.e("ReadoverRepo", "Auto cover fetch error: ${e.message}")
                }
            }
        }

        insertedId
    }

    private fun extractYouTubeVideoId(url: String): String? {
        return try {
            if (url.contains("youtu.be/")) {
                url.substringAfter("youtu.be/").substringBefore('?').substringBefore('/')
            } else if (url.contains("v=")) {
                url.substringAfter("v=").substringBefore('&').substringBefore('?')
            } else if (url.contains("/embed/")) {
                url.substringAfter("/embed/").substringBefore('?').substringBefore('/')
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
