package com.example.util.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val flag: String) {
    TURKISH("tr", "Türkçe", "Türkçe", "🇹🇷"),
    ENGLISH("en", "English", "English", "🇬🇧"),
    JAPANESE("ja", "Japonca", "日本語", "🇯🇵"),
    ARABIC("ar", "Arapça", "العربية", "🇸🇦"),
    RUSSIAN("ru", "Rusça", "Русский", "🇷🇺"),
    ITALIAN("it", "İtalyanca", "Italiano", "🇮🇹"),
    INDONESIAN("id", "Endonezce", "Bahasa Indonesia", "🇮🇩"),
    CHINESE("zh", "Çince", "中文", "🇨🇳"),
    SPANISH("es", "İspanyolca", "Español", "🇪🇸"),
    GERMAN("de", "Almanca", "Deutsch", "🇩🇪"),
    FRENCH("fr", "Fransızca", "Français", "🇫🇷");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: TURKISH
        }
    }
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
