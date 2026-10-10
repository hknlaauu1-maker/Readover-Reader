package com.example.data.openlibrary

import android.util.Log
import com.example.data.openlibrary.opds.OpdsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object BookAggregator {
    private const val TAG = "BookAggregator"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // In-memory cache for fast response and reduced network traffic
    private val cacheMap = ConcurrentHashMap<String, List<OpenBookItem>>()

    val attributionMap = mapOf(
        "Gutenberg" to "Project Gutenberg (m.gutenberg.org)",
        "OpenLibrary" to "Open Library (openlibrary.org)",
        "StandardEbooks" to "Standard Ebooks (standardebooks.org)",
        "Wikisource" to "Wikikaynak (wikisource.org)"
    )

    fun getAttribution(source: String): String {
        return attributionMap[source] ?: "$source (Açık Kaynak)"
    }

    /**
     * Search all open source ebook aggregators in parallel and return aggregated results.
     */
    suspend fun searchAll(query: String, langCode: String = "tr"): List<OpenBookItem> = coroutineScope {
        val trimmedQuery = query.trim()
        val cacheKey = "${trimmedQuery.lowercase()}_$langCode"

        if (cacheMap.containsKey(cacheKey) && cacheMap[cacheKey]!!.isNotEmpty()) {
            return@coroutineScope cacheMap[cacheKey]!!
        }

        if (trimmedQuery.isEmpty()) {
            val curated = fetchCuratedDefaultBooks(langCode)
            cacheMap[cacheKey] = curated
            return@coroutineScope curated
        }

        val gutenbergDeferred = async { fetchGutenberg(trimmedQuery, langCode) }
        val openLibraryDeferred = async { fetchOpenLibrary(trimmedQuery, langCode) }
        val standardEbooksDeferred = async { fetchStandardEbooks(trimmedQuery) }
        val wikisourceDeferred = async { fetchWikisource(trimmedQuery, langCode) }

        val gutenbergList = try { gutenbergDeferred.await() } catch (e: Exception) { emptyList() }
        val openLibraryList = try { openLibraryDeferred.await() } catch (e: Exception) { emptyList() }
        val standardEbooksList = try { standardEbooksDeferred.await() } catch (e: Exception) { emptyList() }
        val wikisourceList = try { wikisourceDeferred.await() } catch (e: Exception) { emptyList() }

        val combined = (wikisourceList + gutenbergList + openLibraryList + standardEbooksList)
            .distinctBy { "${it.title.lowercase()}_${it.author.lowercase()}" }
            .sortedWith(Comparator { a, b ->
                val aIsTurkish = a.language.contains("Türkçe", ignoreCase = true) || a.source == "Wikisource"
                val bIsTurkish = b.language.contains("Türkçe", ignoreCase = true) || b.source == "Wikisource"
                when {
                    langCode == "tr" && aIsTurkish && !bIsTurkish -> -1
                    langCode == "tr" && !aIsTurkish && bIsTurkish -> 1
                    else -> b.downloadCount.compareTo(a.downloadCount)
                }
            })

        val finalResults = if (combined.isEmpty()) {
            OpenLibraryService.curatedCatalog.filter {
                it.title.contains(trimmedQuery, ignoreCase = true) ||
                it.author.contains(trimmedQuery, ignoreCase = true)
            }
        } else {
            combined
        }

        cacheMap[cacheKey] = finalResults
        finalResults
    }

    /**
     * Source 1: Project Gutenberg (Gutendex REST API)
     */
    suspend fun fetchGutenberg(query: String, langCode: String): List<OpenBookItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OpenBookItem>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val langParam = if (langCode.isNotBlank()) "&languages=$langCode" else ""
            val url = "https://gutendex.com/books/?search=$encodedQuery$langParam"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return@use
                    val rootObj = JSONObject(jsonStr)
                    val results = rootObj.optJSONArray("results") ?: return@use

                    for (i in 0 until results.length()) {
                        val bookObj = results.getJSONObject(i)
                        val id = bookObj.optInt("id", 0)
                        val title = bookObj.optString("title", "İsimsiz Eser")
                        
                        val authorsArr = bookObj.optJSONArray("authors")
                        var authorName = "Project Gutenberg"
                        if (authorsArr != null && authorsArr.length() > 0) {
                            authorName = authorsArr.getJSONObject(0).optString("name", "Bilinmeyen Yazar")
                        }

                        val formats = bookObj.optJSONObject("formats")
                        var coverUrl: String? = null
                        var downloadUrl: String? = null
                        var formatStr = "EPUB"

                        if (formats != null) {
                            coverUrl = formats.optString("image/jpeg", null)
                            if (formats.has("application/epub+zip")) {
                                downloadUrl = formats.getString("application/epub+zip")
                                formatStr = "EPUB"
                            } else if (formats.has("text/plain; charset=us-ascii") || formats.has("text/plain")) {
                                downloadUrl = formats.optString("text/plain; charset=us-ascii", formats.optString("text/plain", null))
                                formatStr = "TXT"
                            } else if (formats.has("text/html")) {
                                downloadUrl = formats.getString("text/html")
                                formatStr = "HTML"
                            }
                        }

                        val downloadCount = bookObj.optInt("download_count", 0)
                        val langArr = bookObj.optJSONArray("languages")
                        val itemLang = if (langArr != null && langArr.length() > 0) langArr.getString(0) else langCode

                        list.add(
                            OpenBookItem(
                                id = "gutenberg-$id",
                                title = title,
                                author = authorName,
                                coverUrl = coverUrl ?: "https://www.gutenberg.org/cache/epub/$id/pg$id.cover.medium.jpg",
                                category = "Klasik Edebiyat",
                                language = if (itemLang.equals("tr", ignoreCase = true)) "Türkçe" else "İngilizce",
                                format = formatStr,
                                source = "Gutenberg",
                                description = "$title - Project Gutenberg kamu malı klasik eser.",
                                downloadUrl = downloadUrl,
                                downloadCount = downloadCount,
                                estimatedPages = 210
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gutenberg fetch failed: ${e.message}")
        }
        list
    }

    /**
     * Source 2: Open Library (search.json API with language filtering)
     */
    suspend fun fetchOpenLibrary(query: String, langCode: String): List<OpenBookItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OpenBookItem>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val olLang = when(langCode) {
                "tr" -> "tur"
                "en" -> "eng"
                "ru" -> "rus"
                "de" -> "ger"
                "fr" -> "fre"
                "es" -> "spa"
                "it" -> "ita"
                "ar" -> "ara"
                "ja" -> "jpn"
                "id" -> "ind"
                "zh" -> "chi"
                else -> langCode
            }
            val url = "https://openlibrary.org/search.json?q=$encodedQuery&language=$olLang&limit=15"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return@use
                    val root = JSONObject(jsonStr)
                    val docs = root.optJSONArray("docs") ?: return@use

                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val title = doc.optString("title", "Açık Kitap")
                        val authorsArr = doc.optJSONArray("author_name")
                        val author = if (authorsArr != null && authorsArr.length() > 0) authorsArr.getString(0) else "Open Library"
                        val coverI = doc.optInt("cover_i", 0)
                        val key = doc.optString("key", "")

                        val coverUrl = if (coverI > 0) "https://covers.openlibrary.org/b/id/$coverI-L.jpg" else null

                        list.add(
                            OpenBookItem(
                                id = "openlibrary-${key.replace("/", "-")}",
                                title = title,
                                author = author,
                                coverUrl = coverUrl,
                                category = "Açık Kütüphane",
                                language = if (langCode == "tr") "Türkçe" else "İngilizce",
                                format = "EPUB",
                                source = "OpenLibrary",
                                description = "$title - Open Library kataloğundan açık kaynak eser.",
                                downloadUrl = if (key.isNotBlank()) "https://openlibrary.org$key" else null,
                                estimatedPages = 180
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenLibrary fetch failed: ${e.message}")
        }
        list
    }

    /**
     * Source 3: Standard Ebooks (OPDS Catalog)
     */
    suspend fun fetchStandardEbooks(query: String): List<OpenBookItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OpenBookItem>()
        try {
            val url = "https://standardebooks.org/opds/all"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xmlBytes = response.body?.bytes() ?: return@use
                    val parsed = OpdsParser.parseFeed(ByteArrayInputStream(xmlBytes), "StandardEbooks")
                    val filtered = parsed.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
                    }
                    list.addAll(filtered)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "StandardEbooks fetch failed: ${e.message}")
        }
        list
    }

    /**
     * Source 4: Wikisource / Wikikaynak (MediaWiki API)
     */
    suspend fun fetchWikisource(query: String, langCode: String): List<OpenBookItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OpenBookItem>()
        try {
            val subDomain = if (langCode.equals("tr", ignoreCase = true)) "tr" else "en"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://$subDomain.wikisource.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json&srlimit=10"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return@use
                    val root = JSONObject(jsonStr)
                    val queryObj = root.optJSONObject("query") ?: return@use
                    val searchArr = queryObj.optJSONArray("search") ?: return@use

                    for (i in 0 until searchArr.length()) {
                        val item = searchArr.getJSONObject(i)
                        val title = item.optString("title", "")
                        val snippet = item.optString("snippet", "")
                            .replace(Regex("<[^>]*>"), "")
                            .replace("&quot;", "\"")

                        if (title.isNotBlank()) {
                            list.add(
                                OpenBookItem(
                                    id = "wikisource-$subDomain-${item.optLong("pageid", System.currentTimeMillis())}",
                                    title = title,
                                    author = if (subDomain == "tr") "Wikikaynak Özgür Kitaplık" else "Wikisource Free Library",
                                    coverUrl = "https://upload.wikimedia.org/wikipedia/commons/4/4c/Wikisource-logo.png",
                                    category = "Tarih & Özgür Metinler",
                                    language = if (subDomain == "tr") "Türkçe" else "İngilizce",
                                    format = "TXT",
                                    source = "Wikisource",
                                    description = if (snippet.isBlank()) "$title - Wikikaynak açık erişimli eser." else snippet,
                                    downloadUrl = "https://$subDomain.wikisource.org/wiki/${URLEncoder.encode(title, "UTF-8")}",
                                    estimatedPages = 140
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wikisource fetch failed: ${e.message}")
        }
        list
    }

    private fun fetchCuratedDefaultBooks(langCode: String): List<OpenBookItem> {
        val rawList = OpenLibraryService.curatedCatalog
        if (langCode.equals("tr", ignoreCase = true)) {
            return rawList
        }
        // Map to English counterparts for international users
        return rawList.map { book ->
            when (book.id) {
                "curated-nutuk" -> book.copy(
                    title = "The Speech (Nutuk)",
                    author = "Mustafa Kemal Ataturk",
                    category = "History & Politics",
                    language = "English",
                    description = "The immortal speech of Gazi Mustafa Kemal Ataturk, detailing the Turkish War of Independence and the foundation of the Republic."
                )
                "curated-suc-ve-ceza" -> book.copy(
                    title = "Crime and Punishment",
                    author = "Fyodor Dostoevsky",
                    category = "World Classics",
                    language = "English",
                    description = "Raskolnikov, an impoverished student in St. Petersburg, conceives of a plan to murder and rob an unpleasant pawnbroker."
                )
                "curated-kucuk-prens" -> book.copy(
                    title = "The Little Prince",
                    author = "Antoine de Saint-Exupéry",
                    category = "Children & Philosophy",
                    language = "English",
                    description = "A pilot stranded in the desert meets a young prince who fallen to Earth from a tiny asteroid."
                )
                "curated-donusum" -> book.copy(
                    title = "The Metamorphosis",
                    author = "Franz Kafka",
                    category = "World Classics",
                    language = "English",
                    description = "Gregor Samsa, a traveling salesman, wakes up one morning to find himself transformed into a monstrous insect."
                )
                "curated-satranc" -> book.copy(
                    title = "Chess Story",
                    author = "Stefan Zweig",
                    category = "Psychological Novel",
                    language = "English",
                    description = "A group of passengers on an ocean liner challenge the world chess champion to a match."
                )
                "curated-yeralti" -> book.copy(
                    title = "Notes from Underground",
                    author = "Fyodor Dostoevsky",
                    category = "Philosophy & Fiction",
                    language = "English",
                    description = "A deeply psychological monologue of a retired civil servant living in St. Petersburg."
                )
                "curated-sherlock" -> book.copy(
                    title = "A Study in Scarlet",
                    author = "Arthur Conan Doyle",
                    category = "Mystery & Adventure",
                    language = "English",
                    description = "The historic introduction of Dr. John Watson to the legendary consulting detective Sherlock Holmes."
                )
                "curated-sokrates" -> book.copy(
                    title = "Apology of Socrates",
                    author = "Plato",
                    category = "Philosophy",
                    language = "English",
                    description = "Socrates' famous defense speech at his trial in Athens, defending wisdom, justice, and the examined life."
                )
                "curated-1984" -> book.copy(
                    title = "1984",
                    author = "George Orwell",
                    category = "Dystopian Classic",
                    language = "English",
                    description = "Winston Smith's rebellion against the total control of Big Brother in the dystopian state of Oceania."
                )
                "curated-kurk-mantolu" -> book.copy(
                    title = "Madonna in a Fur Coat",
                    author = "Sabahattin Ali",
                    category = "Turkish Literature",
                    language = "English",
                    description = "A timeless romantic masterpiece detailing Raif Efendi's inner life and his fateful encounter with Maria Puder in Berlin."
                )
                "curated-beyaz-dis" -> book.copy(
                    title = "White Fang",
                    author = "Jack London",
                    category = "Adventure Classic",
                    language = "English",
                    description = "The epic story of a wild wolf-dog's journey through violence and domestication in the frozen North."
                )
                "curated-martin-eden" -> book.copy(
                    title = "Martin Eden",
                    author = "Jack London",
                    category = "World Literature",
                    language = "English",
                    description = "An uneducated sailor's intense struggle to educate himself and become a famous writer for the love of a high-society woman."
                )
                else -> book
            }
        }
    }
}
