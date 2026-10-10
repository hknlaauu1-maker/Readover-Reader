package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DiscoveredFile
import com.example.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultsDialog(
    isOpen: Boolean,
    discoveredFiles: List<DiscoveredFile>,
    isScanning: Boolean,
    onDismiss: () -> Unit,
    onConfirmImport: (List<DiscoveredFile>) -> Unit
) {
    if (!isOpen) return

    var fileList by remember(discoveredFiles) { mutableStateOf(discoveredFiles) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("TÜMÜ") } // TÜMÜ, KİTAP, SES

    val filteredList = remember(fileList, searchQuery, filterType) {
        fileList.filter { item ->
            val matchesQuery = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            val matchesType = when (filterType) {
                "KİTAP" -> !item.isAudio
                "SES" -> item.isAudio
                else -> true
            }
            matchesQuery && matchesType
        }
    }

    val selectedCount = fileList.count { it.isSelected }
    val allSelected = fileList.isNotEmpty() && fileList.all { it.isSelected }

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
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tarama Sonuçları",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = if (isScanning) "Diskteki ve klasördeki dosyalar taranıyor..." else "İçe aktarılacak dosyaları listeden seçin",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("scan_dialog_close_btn")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat")
                            }
                        },
                        actions = {
                            if (fileList.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        val targetState = !allSelected
                                        fileList = fileList.map { it.copy(isSelected = targetState) }
                                    }
                                ) {
                                    Text(
                                        text = if (allSelected) "Tümünü Kaldır" else "Tümünü Seç",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$selectedCount / ${fileList.size} Dosya Seçildi",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Kitaplık tablarına otomatik dağıtılacaktır",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    val toImport = fileList.filter { it.isSelected }
                                    onConfirmImport(toImport)
                                },
                                enabled = selectedCount > 0 && !isScanning,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("btn_confirm_import_scan")
                            ) {
                                Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("İçe Aktar ($selectedCount)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Yerel depolama alanı taranıyor...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "EPUB, PDF, MP3, M4B, WAV, TXT dosyaları aranıyor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (fileList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FolderOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Desteklenen Dosya Bulunamadı",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Seçilen konumda EPUB, PDF, MP3 veya WAV formatında belge/ses dosyası tespit edilemedi.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Search and Filter Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Dosya adı ara...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            // Filter Chips (TÜMÜ, KİTAP, SES)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = filterType == "TÜMÜ",
                                    onClick = { filterType = "TÜMÜ" },
                                    label = { Text("Tümü (${fileList.size})", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = filterType == "KİTAP",
                                    onClick = { filterType = "KİTAP" },
                                    label = { Text("Kitap (${fileList.count { !it.isAudio }})", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = filterType == "SES",
                                    onClick = { filterType = "SES" },
                                    label = { Text("Ses (${fileList.count { it.isAudio }})", fontSize = 11.sp) }
                                )
                            }
                        }

                        // Checklist List
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredList, key = { it.uri.toString() }) { fileItem ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            fileList = fileList.map {
                                                if (it.uri == fileItem.uri) it.copy(isSelected = !it.isSelected) else it
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (fileItem.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (fileItem.isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = fileItem.isSelected,
                                            onCheckedChange = { checked ->
                                                fileList = fileList.map {
                                                    if (it.uri == fileItem.uri) it.copy(isSelected = checked) else it
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // File Icon
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (fileItem.isAudio) EmeraldPrimary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.secondaryContainer
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (fileItem.isAudio) Icons.Default.Headphones else Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = if (fileItem.isAudio) EmeraldPrimary else MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fileItem.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (fileItem.isAudio) EmeraldPrimary else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = fileItem.extension.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Text(
                                                    text = fileItem.formattedSize,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }
}
