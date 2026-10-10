package com.example.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ReadoverDatabase
import com.example.data.model.AudiobookEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.repository.ReadoverRepository
import com.example.ui.reader.ReadingThemeMode
import com.example.util.i18n.AppLanguage
import com.example.util.i18n.I18nManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppThemeMode(val stringKey: String) {
    SYSTEM("theme_mode_system"),
    LIGHT("theme_mode_light"),
    DARK("theme_mode_dark"),
    AMOLED("theme_mode_amoled"),
    SEPIA("theme_mode_sepia"),
    FOREST("theme_mode_forest")
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ReadoverDatabase.getDatabase(application)
    private val repository = ReadoverRepository(
        database.bookDao(),
        database.bookmarkDao(),
        database.audiobookDao(),
        application
    )

    // Current App Theme Mode
    var appThemeMode by mutableStateOf(AppThemeMode.SYSTEM)

    // Default reader theme preference
    var defaultReaderTheme by mutableStateOf(ReadingThemeMode.SEPIA)

    // Global Language State
    var selectedLanguage by mutableStateOf(I18nManager.currentLanguage)

    // Books & stats data
    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAudiobooks: StateFlow<List<AudiobookEntity>> = repository.allAudiobooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLanguage(language: AppLanguage) {
        selectedLanguage = language
        I18nManager.setLanguage(language)
    }

    fun reloadSampleData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            onComplete()
        }
    }
}
