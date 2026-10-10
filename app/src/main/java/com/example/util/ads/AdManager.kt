package com.example.util.ads

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AdManager {
    private const val PREFS_NAME = "readover_monetization_prefs"
    private const val KEY_IS_PREMIUM = "is_premium_unlocked"

    // AdMob Unit IDs (GPLv3 / Open Source ready - configurable by developer)
    var adMobBannerUnitId = "ca-app-pub-3940256099942544/6300978111"
    var adMobInterstitialUnitId = "ca-app-pub-3940256099942544/1033173712"
    var adMobNativeUnitId = "ca-app-pub-3940256099942544/2247696110"

    var isPremiumUser by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val prefs = getPrefs(context)
        isPremiumUser = prefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    fun setPremiumUnlocked(context: Context, unlocked: Boolean) {
        isPremiumUser = unlocked
        getPrefs(context).edit().putBoolean(KEY_IS_PREMIUM, unlocked).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
