package com.example.ui.library

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.openlibrary.OpenBookItem
import com.example.util.i18n.AppLanguage
import com.example.util.i18n.I18nManager
import com.example.util.i18n.appString

private fun getLocalizedAll(lang: AppLanguage): String = when (lang) {
    AppLanguage.TURKISH -> "Tümü"
    AppLanguage.ENGLISH -> "All"
    AppLanguage.RUSSIAN -> "Все"
    AppLanguage.GERMAN -> "Alle"
    AppLanguage.FRENCH -> "Tout"
    AppLanguage.SPANISH -> "Todo"
    AppLanguage.ITALIAN -> "Tutti"
    AppLanguage.ARABIC -> "الكل"
    AppLanguage.JAPANESE -> "すべて"
    AppLanguage.INDONESIAN -> "Semua"
    AppLanguage.CHINESE -> "全部"
}

private fun getLocalizedCategory(cat: String, lang: AppLanguage): String = when (cat) {
    "Tümü" -> getLocalizedAll(lang)
    "Türk Edebiyatı" -> when (lang) {
        AppLanguage.TURKISH -> "Türk Edebiyatı"
        AppLanguage.ENGLISH -> "Turkish Literature"
        AppLanguage.RUSSIAN -> "Турецкая литература"
        AppLanguage.GERMAN -> "Türkische Literatur"
        AppLanguage.FRENCH -> "Littérature Turque"
        AppLanguage.SPANISH -> "Literatura Turca"
        AppLanguage.ITALIAN -> "Letteratura Turca"
        AppLanguage.ARABIC -> "الأdb التركي"
        AppLanguage.JAPANESE -> "トルコ文学"
        AppLanguage.INDONESIAN -> "Sastra Turki"
        AppLanguage.CHINESE -> "土耳其文学"
    }
    "Dünya Klasikleri" -> when (lang) {
        AppLanguage.TURKISH -> "Dünya Klasikleri"
        AppLanguage.ENGLISH -> "World Classics"
        AppLanguage.RUSSIAN -> "Мировая классика"
        AppLanguage.GERMAN -> "Weltklassiker"
        AppLanguage.FRENCH -> "Classiques Mondiaux"
        AppLanguage.SPANISH -> "Clásicos Mundiales"
        AppLanguage.ITALIAN -> "Classici Mondiali"
        AppLanguage.ARABIC -> "الكلاسيكيات العالمية"
        AppLanguage.JAPANESE -> "世界古典文学"
        AppLanguage.INDONESIAN -> "Klasik Dunia"
        AppLanguage.CHINESE -> "世界名著"
    }
    "Felsefe & Düşünce" -> when (lang) {
        AppLanguage.TURKISH -> "Felsefe & Düşünce"
        AppLanguage.ENGLISH -> "Philosophy & Thought"
        AppLanguage.RUSSIAN -> "Философия и мысль"
        AppLanguage.GERMAN -> "Philosophie & Denken"
        AppLanguage.FRENCH -> "Philosophie et Pensée"
        AppLanguage.SPANISH -> "Filosofía y Pensamiento"
        AppLanguage.ITALIAN -> "Filosofia e Pensiero"
        AppLanguage.ARABIC -> "الفلسفة والفكر"
        AppLanguage.JAPANESE -> "哲学と思想"
        AppLanguage.INDONESIAN -> "Filsafat & Pemikiran"
        AppLanguage.CHINESE -> "哲学与思想"
    }
    "Polisiye & Macera" -> when (lang) {
        AppLanguage.TURKISH -> "Polisiye & Macera"
        AppLanguage.ENGLISH -> "Mystery & Adventure"
        AppLanguage.RUSSIAN -> "Детектив и приключения"
        AppLanguage.GERMAN -> "Krimi & Abenteuer"
        AppLanguage.FRENCH -> "Mystère et Aventure"
        AppLanguage.SPANISH -> "Misterio y Aventura"
        AppLanguage.ITALIAN -> "Giallo e Avventura"
        AppLanguage.ARABIC -> "الغموض والمغامرة"
        AppLanguage.JAPANESE -> "ミステリー＆冒険"
        AppLanguage.INDONESIAN -> "Misteri & Petualangan"
        AppLanguage.CHINESE -> "悬疑与冒险"
    }
    "Tarih & Siyaset" -> when (lang) {
        AppLanguage.TURKISH -> "Tarih & Siyaset"
        AppLanguage.ENGLISH -> "History & Politics"
        AppLanguage.RUSSIAN -> "История и политика"
        AppLanguage.GERMAN -> "Geschichte & Politik"
        AppLanguage.FRENCH -> "Histoire et Politique"
        AppLanguage.SPANISH -> "Historia y Política"
        AppLanguage.ITALIAN -> "Storia e Politica"
        AppLanguage.ARABIC -> "التاريخ والسياسة"
        AppLanguage.JAPANESE -> "歴史と政治"
        AppLanguage.INDONESIAN -> "Sejarah & Politik"
        AppLanguage.CHINESE -> "历史与政治"
    }
    else -> cat
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenLibraryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: LibraryViewModel,
    onOpenDownloadedBook: (Long) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var activeSource by remember { mutableStateOf("Tümü") }
    var activeCategory by remember { mutableStateOf("Tümü") }
    var selectedLanguage by remember { mutableStateOf(I18nManager.currentLanguage) }
    var searchInput by remember { mutableStateOf(viewModel.openLibrarySearchQuery) }

    val sources = listOf("Tümü", "Gutenberg", "OpenLibrary", "StandardEbooks", "Wikisource")
    val languages = AppLanguage.entries
    val categories = listOf("Tümü", "Türk Edebiyatı", "Dünya Klasikleri", "Felsefe & Düşünce", "Polisiye & Macera", "Tarih & Siyaset")
    val quickPicks = listOf("Nutuk", "Dostoyevski", "Küçük Prens", "Kafka", "Zweig", "Sherlock Holmes", "Platon", "Jack London")

    val displayedBooks = remember(viewModel.openLibraryBooks, activeSource, activeCategory) {
        viewModel.openLibraryBooks.filter { book ->
            val sourceMatch = activeSource == "Tümü" || book.source.contains(activeSource, ignoreCase = true)
            val categoryMatch = activeCategory == "Tümü" ||
                book.category.contains(activeCategory, ignoreCase = true) ||
                (activeCategory == "Türk Edebiyatı" && (book.author.contains("Atatürk") || book.author.contains("Sabahattin Ali") || book.language == "Türkçe")) ||
                (activeCategory == "Dünya Klasikleri" && (book.category.contains("Klasik") || book.author.contains("Dostoyevski") || book.author.contains("Kafka"))) ||
                (activeCategory == "Felsefe & Düşünce" && (book.category.contains("Felsefe") || book.author.contains("Platon") || book.author.contains("Sokrates"))) ||
                (activeCategory == "Polisiye & Macera" && (book.category.contains("Macera") || book.category.contains("Polisiye") || book.author.contains("Conan Doyle"))) ||
                (activeCategory == "Tarih & Siyaset" && (book.category.contains("Tarih") || book.category.contains("Siyaset") || book.title.contains("Nutuk")))
            
            sourceMatch && categoryMatch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = appString("open_library"),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = appString("open_library_desc"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("open_library_close_btn")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = appString("close"))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Search & Action Header (Single Row Layout)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_library_search_input"),
                            placeholder = {
                                val placeholderText = when (selectedLanguage) {
                                    AppLanguage.TURKISH -> "Eser veya yazar ara..."
                                    AppLanguage.ENGLISH -> "Search book or author..."
                                    AppLanguage.RUSSIAN -> "Поиск книги..."
                                    AppLanguage.GERMAN -> "Buch suchen..."
                                    AppLanguage.FRENCH -> "Rechercher..."
                                    AppLanguage.SPANISH -> "Buscar..."
                                    AppLanguage.ITALIAN -> "Cerca..."
                                    AppLanguage.ARABIC -> "بحث..."
                                    AppLanguage.JAPANESE -> "検索..."
                                    AppLanguage.INDONESIAN -> "Cari..."
                                    AppLanguage.CHINESE -> "搜索..."
                                }
                                Text(
                                    text = placeholderText,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                if (searchInput.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            searchInput = ""
                                            viewModel.searchOpenLibrary("")
                                        }
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.searchOpenLibrary(searchInput)
                            },
                            modifier = Modifier.testTag("open_library_search_btn"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = appString("search"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Loading Indicator or Results Count
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val resultsCountText = when (selectedLanguage) {
                            AppLanguage.TURKISH -> "${displayedBooks.size} açık kaynak eser bulundu"
                            AppLanguage.ENGLISH -> "${displayedBooks.size} open source books found"
                            AppLanguage.RUSSIAN -> "${displayedBooks.size} найдено книг open source"
                            AppLanguage.GERMAN -> "${displayedBooks.size} Open-Source-Bücher gefunden"
                            AppLanguage.FRENCH -> "${displayedBooks.size} livres open source trouvés"
                            AppLanguage.SPANISH -> "${displayedBooks.size} libros de código abierto encontrados"
                            AppLanguage.ITALIAN -> "${displayedBooks.size} libri open source trovati"
                            AppLanguage.ARABIC -> "تم العثور على ${displayedBooks.size} من الكتب مفتوحة المصدر"
                            AppLanguage.JAPANESE -> "${displayedBooks.size} 件のオープンソース書籍が見つかりました"
                            AppLanguage.INDONESIAN -> "${displayedBooks.size} buku sumber terbuka ditemukan"
                            AppLanguage.CHINESE -> "找到 ${displayedBooks.size} 本开源图书"
                        }
                        Text(
                            text = resultsCountText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (viewModel.isOpenLibrarySearching) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val scanningText = when (selectedLanguage) {
                                    AppLanguage.TURKISH -> "Arşiv taranıyor..."
                                    AppLanguage.ENGLISH -> "Searching archive..."
                                    AppLanguage.RUSSIAN -> "Поиск в архиве..."
                                    AppLanguage.GERMAN -> "Archiv wird durchsucht..."
                                    AppLanguage.FRENCH -> "Recherche dans l'archive..."
                                    AppLanguage.SPANISH -> "Buscando en el archivo..."
                                    AppLanguage.ITALIAN -> "Ricerca nell'archivio..."
                                    AppLanguage.ARABIC -> "جاري البحث في الأرشيف..."
                                    AppLanguage.JAPANESE -> "アーカイブを検索中..."
                                    AppLanguage.INDONESIAN -> "Mencari arsip..."
                                    AppLanguage.CHINESE -> "正在搜索归档..."
                                }
                                Text(
                                    text = scanningText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Books Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("open_library_grid")
                    ) {
                        items(displayedBooks, key = { it.id }) { bookItem ->
                            OpenBookCard(
                                item = bookItem,
                                isDownloading = viewModel.downloadingBookIds.contains(bookItem.id),
                                isDownloaded = viewModel.downloadedBookIds.contains(bookItem.id),
                                onDownload = {
                                    viewModel.downloadAndAddOpenBook(bookItem) { newId ->
                                        val addedToastText = when (selectedLanguage) {
                                            AppLanguage.TURKISH -> "\"${bookItem.title}\" kütüphanenize eklendi!"
                                            AppLanguage.ENGLISH -> "\"${bookItem.title}\" added to your library!"
                                            AppLanguage.RUSSIAN -> "\"${bookItem.title}\" добавлено в вашу библиотеку!"
                                            AppLanguage.GERMAN -> "\"${bookItem.title}\" wurde Ihrer Bibliothek hinzugefügt!"
                                            AppLanguage.FRENCH -> "\"${bookItem.title}\" a été ajouté à votre bibliothèque !"
                                            AppLanguage.SPANISH -> "¡\"${bookItem.title}\" añadido a tu biblioteca!"
                                            AppLanguage.ITALIAN -> "\"${bookItem.title}\" aggiunto alla tua libreria!"
                                            AppLanguage.ARABIC -> "تم إضافة \"${bookItem.title}\" إلى مكتبتك!"
                                            AppLanguage.JAPANESE -> "「${bookItem.title}」がライブラリに追加されました！"
                                            AppLanguage.INDONESIAN -> "\"${bookItem.title}\" ditambahkan ke perpustakaan Anda!"
                                            AppLanguage.CHINESE -> "《${bookItem.title}》已添加到您的图书室！"
                                        }
                                        Toast.makeText(
                                            context,
                                            addedToastText,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onOpen = {
                                    viewModel.downloadAndAddOpenBook(bookItem) { newId ->
                                        onDismiss()
                                        onOpenDownloadedBook(newId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OpenBookCard(
    item: OpenBookItem,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("open_book_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Cover Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
            ) {
                if (!item.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.title,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Source & Language Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.source,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Format badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = item.format,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Info and Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = com.example.data.openlibrary.BookAggregator.getAttribution(item.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "~${item.estimatedPages} sayfa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Button: Download or Open
                if (isDownloading) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("İndiriliyor...", fontSize = 12.sp)
                    }
                } else if (isDownloaded) {
                    Button(
                        onClick = onOpen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_open_downloaded_${item.id}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eklendi (Aç)", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_download_${item.id}"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kütüphaneye Ekle", fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}
