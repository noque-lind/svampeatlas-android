package com.noque.svampeatlas.utilities

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager

class SharedPreferencesHelper(context: Context) {

    companion object {
        private val TOKEN_KEY = "TOKEN_KEY"
        private val LOCKED_SUBSTRATE_ID = "LOCKED_SUBSTRATE_ID"
        private val LOCKED_VEGETATIONTYPE_ID = "LOCKED_VEGETATIONTYPE_ID"
        private val LOCKED_HOSTS = "LOCKED_HOSTS"
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

    fun saveSubstrateID(id: Int?) {
        if (id != null)  prefs.edit().putInt(LOCKED_SUBSTRATE_ID, id).apply() else prefs.edit().remove(
            LOCKED_SUBSTRATE_ID).apply()
    }

    fun saveVegetationTypeID(id: Int?) {
        if (id != null) prefs.edit().putInt(LOCKED_VEGETATIONTYPE_ID, id).apply() else prefs.edit().remove(
            LOCKED_VEGETATIONTYPE_ID).apply()
    }

    fun saveHostsID(hostsID: List<Int>?) {

        Log.d("SharedPrefs", hostsID.toString())

        val set = mutableSetOf<String>()

        hostsID?.forEach {
            set.add(it.toString())
        }

        prefs.edit().putStringSet(LOCKED_HOSTS, set).apply()
    }

    fun getSubstrateID(): Int? {
        val id =  prefs.getInt(LOCKED_SUBSTRATE_ID, -1)
        return if (id != -1) id else null
    }

    fun getVegetationTypeID(): Int? {
        val id = prefs.getInt(LOCKED_VEGETATIONTYPE_ID, -1)
        return if (id != -1) id else null
    }

    fun getHosts(): List<Int>? {
        val ids = prefs.getStringSet(LOCKED_HOSTS, null)
        return if (ids != null) ids.mapNotNull { it.toInt() } else null
    }
}