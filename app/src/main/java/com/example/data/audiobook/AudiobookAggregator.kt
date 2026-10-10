package com.example.data.audiobook

import android.util.Log
import android.util.Xml
import com.example.data.model.OnlineAudiobookItem
import com.example.util.i18n.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object AudiobookAggregator {
    private const val TAG = "AudiobookAggregator"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Caching layer for fast dynamic search
    private val cacheMap = ConcurrentHashMap<String, List<OnlineAudiobookItem>>()

    /**
     * Parallel combined search across LibriVox, Internet Archive, and Loyal Books OPDS.
     */
    suspend fun searchAll(query: String, lang: AppLanguage = AppLanguage.TURKISH): List<OnlineAudiobookItem> = coroutineScope {
        val trimmedQuery = query.trim()
        val cacheKey = "${trimmedQuery.lowercase()}_${lang.code}"

        if (cacheMap.containsKey(cacheKey) && cacheMap[cacheKey]!!.isNotEmpty()) {
            return@coroutineScope cacheMap[cacheKey]!!
        }

        if (trimmedQuery.isEmpty()) {
            // Provide curated default popular audiobooks to populate on first open
            val defaults = getCuratedDefaultAudiobooks(lang)
            cacheMap[cacheKey] = defaults
            return@coroutineScope defaults
        }

        // Run search calls in parallel using async coroutines
        val librivoxDeferred = async { fetchLibriVox(trimmedQuery, lang) }
        val archiveDeferred = async { fetchInternetArchive(trimmedQuery, lang) }
        val loyalBooksDeferred = async { fetchLoyalBooks(trimmedQuery) }

        val librivoxList = try { librivoxDeferred.await() } catch (e: Exception) { emptyList() }
        val archiveList = try { archiveDeferred.await() } catch (e: Exception) { emptyList() }
        val loyalBooksList = try { loyalBooksDeferred.await() } catch (e: Exception) { emptyList() }

        // Combine, prioritize search relevancy, distinct by normalized title
        val combined = (archiveList + librivoxList + loyalBooksList)
            .distinctBy { "${it.title.lowercase()}_${it.author.lowercase()}" }
            .sortedByDescending { 
                // Prioritize matching title directly
                it.title.contains(trimmedQuery, ignoreCase = true)
            }

        cacheMap[cacheKey] = combined
        combined
    }

    /**
     * Resolves the actual MP3 streaming file from Internet Archive item metadata.
     */
    suspend fun resolveArchiveAudioUrl(identifier: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://archive.org/metadata/$identifier"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return@use ""
                    val root = JSONObject(jsonStr)
                    val files = root.optJSONArray("files") ?: return@use ""
                    
                    // Look for the first MP3 file
                    for (i in 0 until files.length()) {
                        val fileObj = files.getJSONObject(i)
                        val name = fileObj.optString("name", "")
                        if (name.endsWith(".mp3", ignoreCase = true)) {
                            return@withContext "https://archive.org/download/$identifier/$name"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving Archive metadata for $identifier: ${e.message}")
        }
        // Fallback standard direct download URL
        "https://archive.org/download/$identifier/${identifier}_64kb.mp3"
    }

    /**
     * Parsed from standard LibriVox RSS to resolve the first streamable mp3 file
     */
    suspend fun resolveLibrivoxRssUrl(rssUrl: String): String = withContext(Dispatchers.IO) {
        if (rssUrl.isBlank()) return@withContext ""
        try {
            val request = Request.Builder().url(rssUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xmlBytes = response.body?.bytes() ?: return@use ""
                    val parser = Xml.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(ByteArrayInputStream(xmlBytes), null)

                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && parser.name.equals("enclosure", ignoreCase = true)) {
                            val url = parser.getAttributeValue(null, "url") ?: ""
                            if (url.endsWith(".mp3", ignoreCase = true) || url.contains(".mp3")) {
                                return@withContext url
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "RSS parse failed for $rssUrl: ${e.message}")
        }
        ""
    }

    private fun getLanguageEnglishName(lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.TURKISH -> "Turkish"
            AppLanguage.ENGLISH -> "English"
            AppLanguage.RUSSIAN -> "Russian"
            AppLanguage.GERMAN -> "German"
            AppLanguage.FRENCH -> "French"
            AppLanguage.SPANISH -> "Spanish"
            AppLanguage.ITALIAN -> "Italian"
            AppLanguage.ARABIC -> "Arabic"
            AppLanguage.JAPANESE -> "Japanese"
            AppLanguage.INDONESIAN -> "Indonesian"
            AppLanguage.CHINESE -> "Chinese"
        }
    }

    private fun parseLibriVoxJson(books: org.json.JSONArray, list: MutableList<OnlineAudiobookItem>) {
        for (i in 0 until books.length()) {
            val book = books.getJSONObject(i)
            val id = book.optString("id", "")
            val title = book.optString("title", "Unknown Audiobook")
            val desc = book.optString("description", "")
            val totalTime = book.optLong("totaltimesecs", 0L)
            val rssUrl = book.optString("url_rss", "")

            val authorsArr = book.optJSONArray("authors")
            var author = "LibriVox Volunteers"
            if (authorsArr != null && authorsArr.length() > 0) {
                val firstAuthor = authorsArr.getJSONObject(0)
                val first = firstAuthor.optString("first_name", "")
                val last = firstAuthor.optString("last_name", "")
                author = "$first $last".trim()
            }

            val coverUrl = "https://archive.org/services/img/librivox_audiobook_cover_art"

            list.add(
                OnlineAudiobookItem(
                    id = "librivox-$id",
                    title = title,
                    author = author,
                    narrator = "LibriVox Volunteers",
                    coverImageUrl = coverUrl,
                    audioUrl = rssUrl,
                    source = "LibriVox",
                    description = desc,
                    durationMs = totalTime * 1000L,
                    category = "Kamu Malı (Public Domain)"
                )
            )
        }
    }

    private fun parseArchiveDocs(docs: org.json.JSONArray, list: MutableList<OnlineAudiobookItem>) {
        for (i in 0 until docs.length()) {
            val doc = docs.getJSONObject(i)
            val identifier = doc.optString("identifier", "")
            val title = doc.optString("title", "Internet Archive Audio")
            val creator = doc.optString("creator", "LibriVox Volunteers")
            val downloads = doc.optInt("downloads", 0)
            val desc = doc.optString("description", "")

            val coverUrl = "https://archive.org/services/img/$identifier"

            list.add(
                OnlineAudiobookItem(
                    id = "archive-$identifier",
                    title = title,
                    author = creator,
                    narrator = "LibriVox Volunteers",
                    coverImageUrl = coverUrl,
                    audioUrl = identifier,
                    source = "Internet Archive",
                    description = desc,
                    durationMs = 0L,
                    category = "Kamu Malı (Downloads: $downloads)"
                )
            )
        }
    }

    /**
     * Source 1: LibriVox API Search (by Title, filtered by selected language with dual-fallback)
     */
    private suspend fun fetchLibriVox(query: String, lang: AppLanguage): List<OnlineAudiobookItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OnlineAudiobookItem>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val englishLangName = getLanguageEnglishName(lang)
            var url = "https://librivox.org/api/feed/audiobooks/?title=$encoded&language=$englishLangName&format=json"
            
            var request = Request.Builder().url(url).build()
            var responseSuccess = false
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: ""
                    if (jsonStr.trim().startsWith("{")) {
                        val root = JSONObject(jsonStr)
                        val books = root.optJSONArray("books")
                        if (books != null && books.length() > 0) {
                            parseLibriVoxJson(books, list)
                            responseSuccess = true
                        }
                    }
                }
            }
            
            // Fallback: If no localized results were found, query globally (without language filter)
            if (!responseSuccess && lang != AppLanguage.ENGLISH) {
                url = "https://librivox.org/api/feed/audiobooks/?title=$encoded&format=json"
                request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonStr = response.body?.string() ?: ""
                        if (jsonStr.trim().startsWith("{")) {
                            val root = JSONObject(jsonStr)
                            val books = root.optJSONArray("books")
                            if (books != null) {
                                parseLibriVoxJson(books, list)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "LibriVox fetch error: ${e.message}")
        }
        list
    }

    /**
     * Source 2: Internet Archive (Advanced Search with LibriVox & selected language filter with dual-fallback)
     */
    private suspend fun fetchInternetArchive(query: String, lang: AppLanguage): List<OnlineAudiobookItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OnlineAudiobookItem>()
        try {
            // Include language filter in query (e.g. AND language:turkish or language:tr)
            val langQuery = " AND (language:${lang.code} OR language:${getLanguageEnglishName(lang).lowercase()})"
            val encodedQuery = URLEncoder.encode("collection:(librivox) AND mediatype:(audio) AND (title:($query) OR creator:($query))$langQuery", "UTF-8")
            var url = "https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier,title,creator,downloads,description&sort[]=downloads+desc&rows=15&output=json"

            var request = Request.Builder().url(url).build()
            var responseSuccess = false

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: ""
                    val root = JSONObject(jsonStr)
                    val respObj = root.optJSONObject("response")
                    val docs = respObj?.optJSONArray("docs")
                    if (docs != null && docs.length() > 0) {
                        parseArchiveDocs(docs, list)
                        responseSuccess = true
                    }
                }
            }

            // Fallback to global search if no localized books found
            if (!responseSuccess && lang != AppLanguage.ENGLISH) {
                val fallbackEncodedQuery = URLEncoder.encode("collection:(librivox) AND mediatype:(audio) AND (title:($query) OR creator:($query))", "UTF-8")
                url = "https://archive.org/advancedsearch.php?q=$fallbackEncodedQuery&fl[]=identifier,title,creator,downloads,description&sort[]=downloads+desc&rows=15&output=json"
                request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonStr = response.body?.string() ?: ""
                        val root = JSONObject(jsonStr)
                        val respObj = root.optJSONObject("response")
                        val docs = respObj?.optJSONArray("docs")
                        if (docs != null) {
                            parseArchiveDocs(docs, list)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Internet Archive fetch failed: ${e.message}")
        }
        list
    }

    /**
     * Source 3: Loyal Books OPDS Search / Parser
     */
    private suspend fun fetchLoyalBooks(query: String): List<OnlineAudiobookItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OnlineAudiobookItem>()
        try {
            // Since loyalbooks.com/opds is standard Atom, we parse fiction category and filter on query locally for extreme speed and robustness.
            val url = "https://www.loyalbooks.com/opds/category/fiction"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xmlBytes = response.body?.bytes() ?: return@use
                    val parser = Xml.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(ByteArrayInputStream(xmlBytes), null)

                    var eventType = parser.eventType
                    var inEntry = false
                    var currentTitle = ""
                    var currentAuthor = ""
                    var currentSummary = ""
                    var currentCover: String? = null
                    var currentAudio: String? = null

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val name = parser.name
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (name.equals("entry", ignoreCase = true)) {
                                    inEntry = true
                                    currentTitle = ""
                                    currentAuthor = ""
                                    currentSummary = ""
                                    currentCover = null
                                    currentAudio = null
                                } else if (inEntry) {
                                    when (name.lowercase()) {
                                        "title" -> currentTitle = parser.nextText().trim()
                                        "name" -> if (currentAuthor.isEmpty()) currentAuthor = parser.nextText().trim()
                                        "summary", "content" -> currentSummary = parser.nextText().trim()
                                        "link" -> {
                                            val rel = parser.getAttributeValue(null, "rel") ?: ""
                                            val href = parser.getAttributeValue(null, "href") ?: ""
                                            val type = parser.getAttributeValue(null, "type") ?: ""
                                            if (rel.contains("image") || type.contains("image")) {
                                                currentCover = href
                                            } else if (type.contains("audio/mpeg") || href.endsWith(".mp3")) {
                                                currentAudio = href
                                            }
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (name.equals("entry", ignoreCase = true) && inEntry) {
                                    if (currentTitle.isNotBlank() && (currentTitle.contains(query, ignoreCase = true) || currentAuthor.contains(query, ignoreCase = true))) {
                                        list.add(
                                            OnlineAudiobookItem(
                                                id = "loyalbooks-${currentTitle.hashCode()}",
                                                title = currentTitle,
                                                author = if (currentAuthor.isBlank()) "Loyal Books" else currentAuthor,
                                                narrator = "Bilinmeyen Seslendiren",
                                                coverImageUrl = currentCover,
                                                audioUrl = currentAudio ?: "",
                                                source = "Loyal Books",
                                                description = currentSummary,
                                                durationMs = 0L,
                                                category = "Creative Commons"
                                            )
                                        )
                                    }
                                    inEntry = false
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Loyal Books OPDS parse failure: ${e.message}")
        }
        list
    }

    /**
     * Provide a selection of world classic audiobooks on first load when search is empty (Localized to selected language)
     */
    fun getCuratedDefaultAudiobooks(lang: AppLanguage): List<OnlineAudiobookItem> {
        val list = mutableListOf<OnlineAudiobookItem>()
        if (lang == AppLanguage.TURKISH) {
            // Localized Turkish Masterpieces
            list.add(
                OnlineAudiobookItem(
                    id = "archive-ataturk_nutuk_0811_librivox",
                    title = "Nutuk (1. Bölüm)",
                    author = "Mustafa Kemal Atatürk",
                    narrator = "LibriVox Gönüllüleri",
                    coverImageUrl = "https://archive.org/services/img/ataturk_nutuk_0811_librivox",
                    audioUrl = "ataturk_nutuk_0811_librivox",
                    source = "Internet Archive",
                    description = "Mustafa Kemal Atatürk'ün Kurtuluş Savaşı ve cumhuriyetin kuruluş dönemini bizzat anlattığı ölümsüz eseri.",
                    durationMs = 7200000L,
                    category = "Kamu Malı (Public Domain)"
                )
            )
            list.add(
                OnlineAudiobookItem(
                    id = "archive-eylul_1303_librivox",
                    title = "Eylül",
                    author = "Mehmet Rauf",
                    narrator = "LibriVox Gönüllüleri",
                    coverImageUrl = "https://archive.org/services/img/eylul_1303_librivox",
                    audioUrl = "eylul_1303_librivox",
                    source = "Internet Archive",
                    description = "Mehmet Rauf'un edebiyatımızın ilk psikolojik romanı kabul edilen başyapıtı.",
                    durationMs = 8600000L,
                    category = "Kamu Malı (Public Domain)"
                )
            )
            list.add(
                OnlineAudiobookItem(
                    id = "archive-araba_sevdasi_1409_librivox",
                    title = "Araba Sevdası",
                    author = "Recaizade Mahmut Ekrem",
                    narrator = "LibriVox Gönüllüleri",
                    coverImageUrl = "https://archive.org/services/img/araba_sevdasi_1409_librivox",
                    audioUrl = "araba_sevdasi_1409_librivox",
                    source = "Internet Archive",
                    description = "Türk edebiyatının ilk realist romanı olan, Batılılaşmayı yanlış anlayan bir gencin hikayesi.",
                    durationMs = 9100000L,
                    category = "Kamu Malı (Public Domain)"
                )
            )
        } else {
            // Standard Global Classics for English/Other languages
            list.add(
                OnlineAudiobookItem(
                    id = "archive-war_and_peace_01_librivox",
                    title = "War and Peace",
                    author = "Leo Tolstoy",
                    narrator = "LibriVox Volunteers",
                    coverImageUrl = "https://archive.org/services/img/war_and_peace_01_librivox",
                    audioUrl = "war_and_peace_01_librivox",
                    source = "Internet Archive",
                    description = "Leo Tolstoy's classic masterpiece narrated beautifully by LibriVox volunteers.",
                    durationMs = 7800000L,
                    category = "Kamu Malı (Public Domain)"
                )
            )
            list.add(
                OnlineAudiobookItem(
                    id = "archive-pride_and_prejudice_librivox",
                    title = "Pride and Prejudice",
                    author = "Jane Austen",
                    narrator = "LibriVox Volunteers",
                    coverImageUrl = "https://archive.org/services/img/pride_and_prejudice_librivox",
                    audioUrl = "pride_and_prejudice_librivox",
                    source = "Internet Archive",
                    description = "Jane Austen's famous romantic comedy of manners.",
                    durationMs = 9400000L,
                    category = "Kamu Malı (Public Domain)"
                )
            )
            list.add(
                OnlineAudiobookItem(
                    id = "archive-sherlock_holmes_01_librivox",
                    title = "The Adventures of Sherlock Holmes",
                    author = "Arthur Conan Doyle",
                    narrator = "LibriVox Volunteers",
                    coverImageUrl = "https://archive.org/services/img/adventures_sherlock_holmes_0710_librivox",
                    audioUrl = "adventures_sherlock_holmes_0710_librivox",
                    source = "Internet Archive",
                    description = "Arthur Conan Doyle's collection of standard mystery cases.",
                    durationMs = 8100000L,
                    category = "Kamu Malı (Public Domain)"
                )
            )
            list.add(
                OnlineAudiobookItem(
                    id = "archive-alice_in_wonderland_librivox",
                    title = "Alice's Adventures in Wonderland",
                    author = "Lewis Carroll",
                    narrator = "LibriVox Volunteers",
                    coverImageUrl = "https://archive.org/services/img/alice_in_wonderland_librivox",
                    audioUrl = "alice_in_wonderland_librivox",
                    source = "Internet Archive",
                    description = "Lewis Carroll's beloved fantasy tale.",
                    durationMs = 4500000L,
                    category = "Kamu Malı (Public Domain)"
                )
            )
        }
        return list
    }
}
