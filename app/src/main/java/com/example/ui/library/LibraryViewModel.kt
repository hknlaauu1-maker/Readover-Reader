package com.example.ui.library

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cover.OpenCoverFetcher
import com.example.data.database.ReadoverDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.openlibrary.OpenBookItem
import com.example.data.openlibrary.OpenLibraryService
import com.example.data.repository.ReadoverRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BookSortOrder(val label: String) {
    LAST_READ("Son Okunan"),
    TITLE("İsim (A-Z)"),
    PROGRESS("İlerleme")
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ReadoverDatabase.getDatabase(application)
    private val repository = ReadoverRepository(
        database.bookDao(),
        database.bookmarkDao(),
        database.audiobookDao(),
        application
    )

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    var selectedTab by mutableIntStateOf(0) // 0: Kitaplık, 1: S. Kitap, 2: Son Okunan, 3: Yer İmleri, 4: İstatistik
    var searchQuery by mutableStateOf("")
    var selectedFormatFilter by mutableStateOf("TÜMÜ")
    var sortOrder by mutableStateOf(BookSortOrder.LAST_READ)
    var isGridView by mutableStateOf(true)

    // Open Library State
    var isOpenLibraryDialogOpen by mutableStateOf(false)
    var openLibrarySearchQuery by mutableStateOf("")
    var openLibraryCategoryFilter by mutableStateOf("Tümü")
    var isOpenLibrarySearching by mutableStateOf(false)
    var openLibraryBooks by mutableStateOf<List<OpenBookItem>>(OpenLibraryService.curatedCatalog)
    var downloadingBookIds by mutableStateOf<Set<String>>(emptySet())
    var downloadedBookIds by mutableStateOf<Set<String>>(emptySet())

    fun searchOpenLibrary(query: String) {
        openLibrarySearchQuery = query
        viewModelScope.launch {
            isOpenLibrarySearching = true
            try {
                if (query.isBlank()) {
                    openLibraryBooks = OpenLibraryService.curatedCatalog
                } else {
                    val results = OpenLibraryService.searchOpenBooks(query)
                    openLibraryBooks = results
                }
            } catch (e: Exception) {
                openLibraryBooks = OpenLibraryService.curatedCatalog
            } finally {
                isOpenLibrarySearching = false
            }
        }
    }

    fun downloadAndAddOpenBook(item: OpenBookItem, onDone: (Long) -> Unit) {
        if (downloadingBookIds.contains(item.id)) return
        downloadingBookIds = downloadingBookIds + item.id

        viewModelScope.launch {
            try {
                val newBookId = repository.downloadAndInsertOpenBook(item)
                downloadedBookIds = downloadedBookIds + item.id
                onDone(newBookId)
            } catch (e: Exception) {
                // handle error
            } finally {
                downloadingBookIds = downloadingBookIds - item.id
            }
        }
    }

    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentBooks: StateFlow<List<BookEntity>> = repository.recentBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteBooks: StateFlow<List<BookEntity>> = repository.favoriteBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered books flow
    val filteredBooks: StateFlow<List<BookEntity>> = allBooks.map { books ->
        var list = books

        // Format filter
        if (selectedFormatFilter != "TÜMÜ") {
            list = list.filter { it.format.equals(selectedFormatFilter, ignoreCase = true) }
        }

        // Search query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }

        // Sorting
        when (sortOrder) {
            BookSortOrder.LAST_READ -> list.sortedByDescending { it.lastReadTimestamp }
            BookSortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
            BookSortOrder.PROGRESS -> list.sortedByDescending { it.progressPercent }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importBookFile(uri: Uri, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.importFileFromUri(uri)
            onComplete(id)
        }
    }

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id, !book.isFavorite)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    fun fetchAndApplyCoverForBook(book: BookEntity) {
        viewModelScope.launch {
            val coverUrl = OpenCoverFetcher.fetchCoverUrl(book.title, book.author)
            if (coverUrl != null) {
                database.bookDao().updateCoverImageUrl(book.id, coverUrl)
            }
        }
    }
}
