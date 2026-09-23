package com.example.ui.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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

    private var tts: TextToSpeech? = null
    var isInitialized by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentSentenceIndex by mutableIntStateOf(0)
        private set

    var speechRate by mutableFloatStateOf(1.0f)
        private set

    private var sentences: List<String> = emptyList()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("tr", "TR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isPlaying = true
                }

                override fun onDone(utteranceId: String?) {
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

                override fun onError(utteranceId: String?) {
                    isPlaying = false
                }
            })
            isInitialized = true
        }
    }

    fun setContent(sentenceList: List<String>, startIndex: Int = 0) {
        sentences = sentenceList
        currentSentenceIndex = startIndex.coerceIn(0, (sentenceList.size - 1).coerceAtLeast(0))
    }

    fun play() {
        if (!isInitialized || sentences.isEmpty()) return
        isPlaying = true
        speakCurrent()
    }

    private fun speakCurrent() {
        if (currentSentenceIndex in sentences.indices) {
            val text = sentences[currentSentenceIndex]
            tts?.setSpeechRate(speechRate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_${currentSentenceIndex}")
        }
    }

    fun pause() {
        tts?.stop()
        isPlaying = false
    }

    fun stop() {
        tts?.stop()
        isPlaying = false
        currentSentenceIndex = 0
    }

    fun nextSentence() {
        if (currentSentenceIndex + 1 < sentences.size) {
            tts?.stop()
            currentSentenceIndex++
            onSentenceChanged(currentSentenceIndex)
            if (isPlaying) {
                speakCurrent()
            }
        }
    }

    fun prevSentence() {
        if (currentSentenceIndex > 0) {
            tts?.stop()
            currentSentenceIndex--
            onSentenceChanged(currentSentenceIndex)
            if (isPlaying) {
                speakCurrent()
            }
        }
    }

    fun setRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
