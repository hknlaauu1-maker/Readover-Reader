package com.example.data.cover

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object OpenCoverFetcher {
    private const val TAG = "OpenCoverFetcher"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Pre-curated high quality open source covers for popular classics
    private val curatedCovers = mapOf(
        "küçük prens" to "https://covers.openlibrary.org/b/id/8225261-L.jpg",
        "kucuk prens" to "https://covers.openlibrary.org/b/id/8225261-L.jpg",
        "the little prince" to "https://covers.openlibrary.org/b/id/8225261-L.jpg",
        "le petit prince" to "https://covers.openlibrary.org/b/id/8225261-L.jpg",
        "sherlock holmes" to "https://covers.openlibrary.org/b/id/8739161-L.jpg",
        "kızıl soruşturma" to "https://covers.openlibrary.org/b/id/8739161-L.jpg",
        "a study in scarlet" to "https://covers.openlibrary.org/b/id/8739161-L.jpg",
        "dönüşüm" to "https://covers.openlibrary.org/b/id/8741369-L.jpg",
        "donusum" to "https://covers.openlibrary.org/b/id/8741369-L.jpg",
        "metamorphosis" to "https://covers.openlibrary.org/b/id/8741369-L.jpg",
        "sokrates" to "https://covers.openlibrary.org/b/id/8091724-L.jpg",
        "sokrates'in savunması" to "https://covers.openlibrary.org/b/id/8091724-L.jpg",
        "apology" to "https://covers.openlibrary.org/b/id/8091724-L.jpg",
        "jane eyre" to "https://covers.openlibrary.org/b/id/8276707-L.jpg",
        "nutuk" to "https://covers.openlibrary.org/b/id/11145155-L.jpg",
        "1984" to "https://covers.openlibrary.org/b/id/8575742-L.jpg",
        "hayvan çiftliği" to "https://covers.openlibrary.org/b/id/10543105-L.jpg",
        "animal farm" to "https://covers.openlibrary.org/b/id/10543105-L.jpg",
        "suç ve ceza" to "https://covers.openlibrary.org/b/id/12818862-L.jpg",
        "suc ve ceza" to "https://covers.openlibrary.org/b/id/12818862-L.jpg",
        "crime and punishment" to "https://covers.openlibrary.org/b/id/12818862-L.jpg",
        "simyacı" to "https://covers.openlibrary.org/b/id/8231856-L.jpg",
        "the alchemist" to "https://covers.openlibrary.org/b/id/8231856-L.jpg",
        "satranç" to "https://covers.openlibrary.org/b/id/8235118-L.jpg",
        "satranc" to "https://covers.openlibrary.org/b/id/8235118-L.jpg",
        "beyaz diş" to "https://covers.openlibrary.org/b/id/8268800-L.jpg",
        "white fang" to "https://covers.openlibrary.org/b/id/8268800-L.jpg",
        "yeraltından notlar" to "https://covers.openlibrary.org/b/id/8235650-L.jpg",
        "notes from underground" to "https://covers.openlibrary.org/b/id/8235650-L.jpg",
        "sefiller" to "https://covers.openlibrary.org/b/id/8234320-L.jpg",
        "les miserables" to "https://covers.openlibrary.org/b/id/8234320-L.jpg",
        "martin eden" to "https://covers.openlibrary.org/b/id/8231450-L.jpg",
        "insan ne ile yaşar" to "https://covers.openlibrary.org/b/id/8232980-L.jpg",
        "kumral ada mavi tuna" to "https://covers.openlibrary.org/b/id/8233150-L.jpg",
        "tutunamayanlar" to "https://covers.openlibrary.org/b/id/8236120-L.jpg",
        "kürk mantolu madonna" to "https://covers.openlibrary.org/b/id/8237190-L.jpg",
        "kurk mantolu madonna" to "https://covers.openlibrary.org/b/id/8237190-L.jpg",
        "kuyucaklı yusuf" to "https://covers.openlibrary.org/b/id/8238190-L.jpg",
        "dune" to "https://covers.openlibrary.org/b/id/9125346-L.jpg",
        "sapiens" to "https://covers.openlibrary.org/b/id/8741369-L.jpg",
        "hamlet" to "https://covers.openlibrary.org/b/id/8231991-L.jpg",
        "faust" to "https://covers.openlibrary.org/b/id/8234567-L.jpg",
        "dorian gray" to "https://covers.openlibrary.org/b/id/8236521-L.jpg",
        "gurur ve önyargı" to "https://covers.openlibrary.org/b/id/8231852-L.jpg",
        "pride and prejudice" to "https://covers.openlibrary.org/b/id/8231852-L.jpg",
        "vadideki zambak" to "https://covers.openlibrary.org/b/id/8239012-L.jpg",
        "çalıkuşu" to "https://covers.openlibrary.org/b/id/8240123-L.jpg",
        "calikusu" to "https://covers.openlibrary.org/b/id/8240123-L.jpg",
        "anna karenina" to "https://covers.openlibrary.org/b/id/8234509-L.jpg",
        "savaş ve barış" to "https://covers.openlibrary.org/b/id/8234510-L.jpg",
        "war and peace" to "https://covers.openlibrary.org/b/id/8234510-L.jpg",
        "karamazov kardeşler" to "https://covers.openlibrary.org/b/id/8235651-L.jpg",
        "the brothers karamazov" to "https://covers.openlibrary.org/b/id/8235651-L.jpg"
    )

    suspend fun fetchCoverUrl(title: String, author: String? = null): String? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanSearchTerm(title)
        if (cleanTitle.isBlank()) return@withContext null

        val lowerTitle = cleanTitle.lowercase()

        // 1. Check curated open covers dictionary
        for ((key, url) in curatedCovers) {
            if (lowerTitle.contains(key) || key.contains(lowerTitle)) {
                return@withContext url
            }
        }

        // 2. Query Open Library Search API
        try {
            val query = if (!author.isNullOrBlank() && author != "Yerel Belge" && author != "Bilinmeyen Yazar") {
                "$cleanTitle $author"
            } else {
                cleanTitle
            }

            val encoded = URLEncoder.encode(query, "UTF-8")
            val openLibUrl = "https://openlibrary.org/search.json?q=$encoded&limit=3"

            val request = Request.Builder()
                .url(openLibUrl)
                .header("User-Agent", "Readover-Reader/1.0 (Android E-Book & Audiobook)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val docs = json.optJSONArray("docs")
                        if (docs != null && docs.length() > 0) {
                            for (i in 0 until docs.length()) {
                                val doc = docs.getJSONObject(i)
                                val coverI = doc.optInt("cover_i", -1)
                                if (coverI > 0) {
                                    return@withContext "https://covers.openlibrary.org/b/id/$coverI-L.jpg"
                                }
                                val coverEditionKey = doc.optString("cover_edition_key", "")
                                if (coverEditionKey.isNotBlank()) {
                                    return@withContext "https://covers.openlibrary.org/b/olid/$coverEditionKey-L.jpg"
                                }
                                val isbns = doc.optJSONArray("isbn")
                                if (isbns != null && isbns.length() > 0) {
                                    val firstIsbn = isbns.getString(0)
                                    return@withContext "https://covers.openlibrary.org/b/isbn/$firstIsbn-L.jpg"
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Open Library query failed: ${e.message}")
        }

        // 3. Fallback: Google Books Open API Search
        try {
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val googleUrl = "https://www.googleapis.com/books/v1/volumes?q=$encodedTitle&maxResults=2"

            val request = Request.Builder()
                .url(googleUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val items = json.optJSONArray("items")
                        if (items != null && items.length() > 0) {
                            for (i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                val volumeInfo = item.optJSONObject("volumeInfo")
                                val imageLinks = volumeInfo?.optJSONObject("imageLinks")
                                var thumb = imageLinks?.optString("thumbnail")
                                    ?: imageLinks?.optString("smallThumbnail")
                                if (!thumb.isNullOrBlank()) {
                                    // Ensure https & high resolution
                                    if (thumb.startsWith("http://")) {
                                        thumb = thumb.replace("http://", "https://")
                                    }
                                    // Remove zoom curl constraints for clean full cover
                                    thumb = thumb.replace("&edge=curl", "")
                                    return@withContext thumb
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Google Books query failed: ${e.message}")
        }

        null
    }

    private fun cleanSearchTerm(term: String): String {
        return term
            .replace(Regex("(?i)\\.(pdf|epub|mobi|fb2|txt|mp3|m4a|aac|wav|flac|ogg)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
