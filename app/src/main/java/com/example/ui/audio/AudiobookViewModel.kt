package com.example.ui.audio

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
import com.example.data.model.AudioBookmarkEntity
import com.example.data.model.AudiobookEntity
import com.example.data.repository.ReadoverRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AudiobookViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ReadoverDatabase.getDatabase(application)
    private val repository = ReadoverRepository(
        database.bookDao(),
        database.bookmarkDao(),
        database.audiobookDao(),
        application
    )

    val playerEngine = AudioPlayerEngine(application)

    val allAudiobooks: StateFlow<List<AudiobookEntity>> = repository.allAudiobooks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val favoriteAudiobooks: StateFlow<List<AudiobookEntity>> = repository.favoriteAudiobooks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    var searchQuery by mutableStateOf("")
    var selectedAudioTab by mutableIntStateOf(0) // 0: Player / Now Playing, 1: Sesli Kitaplık, 2: Favoriler

    val isPlaying: StateFlow<Boolean> = playerEngine.isPlaying
    val currentAudiobook: StateFlow<AudiobookEntity?> = playerEngine.currentAudiobook
    val currentPositionMs: StateFlow<Long> = playerEngine.currentPositionMs
    val durationMs: StateFlow<Long> = playerEngine.durationMs
    val playbackSpeed: StateFlow<Float> = playerEngine.playbackSpeed
    val isLoading: StateFlow<Boolean> = playerEngine.isLoading
    val errorMessage: StateFlow<String?> = playerEngine.errorMessage
    val sleepTimerSecondsLeft: StateFlow<Int?> = playerEngine.sleepTimerSecondsLeft

    val filteredAudiobooks: StateFlow<List<AudiobookEntity>> = allAudiobooks.map { list ->
        if (searchQuery.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.narrator.contains(searchQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audio bookmarks for current playing audiobook
    val currentBookmarks: StateFlow<List<AudioBookmarkEntity>> = currentAudiobook
        .flatMapLatest { book ->
            if (book != null) {
                repository.getAudioBookmarks(book.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }

        // Periodically sync playback progress to Room DB
        playerEngine.onProgressUpdateListener = { currentPos, duration ->
            currentAudiobook.value?.let { book ->
                viewModelScope.launch {
                    repository.updateAudioProgress(book.id, currentPos, duration)
                }
            }
        }
    }

    fun playAudiobook(audiobook: AudiobookEntity) {
        playerEngine.loadAndPlay(audiobook, audiobook.currentPositionMs)
    }

    fun togglePlayPause() {
        playerEngine.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun skipForward(seconds: Int = 15) {
        playerEngine.skipForward(seconds)
    }

    fun skipBackward(seconds: Int = 15) {
        playerEngine.skipBackward(seconds)
    }

    fun setSpeed(speed: Float) {
        playerEngine.setSpeed(speed)
    }

    fun startSleepTimer(minutes: Int) {
        playerEngine.startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playerEngine.cancelSleepTimer()
    }

    fun toggleFavorite(audiobook: AudiobookEntity) {
        viewModelScope.launch {
            repository.toggleFavoriteAudiobook(audiobook.id, !audiobook.isFavorite)
        }
    }

    fun deleteAudiobook(audiobook: AudiobookEntity) {
        if (currentAudiobook.value?.id == audiobook.id) {
            playerEngine.pause()
        }
        viewModelScope.launch {
            repository.deleteAudiobook(audiobook)
        }
    }

    fun importAudioFile(uri: Uri, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.importAudioFile(uri)
            val inserted = repository.getAudiobookByIdSync(id)
            if (inserted != null) {
                playAudiobook(inserted)
            }
            onComplete(id)
        }
    }

    fun addWebOrYoutubeAudio(url: String, title: String?, author: String?, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.addWebOrYoutubeAudio(url, title, author)
            val inserted = repository.getAudiobookByIdSync(id)
            if (inserted != null) {
                playAudiobook(inserted)
            }
            onComplete(id)
        }
    }

    fun addBookmark(title: String, note: String) {
        val book = currentAudiobook.value ?: return
        val pos = currentPositionMs.value
        viewModelScope.launch {
            repository.addAudioBookmark(book.id, pos, title, note)
        }
    }

    fun deleteBookmark(bookmark: AudioBookmarkEntity) {
        viewModelScope.launch {
            repository.deleteAudioBookmark(bookmark)
        }
    }

    fun fetchAndApplyCover(audiobook: AudiobookEntity) {
        viewModelScope.launch {
            val cover = OpenCoverFetcher.fetchCoverUrl(audiobook.title, audiobook.author)
            if (cover != null) {
                database.audiobookDao().updateCoverImageUrl(audiobook.id, cover)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }
}
