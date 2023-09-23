package com.daltowacja.daltowacja

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {
    fun setAppLanguage(context: Context, selectedLanguage: String) {
        if (selectedLanguage != "auto") {
            val locale = Locale(selectedLanguage)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}