package com.example.util.stats

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object StatsManager {
    private const val PREFS_NAME = "readover_reading_stats_prefs"
    private const val KEY_READING_TIME = "total_reading_time_seconds"
    private const val KEY_LISTENING_TIME = "total_listening_time_seconds"
    
    // Day of week stats for custom visualization
    private val DAY_KEYS = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")

    var totalReadingSeconds by mutableStateOf(0L)
    var totalListeningSeconds by mutableStateOf(0L)

    fun init(context: Context) {
        val prefs = getPrefs(context)
        totalReadingSeconds = prefs.getLong(KEY_READING_TIME, 0L)
        totalListeningSeconds = prefs.getLong(KEY_LISTENING_TIME, 0L)
        
        // Seed initial beautiful mock data for weekly bars if never initialized
        if (!prefs.contains("mon")) {
            val editor = prefs.edit()
            editor.putLong("mon", 15 * 60) // 15 mins
            editor.putLong("tue", 28 * 60) // 28 mins
            editor.putLong("wed", 42 * 60) // 42 mins
            editor.putLong("thu", 10 * 60) // 10 mins
            editor.putLong("fri", 35 * 60) // 35 mins
            editor.putLong("sat", 55 * 60) // 55 mins
            editor.putLong("sun", 20 * 60) // 20 mins
            editor.apply()
        }
    }

    fun addReadingTime(context: Context, seconds: Long) {
        val prefs = getPrefs(context)
        val current = prefs.getLong(KEY_READING_TIME, 0L)
        val newValue = current + seconds
        prefs.edit().putLong(KEY_READING_TIME, newValue).apply()
        totalReadingSeconds = newValue

        // Also add to today's weekly bar
        addTodayTime(context, seconds)
    }

    fun addListeningTime(context: Context, seconds: Long) {
        val prefs = getPrefs(context)
        val current = prefs.getLong(KEY_LISTENING_TIME, 0L)
        val newValue = current + seconds
        prefs.edit().putLong(KEY_LISTENING_TIME, newValue).apply()
        totalListeningSeconds = newValue

        // Also add to today's weekly bar
        addTodayTime(context, seconds)
    }

    private fun addTodayTime(context: Context, seconds: Long) {
        val prefs = getPrefs(context)
        val todayKey = getTodayKey()
        val current = prefs.getLong(todayKey, 0L)
        prefs.edit().putLong(todayKey, current + seconds).apply()
    }

    fun getWeeklyStats(context: Context): List<Float> {
        val prefs = getPrefs(context)
        return DAY_KEYS.map { key ->
            (prefs.getLong(key, 0L) / 60f) // convert to minutes
        }
    }

    fun getTodayKey(): String {
        val calendar = java.util.Calendar.getInstance()
        return when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "mon"
            java.util.Calendar.TUESDAY -> "tue"
            java.util.Calendar.WEDNESDAY -> "wed"
            java.util.Calendar.THURSDAY -> "thu"
            java.util.Calendar.FRIDAY -> "fri"
            java.util.Calendar.SATURDAY -> "sat"
            java.util.Calendar.SUNDAY -> "sun"
            else -> "sun"
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
