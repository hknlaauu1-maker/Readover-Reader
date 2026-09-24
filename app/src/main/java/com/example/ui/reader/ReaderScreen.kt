package com.example.ui.reader

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BookmarkEntity
import com.example.ui.theme.*
import com.example.util.i18n.appString
import com.example.util.stats.StatsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    viewModel: ReaderViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val book by viewModel.currentBook.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    // Active reading session tracker
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000L)
            StatsManager.addReadingTime(context, 10L)
        }
    }

    // Initialize TTS controller
    val ttsController = remember {
        TtsController(context)
    }

    DisposableEffect(Unit) {
        viewModel.loadBook(bookId)
        onDispose {
            ttsController.release()
        }
    }

    // Handle in-app brightness
    DisposableEffect(viewModel.brightnessLevel) {
        val window = (context as? Activity)?.window
        val layoutParams = window?.attributes
        if (layoutParams != null) {
            layoutParams.screenBrightness = viewModel.brightnessLevel.coerceIn(0.05f, 1.0f)
            window.attributes = layoutParams
        }
        onDispose {
            val restoreParams = window?.attributes
            if (restoreParams != null) {
                restoreParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = restoreParams
            }
        }
    }

    val currentTheme = viewModel.currentTheme
    val currentPageText = viewModel.getCurrentPageText()
    val sentences = remember(currentPageText) {
        BookReaderEngine.extractSentences(currentPageText)
    }

    // Sync TTS content when page changes
    LaunchedEffect(sentences) {
        ttsController.setContent(sentences, 0)
    }

    val totalPages = viewModel.pagination.pages.size.coerceAtLeast(1)
    val currentPageNum = viewModel.currentPageIndex + 1
    val isCurrentPageBookmarked = bookmarks.any { it.pageNumber == currentPageNum }
    val progressPercent = ((currentPageNum.toFloat() / totalPages.toFloat()) * 100).toInt()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = currentTheme.backgroundColor,
        topBar = {
            AnimatedVisibility(
                visible = viewModel.showControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = book?.title ?: appString("app_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${viewModel.getCurrentChapterTitle()} • Sayfa $currentPageNum/$totalPages (%$progressPercent)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = appString("close")
                            )
                        }
                    },
                    actions = {
                        // Quick Save Current Page Progress Button
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentPageAndProgress { page, percent ->
                                    Toast.makeText(
                                        context,
                                        "📌 Sayfa $page (%${(percent * 100).toInt()}) kaldığınız yer olarak kaydedildi!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.testTag("save_progress_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = appString("save_current_page"),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // TTS Button
                        IconButton(
                            onClick = {
                                viewModel.showTtsBar = !viewModel.showTtsBar
                                if (viewModel.showTtsBar) {
                                    ttsController.play()
                                } else {
                                    ttsController.stop()
                                }
                            },
                            modifier = Modifier.testTag("tts_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (ttsController.isPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = appString("tts_title"),
                                tint = if (ttsController.isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Bookmark Button
                        IconButton(
                            onClick = {
                                if (isCurrentPageBookmarked) {
                                    val currentB = bookmarks.find { it.pageNumber == currentPageNum }
                                    if (currentB != null) {
                                        viewModel.deleteBookmark(currentB)
                                        Toast.makeText(context, "Yer imi kaldırıldı!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    viewModel.showBookmarkDialog = true
                                }
                            },
                            modifier = Modifier.testTag("bookmark_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = appString("add_bookmark"),
                                tint = if (isCurrentPageBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Appearance & Theme Settings
                        IconButton(
                            onClick = { viewModel.showFontSettingsSheet = true },
                            modifier = Modifier.testTag("font_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = appString("font_settings")
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.showControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        // Progress Info Header with Save Recorder button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.showPageProgressModal = true }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sayfa $currentPageNum / $totalPages",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Sayfa Değiştir",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Progress Percent & Save Action
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "%$progressPercent",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.saveCurrentPageAndProgress { page, percent ->
                                            Toast.makeText(
                                                context,
                                                "📌 Sayfa $page (%${(percent * 100).toInt()}) kaydedildi!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kaydet", fontSize = 12.sp)
                                }
                            }
                        }

                        // Interactive Progress Slider
                        Slider(
                            value = currentPageNum.toFloat(),
                            onValueChange = { newVal ->
                                viewModel.goToPage(newVal.toInt() - 1)
                            },
                            valueRange = 1f..totalPages.toFloat(),
                            steps = (totalPages - 2).coerceAtLeast(0),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bottom Action Icons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.prevPage() }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki Sayfa")
                            }

                            IconButton(
                                onClick = { viewModel.showTocSheet = true },
                                modifier = Modifier.testTag("toc_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = appString("toc"))
                            }

                            IconButton(
                                onClick = { viewModel.showPageProgressModal = true },
                                modifier = Modifier.testTag("page_progress_modal_button")
                            ) {
                                Icon(Icons.Default.PinDrop, contentDescription = appString("jump_to_page"))
                            }

                            IconButton(
                                onClick = { viewModel.openSpeedReader() },
                                modifier = Modifier.testTag("speed_reader_button")
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = appString("rsvp_speed_reader"))
                            }

                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    imageVector = if (book?.isFavorite == true) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = appString("favorite"),
                                    tint = if (book?.isFavorite == true) Color(0xFFFFB703) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(onClick = { viewModel.nextPage() }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki Sayfa")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(currentTheme.backgroundColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val width = size.width
                            val x = offset.x
                            // Reader tap zones: Left 22% -> Prev, Right 22% -> Next, Center -> Toggle Controls
                            when {
                                x < width * 0.22f -> viewModel.prevPage()
                                x > width * 0.78f -> viewModel.nextPage()
                                else -> viewModel.toggleControls()
                            }
                        }
                    )
                }
        ) {
            // Main Reading Page Content
            val scrollState = rememberScrollState()

            val isPdfBook = book?.format?.equals("PDF", ignoreCase = true) == true && !book?.fileUri.isNullOrBlank()

            if (isPdfBook && !ttsController.isPlaying) {
                PdfPageView(
                    fileUriString = book!!.fileUri!!,
                    pageIndex = viewModel.currentPageIndex,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .verticalScroll(scrollState)
                ) {
                    // If TTS is active, show text with sentence highlighting
                    if (ttsController.isPlaying) {
                        sentences.forEachIndexed { index, sentence ->
                            val isSpoken = index == ttsController.currentSentenceIndex
                            Text(
                                text = sentence + " ",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = viewModel.fontSizeSp.sp,
                                    lineHeight = (viewModel.fontSizeSp * viewModel.lineHeightMultiplier).sp,
                                    fontFamily = viewModel.selectedFontFamily.composeFont,
                                    textAlign = viewModel.textAlign,
                                    color = if (isSpoken) currentTheme.accentColor else currentTheme.textColor,
                                    fontWeight = if (isSpoken) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = if (isSpoken) {
                                    Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(currentTheme.accentColor.copy(alpha = 0.18f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                } else Modifier
                            )
                        }
                    } else {
                        Text(
                            text = currentPageText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = viewModel.fontSizeSp.sp,
                                lineHeight = (viewModel.fontSizeSp * viewModel.lineHeightMultiplier).sp,
                                fontFamily = viewModel.selectedFontFamily.composeFont,
                                textAlign = viewModel.textAlign,
                                color = currentTheme.textColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Eye Comfort Blue Light Warm Tint Overlay (reduces eye fatigue)
            if (viewModel.eyeComfortEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFA000).copy(alpha = viewModel.eyeComfortWarmth * 0.25f))
                )
            }

            // Discreet bottom page footer when controls are hidden
            if (!viewModel.showControls) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.getCurrentChapterTitle(),
                        style = MaterialTheme.typography.labelSmall,
                        color = currentTheme.textColor.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Sayfa $currentPageNum / $totalPages (%$progressPercent)",
                        style = MaterialTheme.typography.labelSmall,
                        color = currentTheme.textColor.copy(alpha = 0.5f)
                    )
                }
            }

            // Floating TTS Controller Bar (when open)
            if (viewModel.showTtsBar) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (viewModel.showControls) 120.dp else 24.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = appString("tts_title"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Cümle: ${ttsController.currentSentenceIndex + 1} / ${sentences.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { ttsController.prevSentence() }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Önceki Cümle")
                            }

                            IconButton(
                                onClick = {
                                    if (ttsController.isPlaying) ttsController.pause() else ttsController.play()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (ttsController.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Oynat / Durdur",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            IconButton(onClick = { ttsController.nextSentence() }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Sonraki Cümle")
                            }

                            // Speed rate button
                            TextButton(
                                onClick = {
                                    val nextRate = when (ttsController.speechRate) {
                                        0.75f -> 1.0f
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 0.75f
                                    }
                                    ttsController.setRate(nextRate)
                                }
                            ) {
                                Text("${ttsController.speechRate}x")
                            }

                            // Dynamic Language Selector Dropdown
                            var showLangMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showLangMenu = true }) {
                                    Icon(Icons.Default.Translate, contentDescription = "Dil Seçimi")
                                }

                                DropdownMenu(
                                    expanded = showLangMenu,
                                    onDismissRequest = { showLangMenu = false }
                                ) {
                                    ttsController.supportedLanguages.forEach { (locale, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                ttsController.setLanguage(locale)
                                                showLangMenu = false
                                            },
                                            leadingIcon = {
                                                if (ttsController.currentLanguage == locale) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = {
                                ttsController.stop()
                                viewModel.showTtsBar = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = appString("close"))
                            }
                        }
                    }
                }
            }
        }
    }

    // Page Progress & Percentage Recorder Modal
    if (viewModel.showPageProgressModal) {
        PageProgressRecorderDialog(
            currentPage = currentPageNum,
            totalPages = totalPages,
            onDismiss = { viewModel.showPageProgressModal = false },
            onJumpToPage = { targetPage ->
                viewModel.goToPage(targetPage - 1)
                viewModel.showPageProgressModal = false
            },
            onSaveProgress = {
                viewModel.saveCurrentPageAndProgress { page, percent ->
                    Toast.makeText(
                        context,
                        "📌 Sayfa $page (%${(percent * 100).toInt()}) kaydedildi!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                viewModel.showPageProgressModal = false
            }
        )
    }

    // Font & Appearance BottomSheet (Theme Management & Blue Light Filter)
    if (viewModel.showFontSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showFontSettingsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = appString("font_settings"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Theme Presets (Gündüz, Sepya, Gece, AMOLED, Orman, Koyu Gri)
                Text(
                    text = appString("theme_management"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ReadingThemeMode.entries.forEach { theme ->
                        val isSelected = viewModel.currentTheme == theme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.currentTheme = theme }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(theme.backgroundColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aa",
                                    color = theme.textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.title.split(" ").first(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Eye Comfort Mode (Göz Dinlendirici Mavi Işık Filtresi)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appString("eye_comfort"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Gece okumalarında göz yorgunluğunu azaltan sıcak kehribar filtresi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = viewModel.eyeComfortEnabled,
                        onCheckedChange = { viewModel.eyeComfortEnabled = it }
                    )
                }

                if (viewModel.eyeComfortEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sıcaklık", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = viewModel.eyeComfortWarmth,
                            onValueChange = { viewModel.eyeComfortWarmth = it },
                            valueRange = 0.1f..0.8f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Brightness Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appString("brightness"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%${(viewModel.brightnessLevel * 100).toInt()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BrightnessLow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Slider(
                        value = viewModel.brightnessLevel,
                        onValueChange = { viewModel.brightnessLevel = it },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Icon(Icons.Default.BrightnessHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appString("font_size"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${viewModel.fontSizeSp.toInt()} sp",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("A", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = viewModel.fontSizeSp,
                        onValueChange = { viewModel.fontSizeSp = it },
                        valueRange = 12f..32f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Font Family Selector
                Text(
                    text = appString("font_family"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderFontFamily.entries.forEach { font ->
                        FilterChip(
                            selected = viewModel.selectedFontFamily == font,
                            onClick = { viewModel.selectedFontFamily = font },
                            label = { Text(font.label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Alignment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appString("text_align"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        IconButton(
                            onClick = { viewModel.textAlign = TextAlign.Justify },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (viewModel.textAlign == TextAlign.Justify) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.FormatAlignJustify, contentDescription = "İki Yana Yasla")
                        }
                        IconButton(
                            onClick = { viewModel.textAlign = TextAlign.Start },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (viewModel.textAlign == TextAlign.Start) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Sola Yasla")
                        }
                    }
                }
            }
        }
    }

    // Table of Contents (TOC) BottomSheet
    if (viewModel.showTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showTocSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                var selectedTab by remember { mutableIntStateOf(0) }

                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Bölümler") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Yer İmleri (${bookmarks.size})") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.pagination.chapters) { chapter ->
                            val isCurrent = currentPageNum >= chapter.startPage
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.goToPage(chapter.startPage - 1)
                                        viewModel.showTocSheet = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Sayfa ${chapter.startPage}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.4f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bu kitap için eklenmiş yer imi yok.\nSağ üstteki yer imi butonunu kullanarak ekleyebilirsiniz.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(0.6f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(bookmarks) { bookmark ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.goToPage(bookmark.pageNumber - 1)
                                            viewModel.showTocSheet = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(bookmark.colorHex))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Sayfa ${bookmark.pageNumber}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                if (bookmark.userNote.isNotEmpty()) {
                                                    Text(
                                                        text = bookmark.userNote,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteBookmark(bookmark)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Sil",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bookmark Creation Dialog
    if (viewModel.showBookmarkDialog) {
        var noteText by remember { mutableStateOf("") }
        var selectedColor by remember { mutableLongStateOf(0xFF059669) }

        val bookmarkColors = listOf(
            0xFF059669, 0xFF0284C7, 0xFFD97706, 0xFFDC2626, 0xFF7C3AED
        )

        AlertDialog(
            onDismissRequest = { viewModel.showBookmarkDialog = false },
            title = { Text(appString("add_bookmark")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sayfa $currentPageNum - ${viewModel.getCurrentChapterTitle()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Notunuz (İsteğe bağlı)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Text("İm Rengi", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        bookmarkColors.forEach { colorHex ->
                            val isSel = selectedColor == colorHex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .clickable { selectedColor = colorHex }
                                    .border(
                                        width = if (isSel) 3.dp else 0.dp,
                                        color = if (isSel) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addBookmark("", noteText, selectedColor)
                        viewModel.showBookmarkDialog = false
                        Toast.makeText(context, "Yer imi eklendi!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(appString("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showBookmarkDialog = false }) {
                    Text(appString("cancel"))
                }
            }
        )
    }

    // Speed Reader (RSVP) Modal Dialog
    if (viewModel.showSpeedReaderModal) {
        Dialog(onDismissRequest = { viewModel.closeSpeedReader() }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = appString("rsvp_speed_reader"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { viewModel.closeSpeedReader() }) {
                            Icon(Icons.Default.Close, contentDescription = appString("close"))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Word Flasher Area
                    val word = if (viewModel.rsvpWords.isNotEmpty() && viewModel.rsvpCurrentIndex in viewModel.rsvpWords.indices) {
                        viewModel.rsvpWords[viewModel.rsvpCurrentIndex]
                    } else "Başlat"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Word count progress
                    Text(
                        text = "${viewModel.rsvpCurrentIndex + 1} / ${viewModel.rsvpWords.size} kelime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Speed Slider (WPM)
                    Text(
                        text = "Okuma Hızı: ${viewModel.rsvpWpm} Kelime/Dk",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Slider(
                        value = viewModel.rsvpWpm.toFloat(),
                        onValueChange = { viewModel.rsvpWpm = it.toInt() },
                        valueRange = 150f..600f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play / Pause Button
                    Button(
                        onClick = { viewModel.toggleRsvp() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.rsvpIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (viewModel.rsvpIsPlaying) "Durdur" else "Başlat")
                    }
                }
            }
        }
    }
}

@Composable
fun PageProgressRecorderDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    onSaveProgress: () -> Unit
) {
    var inputPageText by remember { mutableStateOf(currentPage.toString()) }
    var sliderValue by remember { mutableFloatStateOf(currentPage.toFloat()) }
    val progressPercent = ((sliderValue / totalPages.toFloat()) * 100).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdded,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = appString("page_recorder_title"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Progress Percentage Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "%$progressPercent",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Sayfa ${sliderValue.toInt()} / $totalPages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Interactive Progress Slider
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        inputPageText = it.toInt().toString()
                    },
                    valueRange = 1f..totalPages.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Stepper Buttons (-10, -1, +1, +10)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val newP = (sliderValue.toInt() - 10).coerceAtLeast(1)
                            sliderValue = newP.toFloat()
                            inputPageText = newP.toString()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("-10")
                    }
                    OutlinedButton(
                        onClick = {
                            val newP = (sliderValue.toInt() - 1).coerceAtLeast(1)
                            sliderValue = newP.toFloat()
                            inputPageText = newP.toString()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("-1")
                    }
                    OutlinedButton(
                        onClick = {
                            val newP = (sliderValue.toInt() + 1).coerceAtMost(totalPages)
                            sliderValue = newP.toFloat()
                            inputPageText = newP.toString()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("+1")
                    }
                    OutlinedButton(
                        onClick = {
                            val newP = (sliderValue.toInt() + 10).coerceAtMost(totalPages)
                            sliderValue = newP.toFloat()
                            inputPageText = newP.toString()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("+10")
                    }
                }

                // Manual Page Input Field
                OutlinedTextField(
                    value = inputPageText,
                    onValueChange = {
                        inputPageText = it
                        val parsed = it.toIntOrNull()
                        if (parsed != null && parsed in 1..totalPages) {
                            sliderValue = parsed.toFloat()
                        }
                    },
                    label = { Text("Doğrudan Sayfa Numarası Girin") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = sliderValue.toInt().coerceIn(1, totalPages)
                    onJumpToPage(target)
                    onSaveProgress()
                }
            ) {
                Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(appString("save_current_page"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(appString("cancel"))
            }
        }
    )
}

@Composable
fun PdfPageView(
    fileUriString: String,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(fileUriString, pageIndex) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(fileUriString, pageIndex) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(fileUriString)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        if (pageIndex in 0 until renderer.pageCount) {
                            renderer.openPage(pageIndex).use { page ->
                                val width = (page.width * 2.2).toInt().coerceAtLeast(100)
                                val height = (page.height * 2.2).toInt().coerceAtLeast(100)
                                val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap = bmp
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfPageView", "Error rendering PDF page: ${e.message}")
            }
        }
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "PDF Sayfa ${pageIndex + 1}",
            modifier = modifier
                .fillMaxSize()
                .padding(8.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
