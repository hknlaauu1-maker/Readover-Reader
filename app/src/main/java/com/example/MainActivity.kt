package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.audio.AudiobookPlayerScreen
import com.example.ui.audio.AudiobookViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.reader.ReaderScreen
import com.example.ui.reader.ReaderViewModel
import com.example.ui.settings.AppThemeMode
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.i18n.I18nManager
import com.example.util.i18n.LocalAppLanguage
import com.example.util.stats.StatsManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.database.ReadoverDatabase
import com.example.data.repository.ReadoverRepository

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private val audiobookViewModel: AudiobookViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize persistent stats manager
        StatsManager.init(this)

        // Seed initial data exactly once safely on background thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = ReadoverDatabase.getDatabase(this@MainActivity)
                val repository = ReadoverRepository(
                    database.bookDao(),
                    database.bookmarkDao(),
                    database.audiobookDao(),
                    application
                )
                repository.checkAndSeedInitialData()
            } catch (e: Exception) {
                // Safely catch any startup database initialization/seeding error
            }
        }

        // Check if opened via external intent
        handleIncomingIntent(intent)

        setContent {
            CompositionLocalProvider(LocalAppLanguage provides settingsViewModel.selectedLanguage) {
                MyApplicationTheme(appThemeMode = settingsViewModel.appThemeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = "library",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) }
                        ) {
                            composable("library") {
                                LibraryScreen(
                                    viewModel = libraryViewModel,
                                    audiobookViewModel = audiobookViewModel,
                                    settingsViewModel = settingsViewModel,
                                    onOpenBook = { bookId ->
                                        navController.navigate("reader/$bookId")
                                    }
                                )
                            }

                            composable("audiobook_player") {
                                AudiobookPlayerScreen(
                                    viewModel = audiobookViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(
                                route = "reader/{bookId}",
                                arguments = listOf(
                                    navArgument("bookId") { type = NavType.LongType }
                                )
                            ) { backStackEntry ->
                                val bookId = backStackEntry.arguments?.getLong("bookId") ?: 1L
                                val readerViewModel: ReaderViewModel = viewModel()
                                ReaderScreen(
                                    bookId = bookId,
                                    viewModel = readerViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri: Uri = intent.data ?: return
            val type = intent.type ?: ""
            val scheme = uri.scheme ?: ""
            val path = uri.path?.lowercase() ?: ""

            try {
                if (type.startsWith("audio/") || path.endsWith(".mp3") || path.endsWith(".m4a") || path.endsWith(".wav") || path.endsWith(".aac")) {
                    audiobookViewModel.importAudioFile(uri) {
                        Toast.makeText(this, "Sesli kitap Readover'a aktarıldı!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    libraryViewModel.importBookFile(uri) {
                        Toast.makeText(this, "Belge Readover'a aktarıldı!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MainActivity", "URI erişim güvenlik hatası: ${e.message}")
                Toast.makeText(this, "Dosya erişim izni alınamadı.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("MainActivity", "Intent işleme hatası: ${e.message}")
            }
        }
    }
}
