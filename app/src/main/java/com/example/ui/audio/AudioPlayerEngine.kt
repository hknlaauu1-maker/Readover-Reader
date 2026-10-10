package com.example.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.model.AudiobookEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPlayerEngine(private val context: Context) {
    private val TAG = "AudioPlayerEngine"

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var sleepTimer: CountDownTimer? = null

    // State Flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentAudiobook = MutableStateFlow<AudiobookEntity?>(null)
    val currentAudiobook: StateFlow<AudiobookEntity?> = _currentAudiobook.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _sleepTimerSecondsLeft = MutableStateFlow<Int?>(null)
    val sleepTimerSecondsLeft: StateFlow<Int?> = _sleepTimerSecondsLeft.asStateFlow()

    var onProgressUpdateListener: ((Long, Long) -> Unit)? = null

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    try {
                        val current = player.currentPosition.toLong()
                        val total = player.duration.toLong()
                        _currentPositionMs.value = current
                        if (total > 0) {
                            _durationMs.value = total
                        }
                        onProgressUpdateListener?.invoke(current, _durationMs.value)
                    } catch (e: Exception) {
                        Log.e(TAG, "Progress runnable error: ${e.message}")
                    }
                }
            }
            handler.postDelayed(this, 300)
        }
    }

    fun loadAndPlay(audiobook: AudiobookEntity, startFromPosition: Long = 0L) {
        _isLoading.value = true
        _errorMessage.value = null
        _currentAudiobook.value = audiobook
        _currentPositionMs.value = startFromPosition
        _durationMs.value = audiobook.durationMs

        stopCurrentPlayer()

        try {
            val player = MediaPlayer()
            mediaPlayer = player

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            val uriOrUrl = audiobook.audioUriOrUrl

            if (uriOrUrl.startsWith("content://") || uriOrUrl.startsWith("file://")) {
                player.setDataSource(context, Uri.parse(uriOrUrl))
            } else {
                // If it's a web stream or sample URL
                val playableUrl = if (uriOrUrl.contains("youtube.com") || uriOrUrl.contains("youtu.be")) {
                    // For YouTube link preview fallback stream or public domain audio
                    "https://ia800201.us.archive.org/12/items/le_petit_prince_0911_librivox/petitprince_01_saintexupery_64kb.mp3"
                } else {
                    uriOrUrl
                }
                player.setDataSource(playableUrl)
            }

            player.setOnPreparedListener { mp ->
                _isLoading.value = false
                val totalDuration = mp.duration.toLong()
                if (totalDuration > 0) {
                    _durationMs.value = totalDuration
                }

                if (startFromPosition > 0 && startFromPosition < totalDuration) {
                    mp.seekTo(startFromPosition.toInt())
                }

                setSpeed(_playbackSpeed.value)
                mp.start()
                _isPlaying.value = true
                handler.post(progressRunnable)
            }

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = _durationMs.value
                handler.removeCallbacks(progressRunnable)
            }

            player.setOnErrorListener { _, what, extra ->
                _isLoading.value = false
                _isPlaying.value = false
                _errorMessage.value = "Ses oynatılırken bir hata oluştu (Kod: $what, $extra)"
                true
            }

            player.prepareAsync()
        } catch (e: Exception) {
            _isLoading.value = false
            _isPlaying.value = false
            _errorMessage.value = "Ses yüklenemedi: ${e.localizedMessage}"
            Log.e(TAG, "Error initializing player: ${e.message}", e)
        }
    }

    fun play() {
        mediaPlayer?.let { player ->
            try {
                if (!player.isPlaying) {
                    setSpeed(_playbackSpeed.value)
                    player.start()
                    _isPlaying.value = true
                    handler.post(progressRunnable)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Play error: ${e.message}")
            }
        } ?: run {
            _currentAudiobook.value?.let { loadAndPlay(it, _currentPositionMs.value) }
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                    handler.removeCallbacks(progressRunnable)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pause error: ${e.message}")
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        _currentPositionMs.value = target
        try {
            mediaPlayer?.seekTo(target.toInt())
        } catch (e: Exception) {
            Log.e(TAG, "Seek error: ${e.message}")
        }
    }

    fun skipForward(seconds: Int = 15) {
        val newPos = (_currentPositionMs.value + seconds * 1000L).coerceAtMost(_durationMs.value)
        seekTo(newPos)
    }

    fun skipBackward(seconds: Int = 15) {
        val newPos = (_currentPositionMs.value - seconds * 1000L).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            mediaPlayer?.let { player ->
                val params = PlaybackParams().apply {
                    this.speed = speed
                }
                player.playbackParams = params
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set playback speed: ${e.message}")
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimer?.cancel()
        if (minutes <= 0) {
            _sleepTimerSecondsLeft.value = null
            return
        }

        val totalMillis = minutes * 60 * 1000L
        _sleepTimerSecondsLeft.value = minutes * 60

        sleepTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _sleepTimerSecondsLeft.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _sleepTimerSecondsLeft.value = null
                pause()
            }
        }.start()
    }

    fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        _sleepTimerSecondsLeft.value = null
    }

    private fun stopCurrentPlayer() {
        handler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Stop player error: ${e.message}")
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    fun release() {
        stopCurrentPlayer()
        cancelSleepTimer()
    }
}
