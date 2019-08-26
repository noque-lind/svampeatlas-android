package com.noque.svampeatlas.Utilities

import android.content.Context
import androidx.preference.PreferenceManager

class SharedPreferencesHelper(context: Context) {

    companion object {
        private val TOKEN_KEY = "TOKEN_KEY"
    }


    private val PREF_API_KEY = "Api key"
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun saveToken(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(TOKEN_KEY, null)
    }

    fun removeToken() {
        prefs.edit().remove(TOKEN_KEY).apply()
    }
}