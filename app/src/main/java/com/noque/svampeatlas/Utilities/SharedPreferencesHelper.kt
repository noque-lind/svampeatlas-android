package com.noque.svampeatlas.Utilities

import android.content.Context
import androidx.preference.PreferenceManager

class SharedPreferencesHelper(context: Context) {

    private val PREF_API_KEY = "Api key"
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)


    fun saveAPIKey(key: String?) {
        prefs.edit().putString(PREF_API_KEY, key).apply()
    }

    fun getAPIKey(): String? {
        return prefs.getString(PREF_API_KEY, null)
    }
}