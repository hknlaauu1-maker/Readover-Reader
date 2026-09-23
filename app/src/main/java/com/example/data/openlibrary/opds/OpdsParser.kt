package com.example.data.openlibrary.opds

import android.util.Xml
import com.example.data.openlibrary.OpenBookItem
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object OpdsParser {

    /**
     * Parses an OPDS Atom XML catalog feed and extracts a list of OpenBookItem objects.
     */
    fun parseFeed(inputStream: InputStream, sourceName: String = "StandardEbooks"): List<OpenBookItem> {
        val items = mutableListOf<OpenBookItem>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentTitle = ""
            var currentAuthor = ""
            var currentSummary = ""
            var currentCoverUrl: String? = null
            var currentDownloadUrl: String? = null
            var currentFormat = "EPUB"
            var inEntry = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("entry", ignoreCase = true)) {
                            inEntry = true
                            currentTitle = ""
                            currentAuthor = ""
                            currentSummary = ""
                            currentCoverUrl = null
                            currentDownloadUrl = null
                            currentFormat = "EPUB"
                        } else if (inEntry) {
                            when (name.lowercase()) {
                                "title" -> currentTitle = parser.nextText().trim()
                                "name" -> if (currentAuthor.isEmpty()) currentAuthor = parser.nextText().trim()
                                "summary", "content" -> currentSummary = parser.nextText().trim().take(300)
                                "link" -> {
                                    val rel = parser.getAttributeValue(null, "rel") ?: ""
                                    val href = parser.getAttributeValue(null, "href") ?: ""
                                    val type = parser.getAttributeValue(null, "type") ?: ""

                                    if (rel.contains("image") || rel.contains("thumbnail") || type.contains("image")) {
                                        if (currentCoverUrl == null && href.isNotBlank()) {
                                            currentCoverUrl = href
                                        }
                                    } else if (rel.contains("acquisition") || type.contains("epub") || type.contains("text/html") || type.contains("pdf")) {
                                        if (currentDownloadUrl == null && href.isNotBlank()) {
                                            currentDownloadUrl = href
                                            currentFormat = when {
                                                type.contains("pdf") -> "PDF"
                                                type.contains("html") -> "HTML"
                                                else -> "EPUB"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (name.equals("entry", ignoreCase = true) && inEntry) {
                            if (currentTitle.isNotBlank()) {
                                items.add(
                                    OpenBookItem(
                                        id = "opds-${items.size}-${currentTitle.hashCode()}",
                                        title = currentTitle,
                                        author = if (currentAuthor.isBlank()) "Bilinmeyen Yazar" else currentAuthor,
                                        coverUrl = currentCoverUrl,
                                        category = "Klasik & Açık Kaynak",
                                        language = "İngilizce",
                                        format = currentFormat,
                                        source = sourceName,
                                        description = if (currentSummary.isBlank()) "$currentTitle - $sourceName kütüphanesi açık erişim eseri." else currentSummary,
                                        downloadUrl = currentDownloadUrl,
                                        estimatedPages = 180
                                    )
                                )
                            }
                            inEntry = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }
}
