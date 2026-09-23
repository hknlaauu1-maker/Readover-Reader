package com.example.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.*

enum class ReadingThemeMode(
    val title: String,
    val backgroundColor: Color,
    val textColor: Color,
    val accentColor: Color
) {
    DAY("Gündüz (Beyaz)", ReadingDayBg, ReadingDayText, EmeraldPrimary),
    SEPIA("Sepya (Dinlendirici)", ReadingSepiaBg, ReadingSepiaText, Color(0xFFB45309)),
    NIGHT("Gece (Karanlık)", ReadingNightBg, ReadingNightText, Color(0xFF34D399)),
    AMOLED("AMOLED (Tam Siyah)", ReadingAmoledBg, ReadingAmoledText, Color(0xFF10B981)),
    FOREST("Orman Yeşili (Doğal)", ReadingForestBg, ReadingForestText, Color(0xFF6EE7B7)),
    SLATE("Koyu Gri", ReadingSlateBg, ReadingSlateText, Color(0xFF38BDF8))
}

enum class ReaderFontFamily(val label: String, val composeFont: FontFamily) {
    SERIF("Serif (Klasik Kitap)", FontFamily.Serif),
    SANS("Sans (Modern Okuma)", FontFamily.SansSerif),
    MONO("Daktilo (Monospace)", FontFamily.Monospace),
    CURSIVE("Edebi (El Yazısı)", FontFamily.Cursive)
}

data class ChapterInfo(
    val index: Int,
    val title: String,
    val startPage: Int
)

data class BookPagination(
    val pages: List<String>,
    val chapters: List<ChapterInfo>
)

object BookReaderEngine {

    /**
     * Splits raw book content into paginated text chunks and builds Table of Contents (TOC).
     */
    fun paginate(content: String, charsPerPage: Int = 1100): BookPagination {
        if (content.isBlank()) {
            return BookPagination(
                pages = listOf("Bu belgede görüntülenecek metin bulunamadı."),
                chapters = listOf(ChapterInfo(0, "Giriş", 1))
            )
        }

        val lines = content.lines()
        val pagesList = mutableListOf<String>()
        val chaptersList = mutableListOf<ChapterInfo>()

        var currentPageText = StringBuilder()
        var currentChapterTitle = "Giriş"
        var chapterIndex = 0

        chaptersList.add(ChapterInfo(chapterIndex++, currentChapterTitle, 1))

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                // Chapter header detected
                val chapterName = trimmed.replace("=", "").trim()
                if (currentPageText.isNotEmpty()) {
                    pagesList.add(currentPageText.toString().trim())
                    currentPageText = StringBuilder()
                }
                currentChapterTitle = chapterName
                chaptersList.add(ChapterInfo(chapterIndex++, chapterName, pagesList.size + 1))
                continue
            }

            if (currentPageText.length + line.length > charsPerPage && currentPageText.isNotEmpty()) {
                pagesList.add(currentPageText.toString().trim())
                currentPageText = StringBuilder()
            }
            currentPageText.append(line).append("\n")
        }

        if (currentPageText.isNotEmpty()) {
            pagesList.add(currentPageText.toString().trim())
        }

        val finalPages = if (pagesList.isEmpty()) listOf(content) else pagesList
        val uniqueChapters = chaptersList.distinctBy { it.title }

        return BookPagination(
            pages = finalPages,
            chapters = uniqueChapters
        )
    }

    /**
     * Splits text into sentences for Text-To-Speech.
     */
    fun extractSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val raw = text.split(Regex("(?<=[.!?])\\s+"))
        return raw.map { it.trim() }.filter { it.isNotBlank() }
    }
}
