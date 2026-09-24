package com.example.ui.audio

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AudioBookmarkEntity
import com.example.data.model.AudiobookEntity
import com.example.data.model.OnlineAudiobookItem
import com.example.util.i18n.I18nManager
import com.example.util.i18n.AppLanguage
import com.example.ui.components.AudiobookListItem
import com.example.ui.components.formatTime
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookPlayerScreen(
    viewModel: AudiobookViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val allAudiobooks by viewModel.allAudiobooks.collectAsStateWithLifecycle()
    val filteredAudiobooks by viewModel.filteredAudiobooks.collectAsStateWithLifecycle()
    val currentAudiobook by viewModel.currentAudiobook.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPos by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val duration by viewModel.durationMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val sleepSecondsLeft by viewModel.sleepTimerSecondsLeft.collectAsStateWithLifecycle()
    val bookmarks by viewModel.currentBookmarks.collectAsStateWithLifecycle()

    // Active listening tracker
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                kotlinx.coroutines.delay(10000L)
                com.example.util.stats.StatsManager.addListeningTime(context, 10L)
            }
        }
    }

    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showCoverOptionsDialog by remember { mutableStateOf(false) }

    // File picker launcher for local audio files (MP3, M4A, AAC, WAV, etc.)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudioFile(uri) { id ->
                Toast.makeText(context, "Sesli kitap kütüphaneye eklendi ve kapak resmi aranıyor!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Auto-select first audiobook if none is selected
    LaunchedEffect(allAudiobooks) {
        if (currentAudiobook == null && allAudiobooks.isNotEmpty()) {
            val first = allAudiobooks.first()
            viewModel.playAudiobook(first)
            viewModel.playerEngine.pause() // keep paused initially
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sesli Kitap Player",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isPlaying) "Şu an Çalıyor" else "Readover Audio Studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                        )
                    }
                },
                actions = {
                    // Add URL / YouTube action
                    IconButton(
                        onClick = { showAddLinkDialog = true },
                        modifier = Modifier.testTag("add_audio_link_btn")
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = "Link Ekle")
                    }

                    // Add Local File action
                    IconButton(
                        onClick = {
                            audioPickerLauncher.launch(
                                arrayOf(
                                    "audio/*",
                                    "application/ogg",
                                    "audio/mpeg",
                                    "audio/mp4",
                                    "audio/x-wav",
                                    "audio/aac"
                                )
                            )
                        },
                        modifier = Modifier.testTag("import_audio_file_btn")
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = "Dosya Ekle")
                    }

                    // Cover Search / Refresh
                    if (currentAudiobook != null) {
                        IconButton(onClick = { showCoverOptionsDialog = true }) {
                            Icon(Icons.Default.ImageSearch, contentDescription = "Kapak Resmi")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error banner if any
            errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Localized Tab Titles
            val currentLang = I18nManager.currentLanguage
            val playerTabTitle = when (currentLang) {
                AppLanguage.TURKISH -> "Oynatıcı"
                AppLanguage.ENGLISH -> "Player"
                AppLanguage.RUSSIAN -> "Плеер"
                AppLanguage.GERMAN -> "Spieler"
                AppLanguage.FRENCH -> "Lecteur"
                AppLanguage.SPANISH -> "Reproductor"
                AppLanguage.ITALIAN -> "Lettore"
                AppLanguage.ARABIC -> "المشغل"
                AppLanguage.JAPANESE -> "プレイヤー"
                AppLanguage.INDONESIAN -> "Pemutar"
                AppLanguage.CHINESE -> "播放器"
            }
            val libraryTabTitle = when (currentLang) {
                AppLanguage.TURKISH -> "Kitaplık"
                AppLanguage.ENGLISH -> "Library"
                AppLanguage.RUSSIAN -> "Библиотека"
                AppLanguage.GERMAN -> "Bibliothek"
                AppLanguage.FRENCH -> "Bibliothèque"
                AppLanguage.SPANISH -> "Biblioteca"
                AppLanguage.ITALIAN -> "Libreria"
                AppLanguage.ARABIC -> "المكتبة"
                AppLanguage.JAPANESE -> "ライブラリ"
                AppLanguage.INDONESIAN -> "Perpustakaan"
                AppLanguage.CHINESE -> "馆藏"
            }
            val bookmarksTabTitle = when (currentLang) {
                AppLanguage.TURKISH -> "Zaman İmleri"
                AppLanguage.ENGLISH -> "Bookmarks"
                AppLanguage.RUSSIAN -> "Закладки"
                AppLanguage.GERMAN -> "Lesezeichen"
                AppLanguage.FRENCH -> "Signets"
                AppLanguage.SPANISH -> "Marcadores"
                AppLanguage.ITALIAN -> "Segnalibri"
                AppLanguage.ARABIC -> "العلامات"
                AppLanguage.JAPANESE -> "ブックマーク"
                AppLanguage.INDONESIAN -> "Markah"
                AppLanguage.CHINESE -> "书签"
            }
            val onlineSearchTabTitle = when (currentLang) {
                AppLanguage.TURKISH -> "Arama (Online)"
                AppLanguage.ENGLISH -> "Search (Online)"
                AppLanguage.RUSSIAN -> "Поиск"
                AppLanguage.GERMAN -> "Suche (Online)"
                AppLanguage.FRENCH -> "Recherche"
                AppLanguage.SPANISH -> "Buscar"
                AppLanguage.ITALIAN -> "Cerca"
                AppLanguage.ARABIC -> "البحث"
                AppLanguage.JAPANESE -> "オンライン検索"
                AppLanguage.INDONESIAN -> "Cari Online"
                AppLanguage.CHINESE -> "在线搜索"
            }

            TabRow(
                selectedTabIndex = if (viewModel.selectedAudioTab >= 2) 2 else viewModel.selectedAudioTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = viewModel.selectedAudioTab == 0,
                    onClick = { viewModel.selectedAudioTab = 0 },
                    text = { Text(playerTabTitle) },
                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = viewModel.selectedAudioTab == 1,
                    onClick = { viewModel.selectedAudioTab = 1 },
                    text = { Text("$libraryTabTitle (${allAudiobooks.size})") },
                    icon = { Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = viewModel.selectedAudioTab == 2 || viewModel.selectedAudioTab == 3,
                    onClick = { viewModel.selectedAudioTab = 2 },
                    text = { Text(onlineSearchTabTitle) },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (viewModel.selectedAudioTab) {
                    0 -> PlayerMainView(
                        audiobook = currentAudiobook,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        currentPos = currentPos,
                        duration = duration,
                        speed = playbackSpeed,
                        sleepSecondsLeft = sleepSecondsLeft,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSeek = { viewModel.seekTo(it) },
                        onSkipForward = { viewModel.skipForward(15) },
                        onSkipBackward = { viewModel.skipBackward(15) },
                        onOpenSpeedSheet = { showSpeedSheet = true },
                        onOpenSleepTimer = { showSleepTimerSheet = true },
                        onAddBookmark = { showAddBookmarkDialog = true },
                        onToggleFavorite = { currentAudiobook?.let { viewModel.toggleFavorite(it) } },
                        onPickLocalAudio = {
                            audioPickerLauncher.launch(
                                arrayOf("audio/*", "application/ogg", "audio/mpeg", "audio/mp4", "audio/x-wav")
                            )
                        },
                        onOpenAddLink = { showAddLinkDialog = true }
                    )
                    1 -> AudioLibraryView(
                        audiobooks = filteredAudiobooks,
                        currentAudiobook = currentAudiobook,
                        isPlaying = isPlaying,
                        searchQuery = viewModel.searchQuery,
                        onSearchChange = { viewModel.searchQuery = it },
                        onSelectAudiobook = {
                            viewModel.playAudiobook(it)
                            viewModel.selectedAudioTab = 0
                        },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onDeleteAudiobook = { viewModel.deleteAudiobook(it) },
                        onPickLocalAudio = {
                            audioPickerLauncher.launch(
                                arrayOf("audio/*", "application/ogg", "audio/mpeg", "audio/mp4", "audio/x-wav")
                            )
                        },
                        onOpenAddLink = { showAddLinkDialog = true }
                    )
                    else -> OnlineAudiobookSearchView(
                        viewModel = viewModel,
                        currentLanguage = currentLang
                    )
                }
            }
        }
    }

    // Dialog: Add YouTube / Web URL
    if (showAddLinkDialog) {
        AddAudioLinkDialog(
            onDismiss = { showAddLinkDialog = false },
            onAdd = { url, title, author ->
                viewModel.addWebOrYoutubeAudio(url, title, author) {
                    showAddLinkDialog = false
                    Toast.makeText(context, "Ses bağlantısı eklendi ve oynatılıyor!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Dialog: Add Audio Bookmark
    if (showAddBookmarkDialog) {
        AddAudioBookmarkDialog(
            currentPos = currentPos,
            onDismiss = { showAddBookmarkDialog = false },
            onSave = { title, note ->
                viewModel.addBookmark(title, note)
                showAddBookmarkDialog = false
                Toast.makeText(context, "Zaman damgası kaydedildi!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Sheet: Sleep Timer
    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            activeSecondsLeft = sleepSecondsLeft,
            onDismiss = { showSleepTimerSheet = false },
            onSetTimer = { mins ->
                viewModel.startSleepTimer(mins)
                showSleepTimerSheet = false
                if (mins > 0) {
                    Toast.makeText(context, "$mins dakika sonra oynatma durdurulacak", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Uyku zamanlayıcı kapatıldı", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Sheet: Playback Speed
    if (showSpeedSheet) {
        PlaybackSpeedBottomSheet(
            currentSpeed = playbackSpeed,
            onDismiss = { showSpeedSheet = false },
            onSelectSpeed = { speed ->
                viewModel.setSpeed(speed)
                showSpeedSheet = false
            }
        )
    }

    // Dialog: Cover Art Options
    if (showCoverOptionsDialog && currentAudiobook != null) {
        AlertDialog(
            onDismissRequest = { showCoverOptionsDialog = false },
            icon = { Icon(Icons.Default.ImageSearch, contentDescription = null, tint = EmeraldPrimary) },
            title = { Text("Açık Kaynak Kapak Resmi") },
            text = {
                Column {
                    Text("Kitabın başlığına (${currentAudiobook?.title}) göre Open Library ve Google Books veritabanlarından açık kaynak kitap kapağı aranarak güncellenir.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    currentAudiobook?.let { viewModel.fetchAndApplyCover(it) }
                    showCoverOptionsDialog = false
                    Toast.makeText(context, "Kapak resmi güncelleniyor...", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Otomatik Ara ve Güncelle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCoverOptionsDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// PLAYER MAIN VIEW
// -------------------------------------------------------------------------
@Composable
fun PlayerMainView(
    audiobook: AudiobookEntity?,
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPos: Long,
    duration: Long,
    speed: Float,
    sleepSecondsLeft: Int?,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onAddBookmark: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPickLocalAudio: () -> Unit,
    onOpenAddLink: () -> Unit
) {
    if (audiobook == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Çalınan Sesli Kitap Yok",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Yerel MP3/M4A dosyası seçebilir veya YouTube/Web ses linki ekleyebilirsiniz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPickLocalAudio) {
                    Icon(Icons.Default.AudioFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yerel Dosya Seç")
                }
                OutlinedButton(onClick = onOpenAddLink) {
                    Icon(Icons.Default.AddLink, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Link Ekle")
                }
            }
        }
        return
    }

    var sliderDraggingValue by remember { mutableStateOf<Float?>(null) }
    val progress = if (duration > 0) {
        (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Book Artwork / Cover
        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(audiobook.coverColorHex), Color(0xFF0F172A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!audiobook.coverImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = audiobook.coverImageUrl,
                    contentDescription = audiobook.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = audiobook.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Audio Source Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = when (audiobook.audioSourceType) {
                    "YOUTUBE" -> Color(0xFFDC2626)
                    "WEB_STREAM" -> Color(0xFF2563EB)
                    "LOCAL_FILE" -> EmeraldPrimary
                    else -> Color(0xFF4F46E5)
                }
            ) {
                Text(
                    text = when (audiobook.audioSourceType) {
                        "YOUTUBE" -> "YouTube Audio"
                        "WEB_STREAM" -> "Web Stream"
                        "LOCAL_FILE" -> "Yerel Ses"
                        else -> "Sesli Kitap"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animated Visualizer Sound Waves
        AudioVisualizerBars(isPlaying = isPlaying)

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Author Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audiobook.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${audiobook.author} • Seslendiren: ${audiobook.narrator}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favori",
                    tint = if (audiobook.isFavorite) Color(0xFFFFB703) else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Seek Bar Slider
        Slider(
            value = sliderDraggingValue ?: progress,
            onValueChange = { sliderDraggingValue = it },
            onValueChangeFinished = {
                sliderDraggingValue?.let {
                    val targetMs = (it * duration).toLong()
                    onSeek(targetMs)
                }
                sliderDraggingValue = null
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = EmeraldPrimary,
                activeTrackColor = EmeraldPrimary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // Time indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayPos = if (sliderDraggingValue != null) {
                (sliderDraggingValue!! * duration).toLong()
            } else {
                currentPos
            }
            Text(
                text = formatTime(displayPos),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (duration > 0) formatTime(duration) else "Canlı Yayın",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Player Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Chip / Button
            OutlinedButton(
                onClick = onOpenSpeedSheet,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = "${speed}x",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Skip Backward -15s
            IconButton(
                onClick = onSkipBackward,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "-10 sn",
                    modifier = Modifier.size(26.dp)
                )
            }

            // Large Play / Pause Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(EmeraldPrimary, TealSecondary))
                    )
                    .shadow(8.dp, shape = CircleShape)
                    .clickable { onTogglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Skip Forward +15s
            IconButton(
                onClick = onSkipForward,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "+10 sn",
                    modifier = Modifier.size(26.dp)
                )
            }

            // Sleep Timer Button
            IconButton(
                onClick = onOpenSleepTimer,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (sleepSecondsLeft != null) EmeraldPrimary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Uyku Zamanlayıcı",
                        tint = if (sleepSecondsLeft != null) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    if (sleepSecondsLeft != null) {
                        Text(
                            text = "${sleepSecondsLeft / 60}d",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Action Row: Add Timestamp Bookmark & Add Local/Web file
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onAddBookmark,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Zaman İmi Ekle", fontSize = 13.sp)
            }

            FilledTonalButton(
                onClick = onPickLocalAudio,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Yerel Dosya", fontSize = 13.sp)
            }
        }
    }
}

// -------------------------------------------------------------------------
// ANIMATED AUDIO VISUALIZER BARS
// -------------------------------------------------------------------------
@Composable
fun AudioVisualizerBars(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")

    val barHeights = listOf(
        infiniteTransition.animateFloat(
            initialValue = 6f,
            targetValue = if (isPlaying) 24f else 6f,
            animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
            label = "b1"
        ),
        infiniteTransition.animateFloat(
            initialValue = 12f,
            targetValue = if (isPlaying) 32f else 12f,
            animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
            label = "b2"
        ),
        infiniteTransition.animateFloat(
            initialValue = 8f,
            targetValue = if (isPlaying) 20f else 8f,
            animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
            label = "b3"
        ),
        infiniteTransition.animateFloat(
            initialValue = 14f,
            targetValue = if (isPlaying) 28f else 14f,
            animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
            label = "b4"
        ),
        infiniteTransition.animateFloat(
            initialValue = 6f,
            targetValue = if (isPlaying) 22f else 6f,
            animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse),
            label = "b5"
        ),
        infiniteTransition.animateFloat(
            initialValue = 10f,
            targetValue = if (isPlaying) 30f else 10f,
            animationSpec = infiniteRepeatable(tween(410, easing = LinearEasing), RepeatMode.Reverse),
            label = "b6"
        ),
        infiniteTransition.animateFloat(
            initialValue = 8f,
            targetValue = if (isPlaying) 26f else 8f,
            animationSpec = infiniteRepeatable(tween(330, easing = LinearEasing), RepeatMode.Reverse),
            label = "b7"
        )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(36.dp)
    ) {
        barHeights.forEach { heightAnim ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(heightAnim.value.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isPlaying) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

// -------------------------------------------------------------------------
// AUDIO LIBRARY VIEW (Tab 1)
// -------------------------------------------------------------------------
@Composable
fun AudioLibraryView(
    audiobooks: List<AudiobookEntity>,
    currentAudiobook: AudiobookEntity?,
    isPlaying: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectAudiobook: (AudiobookEntity) -> Unit,
    onTogglePlayPause: () -> Unit,
    onDeleteAudiobook: (AudiobookEntity) -> Unit,
    onPickLocalAudio: () -> Unit,
    onOpenAddLink: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Sesli kitap, yazar veya seslendiren ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPickLocalAudio,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yerel MP3 Ekle", fontSize = 13.sp)
                }

                FilledTonalButton(
                    onClick = onOpenAddLink,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Link / YouTube", fontSize = 13.sp)
                }
            }
        }

        item {
            Text(
                text = "Kayıtlı Sesli Kitaplar (${audiobooks.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (audiobooks.isEmpty()) {
            item {
                Text(
                    text = "Kitaplıkta sesli kitap bulunamadı. Yukarıdaki butonlarla cihazınızdan ses dosyası veya YouTube linki ekleyebilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(audiobooks, key = { it.id }) { book ->
                val isCurrent = currentAudiobook?.id == book.id
                AudiobookListItem(
                    audiobook = book,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { onSelectAudiobook(book) },
                    onTogglePlayPause = {
                        if (isCurrent) {
                            onTogglePlayPause()
                        } else {
                            onSelectAudiobook(book)
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// AUDIO BOOKMARKS VIEW (Tab 2)
// -------------------------------------------------------------------------
@Composable
fun AudioBookmarksView(
    bookmarks: List<AudioBookmarkEntity>,
    onSeekTo: (Long) -> Unit,
    onDeleteBookmark: (AudioBookmarkEntity) -> Unit,
    onAddBookmark: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bu Kitaba Ait Zaman Damgaları",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onAddBookmark) {
                    Icon(Icons.Default.Add, contentDescription = "İm Ekle", tint = EmeraldPrimary)
                }
            }
        }

        if (bookmarks.isEmpty()) {
            item {
                Text(
                    text = "Henüz zaman damgası veya not kaydedilmedi. Ses kaydının istediğiniz anında 'Zaman İmi Ekle' butonuna basarak not alabilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(bookmarks, key = { it.id }) { bookmark ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeekTo(bookmark.timestampMs) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = formatTime(bookmark.timestampMs),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bookmark.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (bookmark.note.isNotBlank()) {
                                Text(
                                    text = bookmark.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteBookmark(bookmark) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// DIALOGS & BOTTOM SHEETS
// -------------------------------------------------------------------------
@Composable
fun AddAudioLinkDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddLink, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ses Bağlantısı / YouTube Ekle")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "YouTube video bağlantısı, canlı radyo akışı, podcast veya doğrudan MP3/M4A URL'si girin:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL Bağlantısı (Zorunlu)") },
                    placeholder = { Text("https://youtube.com/watch?v=... veya https://...mp3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Kitap / Ses Başlığı (İsteğe bağlı)") },
                    placeholder = { Text("Otomatik belirlenir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Yazar / Seslendiren (İsteğe bağlı)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onAdd(url, title.ifBlank { null }, author.ifBlank { null })
                    }
                },
                enabled = url.isNotBlank()
            ) {
                Text("Oynatıcıya Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun AddAudioBookmarkDialog(
    currentPos: Long,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("Zaman Damgası (${formatTime(currentPos)})") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Zaman İmi ve Not Kaydet")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Şu anki konum: ${formatTime(currentPos)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Başlık / Bölüm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notunuz (İsteğe bağlı)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, note)
                    }
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    activeSecondsLeft: Int?,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Uyku Zamanlayıcısı",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Belirlenen süre sonunda sesli kitap otomatik olarak durdurulur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            val timerOptions = listOf(
                "5 Dakika" to 5,
                "15 Dakika" to 15,
                "30 Dakika" to 30,
                "45 Dakika" to 45,
                "60 Dakika (1 Saat)" to 60,
                "90 Dakika (1.5 Saat)" to 90
            )

            timerOptions.forEach { (label, mins) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSetTimer(mins) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            if (activeSecondsLeft != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onSetTimer(0) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zamanlayıcıyı İptal Et")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedBottomSheet(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Oynatma Hızı",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f)

            speeds.forEach { speedVal ->
                val isSelected = (currentSpeed == speedVal)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectSpeed(speedVal) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (speedVal == 1.0f) "1.0x (Normal Hız)" else "${speedVal}x",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineAudiobookSearchView(
    viewModel: AudiobookViewModel,
    currentLanguage: AppLanguage
) {
    val context = LocalContext.current
    var searchInput by remember { mutableStateOf(viewModel.onlineSearchQuery) }

    val searchPlaceholder = when (currentLanguage) {
        AppLanguage.TURKISH -> "Sesli kitap veya yazar ara..."
        AppLanguage.ENGLISH -> "Search audiobook or author..."
        AppLanguage.RUSSIAN -> "Поиск аудиокниги..."
        AppLanguage.GERMAN -> "Hörbuch suchen..."
        AppLanguage.FRENCH -> "Rechercher un livre audio..."
        AppLanguage.SPANISH -> "Buscar audiolibro..."
        AppLanguage.ITALIAN -> "Cerca audiolibro..."
        AppLanguage.ARABIC -> "البحث عن كتاب صوتi..."
        AppLanguage.JAPANESE -> "オーディオブックを検索..."
        AppLanguage.INDONESIAN -> "Cari buku audio..."
        AppLanguage.CHINESE -> "搜索有声书..."
    }

    val searchButtonText = when (currentLanguage) {
        AppLanguage.TURKISH -> "Ara"
        AppLanguage.ENGLISH -> "Search"
        AppLanguage.RUSSIAN -> "Поиск"
        AppLanguage.GERMAN -> "Suchen"
        AppLanguage.FRENCH -> "Rechercher"
        AppLanguage.SPANISH -> "Buscar"
        AppLanguage.ITALIAN -> "Cerca"
        AppLanguage.ARABIC -> "بحث"
        AppLanguage.JAPANESE -> "検索"
        AppLanguage.INDONESIAN -> "Cari"
        AppLanguage.CHINESE -> "搜索"
    }

    val attributionText = when (currentLanguage) {
        AppLanguage.TURKISH -> "Bu sesli kitap kayıtları LibriVox gönüllüleri tarafından sağlanmıştır (librivox.org). Kamu malı eserlerdir."
        AppLanguage.ENGLISH -> "These audiobook recordings are provided by LibriVox volunteers (librivox.org). They are public domain works."
        AppLanguage.RUSSIAN -> "Эти аудиозаписи предоставлены волонтерами LibriVox (librivox.org). Это произведения общественного достояния."
        AppLanguage.GERMAN -> "Diese Hörbuchaufnahmen werden von LibriVox-Freiwilligen zur Verfügung gestellt (librivox.org). Sie sind gemeinfrei."
        AppLanguage.FRENCH -> "Ces enregistrements de livres audio sont fournis par des bénévoles de LibriVox (librivox.org). Ce sont des œuvres du domaine public."
        AppLanguage.SPANISH -> "Estas grabaciones de audiolibros son proporcionadas por voluntarios de LibriVox (librivox.org). Son obras de dominio público."
        AppLanguage.ITALIAN -> "Queste registrazioni di audiolibri sono fornite dai volontari di LibriVox (librivox.org). Sono opere di pubblico dominio."
        AppLanguage.ARABIC -> "يتم توفير تسجيلات الكتب الصوتية هذه من قبل متطوعي LibriVox (librivox.org). وهي أعمال في المجال العام."
        AppLanguage.JAPANESE -> "これらのオーディオブックの録音は、LibriVoxのボランティア（librivox.org）によって提供されています。これらはパブリックドメインの作品です。"
        AppLanguage.INDONESIAN -> "Rekaman buku audio ini disediakan oleh sukarelawan LibriVox (librivox.org). Ini adalah karya domain publik."
        AppLanguage.CHINESE -> "这些有声书录音由 LibriVox 志愿者 (librivox.org) 提供。它们是公共领域的作品。"
    }

    val scanningText = when (currentLanguage) {
        AppLanguage.TURKISH -> "Açık arşivler taranıyor, ses akışları çözümleniyor..."
        AppLanguage.ENGLISH -> "Scanning public archives, resolving streams..."
        AppLanguage.RUSSIAN -> "Сканирование архивов, определение потоков..."
        AppLanguage.GERMAN -> "Archive werden gescannt, Streams werden aufgelöst..."
        AppLanguage.FRENCH -> "Analyse des archives, résolution des flux..."
        AppLanguage.SPANISH -> "Escaneando archivos, resolviendo transmisiones..."
        AppLanguage.ITALIAN -> "Scansione archivi, risoluzione flussi..."
        AppLanguage.ARABIC -> "جاري مسح الأرشيفات، وحل التدفقات..."
        AppLanguage.JAPANESE -> "公開アーカイブをスキャン中、ストリームを解決中..."
        AppLanguage.INDONESIAN -> "Memindai arsip, menyelesaikan aliran..."
        AppLanguage.CHINESE -> "正在扫描公共归档，正在解析音频流..."
    }

    val initialText = when (currentLanguage) {
        AppLanguage.TURKISH -> "Arşivlerde milyonlarca kamu malı eser bulunuyor. Arama yaparak dinlemeye başlayın!"
        AppLanguage.ENGLISH -> "There are millions of public domain works in archives. Search to start listening!"
        AppLanguage.RUSSIAN -> "В архивах миллионы произведений общественного достояния. Найдите, чтобы начать слушать!"
        AppLanguage.GERMAN -> "Es gibt Millionen gemeinfreier Werke in den Archiven. Suchen Sie, um zuzuhören!"
        AppLanguage.FRENCH -> "Il y a des millions d'œuvres du domaine public dans les archives. Recherchez pour écouter !"
        AppLanguage.SPANISH -> "Hay millones de obras de dominio público en los archivos. ¡Busca para empezar a escuchar!"
        AppLanguage.ITALIAN -> "Ci sono milioni di opere di pubblico dominio negli archivi. Cerca per iniziare ad ascoltare!"
        AppLanguage.ARABIC -> "هناك الملايين من أعمال المجال العام في الأرشيف. ابحث لبدء الاستماع!"
        AppLanguage.JAPANESE -> "アーカイブには何百万ものパブリックドメインの作品があります。検索して聴き始めましょう！"
        AppLanguage.INDONESIAN -> "Ada jutaan karya domain publik di arsip. Cari untuk mulai mendengarkan!"
        AppLanguage.CHINESE -> "归档中包含数百万部公共领域作品。开始搜索并倾听吧！"
    }

    val resultsTitle = when (currentLanguage) {
        AppLanguage.TURKISH -> "Arama Sonuçları"
        AppLanguage.ENGLISH -> "Search Results"
        AppLanguage.RUSSIAN -> "Результаты поиска"
        AppLanguage.GERMAN -> "Suchergebnisse"
        AppLanguage.FRENCH -> "Résultats de recherche"
        AppLanguage.SPANISH -> "Resultados de búsqueda"
        AppLanguage.ITALIAN -> "Risultati della ricerca"
        AppLanguage.ARABIC -> "نتائج البحث"
        AppLanguage.JAPANESE -> "検索結果"
        AppLanguage.INDONESIAN -> "Hasil Pencarian"
        AppLanguage.CHINESE -> "搜索结果"
    }

    val streamLoadingMsg = when (currentLanguage) {
        AppLanguage.TURKISH -> "Ses akış adresi doğrulanıyor ve yükleniyor..."
        AppLanguage.ENGLISH -> "Verifying and loading audio stream..."
        AppLanguage.RUSSIAN -> "Проверка и загрузка аудиопотока..."
        AppLanguage.GERMAN -> "Audiostream wird überprüft und geladen..."
        AppLanguage.FRENCH -> "Vérification et chargement du flux audio..."
        AppLanguage.SPANISH -> "Verificando y cargando transmisión de audio..."
        AppLanguage.ITALIAN -> "Verifica e caricamento del flusso audio..."
        AppLanguage.ARABIC -> "جاري التحقق من تدفق الصوت وتحميله..."
        AppLanguage.JAPANESE -> "音声ストリームを検証して読み込み中..."
        AppLanguage.INDONESIAN -> "Memverifikasi dan memuat aliran audio..."
        AppLanguage.CHINESE -> "正在验证并加载音频流..."
    }

    val nowPlayingMsg = when (currentLanguage) {
        AppLanguage.TURKISH -> "çalınıyor!"
        AppLanguage.ENGLISH -> "is playing!"
        AppLanguage.RUSSIAN -> "играет!"
        AppLanguage.GERMAN -> "wird abgespielt!"
        AppLanguage.FRENCH -> "est en cours de lecture !"
        AppLanguage.SPANISH -> "se está reproduciendo!"
        AppLanguage.ITALIAN -> "in riproduzione!"
        AppLanguage.ARABIC -> "يشتغل الآن!"
        AppLanguage.JAPANESE -> "が再生されています！"
        AppLanguage.INDONESIAN -> "sedang diputar!"
        AppLanguage.CHINESE -> "正在播放！"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("online_audiobook_search_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Unified Search Field
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text(searchPlaceholder, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        viewModel.searchOnlineAudiobooks(searchInput)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(searchButtonText, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Legal Attribution Info Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attributionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LibriVox • Internet Archive • Loyal Books",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }

        // Search Status & Errors
        if (viewModel.isOnlineSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = scanningText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        viewModel.onlineSearchError?.let { err ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Items Grid/List
        if (viewModel.onlineAudiobooks.isEmpty() && !viewModel.isOnlineSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (!viewModel.isOnlineSearching) {
            item {
                Text(
                    text = "$resultsTitle (${viewModel.onlineAudiobooks.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(viewModel.onlineAudiobooks, key = { it.id }) { item ->
                OnlineAudiobookItemCard(
                    item = item,
                    currentLanguage = currentLanguage,
                    onPlay = {
                        Toast.makeText(context, streamLoadingMsg, Toast.LENGTH_SHORT).show()
                        viewModel.playOnlineAudiobook(item) {
                            Toast.makeText(context, "\"${item.title}\" $nowPlayingMsg", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun OnlineAudiobookItemCard(
    item: OnlineAudiobookItem,
    currentLanguage: AppLanguage,
    onPlay: () -> Unit
) {
    val authorLabel = when (currentLanguage) {
        AppLanguage.TURKISH -> "Yazar"
        AppLanguage.ENGLISH -> "Author"
        AppLanguage.RUSSIAN -> "Автор"
        AppLanguage.GERMAN -> "Autor"
        AppLanguage.FRENCH -> "Auteur"
        AppLanguage.SPANISH -> "Autor"
        AppLanguage.ITALIAN -> "Autore"
        AppLanguage.ARABIC -> "المؤلف"
        AppLanguage.JAPANESE -> "著者"
        AppLanguage.INDONESIAN -> "Penulis"
        AppLanguage.CHINESE -> "作者"
    }

    val narratorLabel = when (currentLanguage) {
        AppLanguage.TURKISH -> "Seslendiren"
        AppLanguage.ENGLISH -> "Narrator"
        AppLanguage.RUSSIAN -> "Читает"
        AppLanguage.GERMAN -> "Sprecher"
        AppLanguage.FRENCH -> "Narrateur"
        AppLanguage.SPANISH -> "Narrador"
        AppLanguage.ITALIAN -> "Narratore"
        AppLanguage.ARABIC -> "الراوي"
        AppLanguage.JAPANESE -> "ナレーター"
        AppLanguage.INDONESIAN -> "Narator"
        AppLanguage.CHINESE -> "朗读者"
    }

    val sourceLabel = when (currentLanguage) {
        AppLanguage.TURKISH -> "Kaynak"
        AppLanguage.ENGLISH -> "Source"
        AppLanguage.RUSSIAN -> "Источник"
        AppLanguage.GERMAN -> "Quelle"
        AppLanguage.FRENCH -> "Source"
        AppLanguage.SPANISH -> "Fuente"
        AppLanguage.ITALIAN -> "Fonte"
        AppLanguage.ARABIC -> "المصدر"
        AppLanguage.JAPANESE -> "ソース"
        AppLanguage.INDONESIAN -> "Sumber"
        AppLanguage.CHINESE -> "来源"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.coverImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$authorLabel: ${item.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.narrator.isNotBlank()) {
                    Text(
                        text = "$narratorLabel: ${item.narrator}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(1.dp)
                    ) {
                        Text(
                            text = "$sourceLabel: ${item.source}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EmeraldPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onPlay,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                    contentColor = EmeraldPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Oynat")
            }
        }
    }
}

