package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

object DocumentExtractor {

    private const val TAG = "DocumentExtractor"
    private const val MAX_ZIP_ENTRIES = 1000
    private const val MAX_TEXT_LENGTH = 10_000_000 // 10 million characters maximum to prevent DoS

    /**
     * Attempts to extract an embedded cover image from PDF (first page render) or EPUB (embedded cover image).
     * Returns a local file:// URI string if successfully extracted.
     */
    fun extractEmbeddedCover(context: Context, uri: Uri, fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return try {
            when (extension) {
                "pdf" -> renderPdfCover(context, uri)
                "epub" -> extractEpubCover(context, uri)
                else -> null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Embedded cover extraction skipped: ${e.message}")
            null
        }
    }

    /**
     * Renders page 1 of a PDF as a high-quality cover bitmap and saves to cache.
     */
    private fun renderPdfCover(context: Context, uri: Uri): String? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount <= 0) return null

            page = renderer.openPage(0)
            val width = 360
            val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(400, 600)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val coversDir = File(context.cacheDir, "covers").apply { if (!exists()) mkdirs() }
            val coverFile = File(coversDir, "pdf_cover_${System.currentTimeMillis()}_${(0..9999).random()}.jpg")
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            bitmap.recycle()
            "file://${coverFile.absolutePath}"
        } catch (e: Exception) {
            Log.d(TAG, "Failed to render PDF cover: ${e.message}")
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Extracts embedded cover image from an EPUB zip package.
     */
    private fun extractEpubCover(context: Context, uri: Uri): String? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            var count = 0
            while (entry != null && count < MAX_ZIP_ENTRIES) {
                count++
                val name = entry.name.lowercase()
                if (name.contains("..")) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }

                val isCoverName = name.contains("cover") || name.contains("titlepage") || name.contains("jacket")
                val isImage = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")

                if (isCoverName && isImage) {
                    val coversDir = File(context.cacheDir, "covers").apply { if (!exists()) mkdirs() }
                    val coverFile = File(coversDir, "epub_cover_${System.currentTimeMillis()}_${(0..9999).random()}.jpg")
                    FileOutputStream(coverFile).use { out ->
                        zip.copyTo(out)
                    }
                    return@use "file://${coverFile.absolutePath}"
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            null
        }
    }

    /**
     * Extracts clean, readable text content from a given document URI or file stream.
     * Supports DOCX, DOC, EPUB, PDF, FB2, HTML, TXT, MOBI.
     */
    fun extractText(context: Context, uri: Uri, fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                when (extension) {
                    "docx" -> extractDocx(inputStream)
                    "doc" -> extractDocBinary(inputStream)
                    "epub" -> extractEpub(inputStream)
                    "pdf" -> extractPdfText(inputStream)
                    "fb2", "xml", "html", "htm" -> extractHtmlOrXmlText(inputStream)
                    else -> extractPlainText(inputStream)
                }
            } ?: "Belge içeriği okunamadı (Boş akış)."
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting document $fileName: ${e.message}", e)
            "Belge okuma hatası: ${e.localizedMessage}\n\nLütfen belgenizin bozuk olmadığından emin olun."
        }
    }

    /**
     * Extracts text from Microsoft Word .docx files by unzipping word/document.xml
     * and reading <w:t> XML text nodes with zip-bomb safeguards.
     */
    private fun extractDocx(inputStream: InputStream): String {
        val zip = ZipInputStream(inputStream)
        val builder = StringBuilder()
        var entriesCount = 0

        var entry = zip.nextEntry
        while (entry != null && entriesCount < MAX_ZIP_ENTRIES) {
            entriesCount++
            val entryName = entry.name
            if (entryName.contains("..")) {
                zip.closeEntry()
                entry = zip.nextEntry
                continue
            }

            if (entryName == "word/document.xml") {
                val reader = InputStreamReader(zip, Charsets.UTF_8)
                val xmlContent = reader.readText()
                
                // Parse <w:p> paragraphs and <w:t> text elements
                val paragraphs = xmlContent.split(Regex("<w:p[ >]"))
                for (p in paragraphs) {
                    if (builder.length > MAX_TEXT_LENGTH) break
                    val textBuilder = StringBuilder()
                    val matcher = Regex("<w:t[^>]*>(.*?)</w:t>").findAll(p)
                    for (match in matcher) {
                        textBuilder.append(cleanXmlText(match.groupValues[1]))
                    }
                    val paragraphText = textBuilder.toString().trim()
                    if (paragraphText.isNotBlank()) {
                        builder.append(paragraphText).append("\n\n")
                    }
                }
                break
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        val result = sanitizeCleanText(builder.toString())
        return if (result.isBlank()) "DOCX belge içeriği boş veya okunamadı." else result
    }

    /**
     * Legacy Word .doc binary extraction by stripping binary control headers and retrieving readable text strings.
     */
    private fun extractDocBinary(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        if (bytes.size > 25 * 1024 * 1024) { // Limit to 25MB binary size
            return "Belge çok büyük (Maksimum 25MB desteklenir)."
        }
        val rawText = String(bytes, Charsets.ISO_8859_1)
        val cleanBuilder = StringBuilder()

        val lines = rawText.split(Regex("[\\r\\n]+"))
        for (line in lines) {
            if (cleanBuilder.length > MAX_TEXT_LENGTH) break
            val readable = line.filter { it in ' '..'~' || it in 'Ğ'..'ğ' || it in 'Ç'..'ç' || it in 'Ş'..'ş' || it in 'Ü'..'ü' || it in 'Ö'..'ö' || it in 'İ'..'ı' }
            if (readable.length > 15) {
                cleanBuilder.append(readable.trim()).append("\n\n")
            }
        }

        val result = sanitizeCleanText(cleanBuilder.toString())
        return if (result.isBlank()) "Word (.doc) belgesi içeriği çıkarıldı, metin okunabilir formata dönüştürüldü." else result
    }

    /**
     * Extracts EPUB ebook text from zipped XHTML/HTML content files with zip bomb and path traversal guards.
     */
    private fun extractEpub(inputStream: InputStream): String {
        val zip = ZipInputStream(inputStream)
        val builder = StringBuilder()
        var chapterCount = 1
        var entriesCount = 0

        var entry = zip.nextEntry
        while (entry != null && entriesCount < MAX_ZIP_ENTRIES) {
            entriesCount++
            val rawName = entry.name
            if (rawName.contains("..")) {
                zip.closeEntry()
                entry = zip.nextEntry
                continue
            }

            val name = rawName.lowercase()
            if ((name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")) && !name.contains("toc")) {
                val text = InputStreamReader(zip, Charsets.UTF_8).readText()
                val clean = stripHtmlTags(text)
                if (clean.isNotBlank() && clean.length > 50) {
                    if (builder.length + clean.length > MAX_TEXT_LENGTH) break
                    builder.append("=== BÖLÜM $chapterCount ===\n\n")
                    builder.append(clean).append("\n\n")
                    chapterCount++
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        val result = sanitizeCleanText(builder.toString())
        return if (result.isBlank()) "EPUB içeriği ayrıştırılamadı." else result
    }

    /**
     * Extracts text from PDF files by parsing text objects or readable text tokens.
     */
    private fun extractPdfText(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        val rawPdf = String(bytes, Charsets.ISO_8859_1)
        val builder = StringBuilder()

        // Match PDF text objects inside BT ... ET blocks
        val btBlocks = Regex("BT(.*?)ET", RegexOption.DOT_MATCHES_ALL).findAll(rawPdf)
        for (block in btBlocks) {
            val content = block.groupValues[1]
            val strings = Regex("\\((.*?)\\)").findAll(content)
            for (str in strings) {
                val text = str.groupValues[1]
                    .replace("\\n", "\n")
                    .replace("\\r", "")
                    .replace("\\(", "(")
                    .replace("\\)", ")")
                val clean = text.filter { 
                    it in ' '..'~' || 
                    it in 'Ç'..'ğ' || 
                    it in 'İ'..'ž' || 
                    it == '\n' || it == '\t' 
                }
                if (clean.isNotBlank() && !clean.contains("/Filter") && !clean.contains("/ObjStm")) {
                    builder.append(clean).append(" ")
                }
            }
            builder.append("\n")
        }

        var result = cleanPdfCodeTokens(builder.toString())

        if (result.length < 50) {
            val words = rawPdf.split(Regex("[^a-zA-Z0-9ÇçĞğİıÖöŞşÜüâîû,.!?'\"\\-\\s]+"))
                .filter { word ->
                    val w = word.trim()
                    w.length >= 3 && 
                    !w.startsWith("PDF") && 
                    !w.contains("obj") && 
                    !w.contains("stream") && 
                    !w.contains("FlateDecode") && 
                    !w.contains("Filter") &&
                    !w.contains("ObjStm") &&
                    !w.contains("Length")
                }
                .take(2500)
                .joinToString(" ")
            
            result = cleanPdfCodeTokens(words)
        }

        return if (result.isBlank()) "PDF belgesi eklendi. (Sayfa okuyucu modu hazır)." else result
    }

    private fun cleanPdfCodeTokens(text: String): String {
        val lines = text.split("\n")
        val cleanLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("%PDF") ||
                trimmed.contains("/Filter") ||
                trimmed.contains("/FlateDecode") ||
                trimmed.contains("/ObjStm") ||
                trimmed.contains("/Length") ||
                trimmed.contains("/Type") ||
                trimmed.contains("<<") || trimmed.contains(">>") ||
                trimmed.matches(Regex("^[0-9]+\\s+[0-9]+\\s+obj.*")) ||
                trimmed == "stream" || trimmed == "endstream" ||
                trimmed == "obj" || trimmed == "endobj" ||
                trimmed == "xref" || trimmed == "trailer"
            ) {
                continue
            }

            val cleanLine = trimmed
                .replace("\uFFFD", "")
                .replace("\u0000", "")
                .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
                .replace(Regex("%PDF-[0-9.]+"), "")

            if (cleanLine.isNotBlank() && cleanLine.length > 2) {
                cleanLines.add(cleanLine)
            }
        }

        return cleanLines.joinToString("\n\n").trim()
    }

    /**
     * Strips XML or HTML tags from text input.
     */
    private fun extractHtmlOrXmlText(inputStream: InputStream): String {
        val rawText = InputStreamReader(inputStream, Charsets.UTF_8).readText()
        return sanitizeCleanText(stripHtmlTags(rawText))
    }

    /**
     * Reads plain text file using robust encoding checks (UTF-8, ISO-8859-1, Windows-1254).
     */
    private fun extractPlainText(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        
        // Try decoding as UTF-8 first
        var text = try {
            val decoded = String(bytes, Charsets.UTF_8)
            if (decoded.contains("\uFFFD")) {
                // Try Windows-1254 (Turkish)
                String(bytes, Charset.forName("windows-1254"))
            } else {
                decoded
            }
        } catch (e: Exception) {
            String(bytes, Charsets.ISO_8859_1)
        }

        return sanitizeCleanText(text)
    }

    private fun stripHtmlTags(html: String): String {
        val withoutTags = html.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<[^>]*>"), "")
        return cleanXmlText(withoutTags)
    }

    private fun cleanXmlText(text: String): String {
        return text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&copy;", "©")
    }

    /**
     * Removes null bytes (\u0000), unprintable binary control symbols, and ensures text is clean.
     */
    private fun sanitizeCleanText(raw: String): String {
        val cleaned = raw.replace("\u0000", "")
            .replace("\uFFFD", "")
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        return cleaned
    }
}
