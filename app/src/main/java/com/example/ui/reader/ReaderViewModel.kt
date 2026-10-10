package com.example.ui.reader

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ReadoverDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.repository.ReadoverRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReadoverRepository

    init {
        val db = ReadoverDatabase.getDatabase(application)
        repository = ReadoverRepository(db.bookDao(), db.bookmarkDao(), db.audiobookDao(), application)
    }

    private val _currentBook = MutableStateFlow<BookEntity?>(null)
    val currentBook: StateFlow<BookEntity?> = _currentBook.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    var pagination by mutableStateOf(BookPagination(emptyList(), emptyList()))
        private set

    var currentPageIndex by mutableIntStateOf(0) // 0-based
        private set

    // Reader UI appearance states & Theme Management
    var currentTheme by mutableStateOf(ReadingThemeMode.SEPIA)
    var fontSizeSp by mutableFloatStateOf(18f)
    var lineHeightMultiplier by mutableFloatStateOf(1.5f)
    var selectedFontFamily by mutableStateOf(ReaderFontFamily.SERIF)
    var textAlign by mutableStateOf(TextAlign.Justify)
    var brightnessLevel by mutableFloatStateOf(1.0f) // 0.1 to 1.0

    // Eye comfort blue light filter overlay
    var eyeComfortEnabled by mutableStateOf(false)
    var eyeComfortWarmth by mutableFloatStateOf(0.35f)

    // Fullscreen UI controls toggle (hide bars for immersive reading)
    var showControls by mutableStateOf(true)

    // Dialogs / Sheets / Modals
    var showFontSettingsSheet by mutableStateOf(false)
    var showTocSheet by mutableStateOf(false)
    var showBookmarkDialog by mutableStateOf(false)
    var showSpeedReaderModal by mutableStateOf(false)
    var showTtsBar by mutableStateOf(false)
    var showPageProgressModal by mutableStateOf(false)

    // Speed Reader (RSVP) state
    var rsvpWords by mutableStateOf<List<String>>(emptyList())
    var rsvpCurrentIndex by mutableIntStateOf(0)
    var rsvpIsPlaying by mutableStateOf(false)
    var rsvpWpm by mutableIntStateOf(300)
    private var rsvpJob: Job? = null

    private fun getPdfPageCount(fileUriString: String?): Int {
        if (fileUriString.isNullOrBlank()) return 0
        return try {
            val uri = android.net.Uri.parse(fileUriString)
            val context = getApplication<Application>()
            val pfd = if (uri.scheme == "file") {
                val path = uri.path ?: ""
                val file = java.io.File(path)
                if (file.exists()) android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY) else null
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
            pfd?.use { descriptor ->
                android.graphics.pdf.PdfRenderer(descriptor).use { renderer ->
                    renderer.pageCount
                }
            } ?: 0
        } catch (e: Exception) {
            android.util.Log.e("ReaderViewModel", "Error getting PDF real page count: ${e.message}")
            0
        }
    }

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            val book = repository.getBookByIdSync(bookId)
            _currentBook.value = book
            if (book != null) {
                var parsed = BookReaderEngine.paginate(book.content)

                // If PDF book, derive pagination from real PDF file page count
                if (book.format.equals("PDF", ignoreCase = true) && !book.fileUri.isNullOrBlank()) {
                    val realPdfPages = getPdfPageCount(book.fileUri)
                    if (realPdfPages > 0) {
                        val pdfPageList = List(realPdfPages) { idx -> "PDF Sayfa ${idx + 1}" }
                        val chaptersList = listOf(ChapterInfo(0, "Tüm Belge", 1))
                        parsed = BookPagination(pages = pdfPageList, chapters = chaptersList)
                    }
                }

                pagination = parsed

                // Restore previous page (1-based to 0-based)
                val initialPage = (book.currentPage - 1).coerceIn(0, (parsed.pages.size - 1).coerceAtLeast(0))
                currentPageIndex = initialPage

                // Observe bookmarks
                repository.getBookmarksForBook(bookId).collect {
                    _bookmarks.value = it
                }
            }
        }
    }

    fun goToPage(index: Int) {
        val validIndex = index.coerceIn(0, (pagination.pages.size - 1).coerceAtLeast(0))
        currentPageIndex = validIndex

        // Persist progress to DB
        val total = pagination.pages.size.coerceAtLeast(1)
        val progress = (validIndex + 1).toFloat() / total.toFloat()
        _currentBook.value?.let { book ->
            viewModelScope.launch {
                repository.updateReadingProgress(book.id, validIndex + 1, progress)
            }
        }
    }

    fun goToPercent(percent: Float) {
        val total = pagination.pages.size.coerceAtLeast(1)
        val targetPage = (percent * total).toInt().coerceIn(0, total - 1)
        goToPage(targetPage)
    }

    fun saveCurrentPageAndProgress(onSaved: (Int, Float) -> Unit) {
        val book = _currentBook.value ?: return
        val pageNum = currentPageIndex + 1
        val total = pagination.pages.size.coerceAtLeast(1)
        val progress = pageNum.toFloat() / total.toFloat()

        viewModelScope.launch {
            repository.updateReadingProgress(book.id, pageNum, progress)
            _currentBook.value = book.copy(
                currentPage = pageNum,
                progressPercent = progress,
                lastReadTimestamp = System.currentTimeMillis()
            )
            onSaved(pageNum, progress)
        }
    }

    fun nextPage() {
        if (currentPageIndex + 1 < pagination.pages.size) {
            goToPage(currentPageIndex + 1)
        }
    }

    fun prevPage() {
        if (currentPageIndex > 0) {
            goToPage(currentPageIndex - 1)
        }
    }

    fun toggleControls() {
        showControls = !showControls
    }

    fun toggleFavorite() {
        val book = _currentBook.value ?: return
        val newFav = !book.isFavorite
        _currentBook.value = book.copy(isFavorite = newFav)
        viewModelScope.launch {
            repository.toggleFavorite(book.id, newFav)
        }
    }

    fun addBookmark(quote: String, note: String, colorHex: Long) {
        val book = _currentBook.value ?: return
        val currentChapter = getCurrentChapterTitle()
        viewModelScope.launch {
            repository.addBookmark(
                bookId = book.id,
                bookTitle = book.title,
                pageNumber = currentPageIndex + 1,
                chapterTitle = currentChapter,
                quote = quote.ifBlank { getCurrentPageText().take(120) + "..." },
                note = note,
                colorHex = colorHex
            )
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    fun getCurrentPageText(): String {
        return if (currentPageIndex in pagination.pages.indices) {
            pagination.pages[currentPageIndex]
        } else {
            ""
        }
    }

    fun getCurrentChapterTitle(): String {
        val pageNum = currentPageIndex + 1
        val chapters = pagination.chapters
        if (chapters.isEmpty()) return "Bölüm"

        val found = chapters.lastOrNull { it.startPage <= pageNum }
        return found?.title ?: chapters.first().title
    }

    // -------------------------------------------------------------
    // SPEED READER (RSVP) METHODS
    // -------------------------------------------------------------
    fun openSpeedReader() {
        val text = getCurrentPageText()
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        rsvpWords = words
        rsvpCurrentIndex = 0
        rsvpIsPlaying = false
        showSpeedReaderModal = true
    }

    fun startRsvp() {
        if (rsvpWords.isEmpty()) return
        rsvpIsPlaying = true
        rsvpJob?.cancel()
        rsvpJob = viewModelScope.launch {
            while (rsvpIsPlaying && rsvpCurrentIndex < rsvpWords.size) {
                val delayMs = (60000 / rsvpWpm).toLong().coerceAtLeast(80)
                delay(delayMs)
                if (rsvpCurrentIndex + 1 < rsvpWords.size) {
                    rsvpCurrentIndex++
                } else {
                    rsvpIsPlaying = false
                }
            }
        }
    }

    fun pauseRsvp() {
        rsvpIsPlaying = false
        rsvpJob?.cancel()
    }

    fun toggleRsvp() {
        if (rsvpIsPlaying) pauseRsvp() else startRsvp()
    }

    fun closeSpeedReader() {
        pauseRsvp()
        showSpeedReaderModal = false
    }

    override fun onCleared() {
        super.onCleared()
        rsvpJob?.cancel()
    }
}
