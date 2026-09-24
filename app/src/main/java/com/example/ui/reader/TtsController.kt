package com.example.ui.reader

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class TtsController(
    private val context: Context,
    private val onSentenceChanged: (Int) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val TAG = "TtsController"
    private var tts: TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var isInitialized by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentSentenceIndex by mutableIntStateOf(0)
        private set

    var speechRate by mutableFloatStateOf(1.0f)
        private set

    var currentLanguage by mutableStateOf(Locale("tr", "TR"))
        private set

    private var sentences: List<String> = emptyList()

    val supportedLanguages = listOf(
        Locale("tr", "TR") to "Türkçe (TR)",
        Locale.US to "English (US)",
        Locale.GERMANY to "Deutsch (DE)",
        Locale.FRANCE to "Français (FR)",
        Locale("es", "ES") to "Español (ES)",
        Locale("ru", "RU") to "Русский (RU)",
        Locale("ar", "AE") to "العربية (AR)"
    )

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech instance: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            mainHandler.post {
                try {
                    setLanguage(currentLanguage)
                    
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            mainHandler.post {
                                isPlaying = true
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            mainHandler.post {
                                val nextIdx = currentSentenceIndex + 1
                                if (nextIdx < sentences.size) {
                                    currentSentenceIndex = nextIdx
                                    onSentenceChanged(nextIdx)
                                    speakCurrent()
                                } else {
                                    isPlaying = false
                                    currentSentenceIndex = 0
                                }
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            mainHandler.post {
                                isPlaying = false
                            }
                        }
                    })
                    isInitialized = true
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up TextToSpeech listener: ${e.message}")
                }
            }
        }
    }

    fun setContent(sentenceList: List<String>, startIndex: Int = 0) {
        sentences = sentenceList
        currentSentenceIndex = startIndex.coerceIn(0, (sentenceList.size - 1).coerceAtLeast(0))
        
        // Auto-detect language based on the first few sentences content
        if (sentenceList.isNotEmpty()) {
            val sampleText = sentenceList.take(3).joinToString(" ")
            val detectedLocale = detectLanguage(sampleText)
            setLanguage(detectedLocale)
        }
    }

    fun detectLanguage(text: String): Locale {
        val lower = text.lowercase()
        // Common Turkish stop words
        val trCount = listOf(" ve ", " bir ", " bu ", " için ", " da ", " de ", " ne ", " o ", " ama ", " çok ").count { lower.contains(it) }
        // Common English stop words
        val enCount = listOf(" the ", " and ", " of ", " to ", " a ", " in ", " that ", " is ", " was ", " for ").count { lower.contains(it) }
        // Common German stop words
        val deCount = listOf(" der ", " die ", " das ", " und ", " ist ", " in ", " zu ", " von ", " mit ", " auf ").count { lower.contains(it) }
        // Common French stop words
        val frCount = listOf(" le ", " la ", " les ", " et ", " est ", " dans ", " de ", " un ", " une ", " pour ").count { lower.contains(it) }
        // Common Spanish stop words
        val esCount = listOf(" el ", " la ", " los ", " y ", " en ", " que ", " un ", " una ", " para ", " con ").count { lower.contains(it) }

        val max = maxOf(trCount, enCount, deCount, frCount, esCount)
        return when {
            max == 0 -> Locale("tr", "TR") // Default to Turkish
            trCount == max -> Locale("tr", "TR")
            enCount == max -> Locale.US
            deCount == max -> Locale.GERMANY
            frCount == max -> Locale.FRANCE
            esCount == max -> Locale("es", "ES")
            else -> Locale("tr", "TR")
        }
    }

    fun setLanguage(locale: Locale) {
        currentLanguage = locale
        try {
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting language $locale: ${e.message}")
        }
    }

    fun play() {
        if (!isInitialized || sentences.isEmpty()) return
        isPlaying = true
        speakCurrent()
    }

    private fun speakCurrent() {
        if (currentSentenceIndex in sentences.indices) {
            val text = sentences[currentSentenceIndex]
            try {
                tts?.setSpeechRate(speechRate)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_${currentSentenceIndex}")
            } catch (e: Exception) {
                Log.e(TAG, "Error trying to speak: ${e.message}")
            }
        }
    }

    fun pause() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error on pause stop: ${e.message}")
        }
        isPlaying = false
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error on stop: ${e.message}")
        }
        isPlaying = false
        currentSentenceIndex = 0
    }

    fun nextSentence() {
        if (currentSentenceIndex + 1 < sentences.size) {
            try {
                tts?.stop()
            } catch (e: Exception) {}
            currentSentenceIndex++
            onSentenceChanged(currentSentenceIndex)
            if (isPlaying) {
                speakCurrent()
            }
        }
    }

    fun prevSentence() {
        if (currentSentenceIndex > 0) {
            try {
                tts?.stop()
            } catch (e: Exception) {}
            currentSentenceIndex--
            onSentenceChanged(currentSentenceIndex)
            if (isPlaying) {
                speakCurrent()
            }
        }
    }

    fun setRate(rate: Float) {
        speechRate = rate
        try {
            tts?.setSpeechRate(rate)
        } catch (e: Exception) {}
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing TTS: ${e.message}")
        }
        tts = null
    }
}
