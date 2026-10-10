package com.example.data.model

import android.net.Uri

data class DiscoveredFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val extension: String,
    val isAudio: Boolean,
    var isSelected: Boolean = true
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "Bilinmiyor"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f KB", kb)
            }
        }
}
