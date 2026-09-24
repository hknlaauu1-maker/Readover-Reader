package com.example.ui.library

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.ui.audio.AudiobookPlayerScreen
import com.example.ui.audio.AudiobookViewModel
import com.example.ui.components.AdBannerCard
import com.example.ui.components.WoodenBookshelfGrid
import com.example.ui.components.BookListItem
import com.example.ui.components.FormatBadge
import com.example.ui.components.MiniAudioPlayerBar
import com.example.ui.components.ReadingStatsDashboardCard
import com.example.ui.settings.AppThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.settings.PremiumPurchaseDialog
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.util.ads.AdManager
import com.example.util.i18n.appString
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    audiobookViewModel: AudiobookViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenBook: (Long) -> Unit
) {
    val context = LocalContext.current
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val filteredBooks by viewModel.filteredBooks.collectAsStateWithLifecycle()
    val recentBooks by viewModel.recentBooks.collectAsStateWithLifecycle()
    val favoriteBooks by viewModel.favoriteBooks.collectAsStateWithLifecycle()
    val allBookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()

    // Audio states for mini player
    val currentAudiobook by audiobookViewModel.currentAudiobook.collectAsStateWithLifecycle()
    val isAudioPlaying by audiobookViewModel.isPlaying.collectAsStateWithLifecycle()
    val currentAudioPos by audiobookViewModel.currentPositionMs.collectAsStateWithLifecycle()
    val audioDuration by audiobookViewModel.durationMs.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedBookForDetails by remember { mutableStateOf<BookEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AdManager.init(context)
    }

    // File picker launcher for importing PDF, EPUB, MOBI, FB2, TXT
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBookFile(uri) { newBookId ->
                Toast.makeText(context, "Kitap eklendi ve açık kaynak kapağı taranıyor!", Toast.LENGTH_SHORT).show()
                onOpenBook(newBookId)
            }
        }
    }

    val formats = listOf("TÜMÜ", "EPUB", "PDF", "MOBI", "FB2", "TXT")

    // If "S. Kitap" tab is active (index 1), display the full Audiobook Player Screen
    if (viewModel.selectedTab == 1) {
        Scaffold(
            bottomBar = {
                ReadoverBottomBar(
                    selectedTab = viewModel.selectedTab,
                    onSelectTab = { viewModel.selectedTab = it }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AudiobookPlayerScreen(viewModel = audiobookViewModel)
            }
        }
        return
    }

    // If "Ayarlar" tab is active (index 4), display the Settings & Stats Screen
    if (viewModel.selectedTab == 4) {
        Scaffold(
            bottomBar = {
                ReadoverBottomBar(
                    selectedTab = viewModel.selectedTab,
                    onSelectTab = { viewModel.selectedTab = it }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = appString("app_title"),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                                Text(
                                    text = appString("app_subtitle"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick Dark Mode Toggle Button
                        val isDark = when (settingsViewModel.appThemeMode) {
                            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                            AppThemeMode.LIGHT, AppThemeMode.SEPIA -> false
                            AppThemeMode.DARK, AppThemeMode.AMOLED, AppThemeMode.FOREST -> true
                        }
                        IconButton(
                            onClick = {
                                settingsViewModel.appThemeMode = if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                            },
                            modifier = Modifier.testTag("quick_dark_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Gece Modu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Search Button
                        IconButton(
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) viewModel.searchQuery = ""
                            },
                            modifier = Modifier.testTag("search_icon_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = appString("search")
                            )
                        }

                        // Grid / List View Toggle (For books tab)
                        if (viewModel.selectedTab == 0) {
                            IconButton(
                                onClick = { viewModel.isGridView = !viewModel.isGridView },
                                modifier = Modifier.testTag("view_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (viewModel.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = appString("view_mode")
                                )
                            }
                        }

                        // Sort Menu
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = appString("sort"))
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                BookSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            viewModel.sortOrder = order
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (viewModel.sortOrder == order) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Search Bar
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("search_text_input"),
                        placeholder = { Text(appString("search_books")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (viewModel.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = appString("cancel"))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        bottomBar = {
            Column {
                // Mini Player Bar (if audio is loaded/playing)
                MiniAudioPlayerBar(
                    audiobook = currentAudiobook,
                    isPlaying = isAudioPlaying,
                    currentPos = currentAudioPos,
                    duration = audioDuration,
                    onExpandPlayer = { viewModel.selectedTab = 1 },
                    onTogglePlayPause = { audiobookViewModel.togglePlayPause() },
                    onSkipForward = { audiobookViewModel.skipForward(10) }
                )

                ReadoverBottomBar(
                    selectedTab = viewModel.selectedTab,
                    onSelectTab = { viewModel.selectedTab = it }
                )
            }
        },
        floatingActionButton = {
            if (viewModel.selectedTab == 0 && filteredBooks.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    // Tek Ana "Kitap Aç" Butonu (Kitaplar varken sağ altta)
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.isOpenLibraryDialogOpen = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = appString("open_book")) },
                        text = { Text(appString("open_book"), fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_open_book")
                    )

                    if (!AdManager.isPremiumUser) {
                        AdBannerCard(
                            onRemoveAdsClick = { showPremiumDialog = true },
                            isCompact = true,
                            modifier = Modifier.fillMaxWidth(0.95f)
                        )
                    }
                }
            } else if (viewModel.selectedTab == 0 && !AdManager.isPremiumUser && filteredBooks.isEmpty()) {
                AdBannerCard(
                    onRemoveAdsClick = { showPremiumDialog = true },
                    isCompact = true,
                    modifier = Modifier.fillMaxWidth(0.95f)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (viewModel.selectedTab) {
                0 -> LibraryContent(
                    books = filteredBooks,
                    allBooks = allBooks,
                    isGridView = viewModel.isGridView,
                    onOpenBook = onOpenBook,
                    onOpenLibraryClicked = { viewModel.isOpenLibraryDialogOpen = true },
                    onRemoveAdsClick = { showPremiumDialog = true },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onBookLongClick = { selectedBookForDetails = it },
                    selectedFormatFilter = viewModel.selectedFormatFilter,
                    onFormatFilterSelected = { viewModel.selectedFormatFilter = it },
                    searchQuery = viewModel.searchQuery,
                    onSearchQueryChanged = { viewModel.searchQuery = it },
                    onImportClicked = { viewModel.isOpenLibraryDialogOpen = true }
                )
                2 -> RecentReadsContent(
                    recentBooks = recentBooks,
                    favoriteBooks = favoriteBooks,
                    onOpenBook = onOpenBook,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onBookLongClick = { selectedBookForDetails = it }
                )
                3 -> BookmarksContent(
                    bookmarks = allBookmarks,
                    onOpenBookPage = { bookId, _ -> onOpenBook(bookId) },
                    onDeleteBookmark = { viewModel.deleteBookmark(it) }
                )
            }
        }
    }

    // Premium Purchase Dialog
    PremiumPurchaseDialog(
        isOpen = showPremiumDialog,
        onDismiss = { showPremiumDialog = false }
    )

    // Open Library & Internet Archive Book Explorer Dialog
    OpenLibraryDialog(
        isOpen = viewModel.isOpenLibraryDialogOpen,
        onDismiss = { viewModel.isOpenLibraryDialogOpen = false },
        viewModel = viewModel,
        onOpenDownloadedBook = { downloadedBookId ->
            onOpenBook(downloadedBookId)
        },
        onImportLocalFile = {
            filePickerLauncher.launch(
                arrayOf(
                    "application/pdf",
                    "application/epub+zip",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain",
                    "application/octet-stream",
                    "*/*"
                )
            )
        }
    )

    // Book Details & Actions Dialog
    selectedBookForDetails?.let { book ->
        BookDetailsDialog(
            book = book,
            onDismiss = { selectedBookForDetails = null },
            onOpenBook = {
                selectedBookForDetails = null
                onOpenBook(book.id)
            },
            onDeleteBook = {
                viewModel.deleteBook(book)
                selectedBookForDetails = null
                Toast.makeText(context, "\"${book.title}\" kitaplıktan kaldırıldı", Toast.LENGTH_SHORT).show()
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(book)
                selectedBookForDetails = null
            },
            onRefreshCover = {
                viewModel.fetchAndApplyCoverForBook(book)
                selectedBookForDetails = null
                Toast.makeText(context, "Açık kaynak kapak resmi aranıyor...", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ReadoverBottomBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        // Tab 0: Kitaplık
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onSelectTab(0) },
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = appString("nav_library")) },
            label = {
                Text(
                    text = appString("nav_library"),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center
                )
            },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("nav_tab_library")
        )

        // Tab 1: S. Kitap (Audiobook)
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onSelectTab(1) },
            icon = { Icon(Icons.Default.Headphones, contentDescription = appString("nav_audiobook")) },
            label = {
                Text(
                    text = appString("nav_audiobook"),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center
                )
            },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("nav_tab_audiobook")
        )

        // Tab 2: Son Okunan (strictly horizontal single-line so top icon is perfectly aligned)
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onSelectTab(2) },
            icon = { Icon(Icons.Default.History, contentDescription = appString("nav_recent")) },
            label = {
                Text(
                    text = appString("nav_recent"),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("nav_tab_recent")
        )

        // Tab 3: Yer İmleri
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onSelectTab(3) },
            icon = { Icon(Icons.Default.Bookmark, contentDescription = appString("nav_bookmarks")) },
            label = {
                Text(
                    text = appString("nav_bookmarks"),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center
                )
            },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("nav_tab_bookmarks")
        )

        // Tab 4: Ayarlar & İstatistik
        NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { onSelectTab(4) },
            icon = { Icon(Icons.Default.Settings, contentDescription = appString("nav_settings")) },
            label = {
                Text(
                    text = appString("nav_settings"),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center
                )
            },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("nav_tab_settings")
        )
    }
}

@Composable
fun LibraryContent(
    books: List<BookEntity>,
    allBooks: List<BookEntity>,
    isGridView: Boolean,
    onOpenBook: (Long) -> Unit,
    onOpenLibraryClicked: () -> Unit,
    onRemoveAdsClick: () -> Unit,
    onToggleFavorite: (BookEntity) -> Unit,
    onBookLongClick: (BookEntity) -> Unit,
    selectedFormatFilter: String = "TÜMÜ",
    onFormatFilterSelected: (String) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {},
    onImportClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (books.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Active "Kitap Aç" button in place of the center empty state image
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable { onImportClicked() }
                        .testTag("btn_center_open_book")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = appString("open_book"),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Kitaplığınız Boş",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Cihazınızda, Google Drive veya diğer bulut hesaplarınızda bulunan kitap ve belgelerinizi hemen kütüphanenize aktarabilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            if (isGridView) {
                WoodenBookshelfGrid(
                    books = books,
                    onOpenBook = onOpenBook,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(books, key = { it.id }) { book ->
                        BookListItem(
                            book = book,
                            onClick = { onOpenBook(book.id) },
                            onToggleFavorite = { onToggleFavorite(book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentReadsContent(
    recentBooks: List<BookEntity>,
    favoriteBooks: List<BookEntity>,
    onOpenBook: (Long) -> Unit,
    onToggleFavorite: (BookEntity) -> Unit,
    onBookLongClick: (BookEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Continue Reading Hero Card
        val lastReadBook = recentBooks.firstOrNull()
        if (lastReadBook != null) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Kaldığınız Yerden Devam Edin",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenBook(lastReadBook.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(76.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(lastReadBook.coverColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            FormatBadge(format = lastReadBook.format, isCompact = true)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lastReadBook.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = lastReadBook.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { lastReadBook.progressPercent.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sayfa ${lastReadBook.currentPage} / ${lastReadBook.totalPages} (%${(lastReadBook.progressPercent * 100).toInt()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Recent Books List
        if (recentBooks.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Son Okunan Kitaplar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            items(recentBooks, key = { "recent_${it.id}" }) { book ->
                BookListItem(
                    book = book,
                    onClick = { onOpenBook(book.id) },
                    onToggleFavorite = { onToggleFavorite(book) }
                )
            }
        }

        // Favorites Section
        if (favoriteBooks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Favori Kitaplarım",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            items(favoriteBooks, key = { "fav_${it.id}" }) { book ->
                BookListItem(
                    book = book,
                    onClick = { onOpenBook(book.id) },
                    onToggleFavorite = { onToggleFavorite(book) }
                )
            }
        }
    }
}

@Composable
fun BookmarksContent(
    bookmarks: List<BookmarkEntity>,
    onOpenBookPage: (Long, Int) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit
) {
    if (bookmarks.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.BookmarkBorder,
            title = "Henüz Yer İmi Yok",
            description = "Okuma ekranında beğendiğiniz alıntıları ve sayfaları yer imi olarak ekleyebilirsiniz."
        )
        return
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(bookmarks, key = { it.id }) { bookmark ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBookPage(bookmark.bookId, bookmark.pageNumber) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(bookmark.colorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bookmark.bookTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onDeleteBookmark(bookmark) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = appString("delete"),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (bookmark.quote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(bookmark.colorHex).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "“${bookmark.quote}”",
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (bookmark.userNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Not: ${bookmark.userNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sayfa ${bookmark.pageNumber} • ${bookmark.chapterTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dateFormat.format(Date(bookmark.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BookDetailsDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onOpenBook: () -> Unit,
    onDeleteBook: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRefreshCover: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = book.title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Yazar: ${book.author}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Kategori: ${book.category}", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Format: ", style = MaterialTheme.typography.bodyMedium)
                    FormatBadge(format = book.format)
                }
                Text(text = "Toplam Sayfa: ${book.totalPages}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "İlerleme: Sayfa ${book.currentPage} (%${(book.progressPercent * 100).toInt()})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Boyut: ${(book.fileSizeBytes / 1024)} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onRefreshCover,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(appString("open_cover_search"), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenBook) {
                Text(appString("open_book"))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDeleteBook,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(appString("delete"))
                }
                TextButton(onClick = onDismiss) {
                    Text(appString("close"))
                }
            }
        }
    )
}
