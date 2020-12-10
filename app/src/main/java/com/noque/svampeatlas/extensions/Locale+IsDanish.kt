package com.noque.svampeatlas.extensions

import android.util.Log
import com.noque.svampeatlas.R
import com.noque.svampeatlas.utilities.SharedPreferences
import java.util.*

enum class AppLanguage {
    Danish,
    English,
    Czech
}

fun Locale.appLanguage(): AppLanguage {
    return when (SharedPreferences.preferredLanguage) {
        "language_danish" -> AppLanguage.Danish
        "language_english" -> AppLanguage.English
        "language_czech" -> AppLanguage.Czech
        else -> when (language) {
            "en" -> AppLanguage.English
            "da" -> AppLanguage.Danish
            "cs" -> AppLanguage.Czech
            else -> AppLanguage.Danish
        }
    }
}