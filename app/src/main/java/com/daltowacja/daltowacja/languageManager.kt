package com.daltowacja.daltowacja

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {
    private var originalLocale: Locale? = null

    fun setAppLanguage(context: Context, selectedLanguage: String) {
        if (originalLocale == null) {
            // Store the original system locale when it's not already stored
            originalLocale = context.resources.configuration.locales.get(0)
        }

        val locale = if (selectedLanguage == "auto") {
            // Use the original system locale for "auto"
            originalLocale
        } else {
            // Use the selected language
            Locale(selectedLanguage)
        }

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}