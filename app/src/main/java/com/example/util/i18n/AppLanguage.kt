package com.example.util.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    TURKISH("tr", "Türkçe", "🇹🇷"),
    ENGLISH("en", "English", "🇬🇧"),
    GERMAN("de", "Deutsch", "🇩🇪"),
    SPANISH("es", "Español", "🇪🇸"),
    FRENCH("fr", "Français", "🇫🇷")
}

object I18nManager {
    var currentLanguage by mutableStateOf(AppLanguage.TURKISH)

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
    }

    fun getString(key: String): String {
        return AppStrings.get(key, currentLanguage)
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.TURKISH }

@Composable
fun appString(key: String): String {
    val lang = LocalAppLanguage.current
    return AppStrings.get(key, lang)
}
