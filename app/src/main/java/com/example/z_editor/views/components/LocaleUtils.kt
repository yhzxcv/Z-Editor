package com.example.z_editor.views.components

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.core.content.edit

object LocaleUtils {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LANG = "selected_language"

    // 保存语言设置
    fun saveLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_LANG, lang) }
    }

    // 获取保存的语言，默认跟随系统
    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "zh") ?: "zh" // 默认中文
    }

    // 核心：应用语言设置并返回新的 Context
    fun applyLocale(context: Context, lang: String): Context {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}