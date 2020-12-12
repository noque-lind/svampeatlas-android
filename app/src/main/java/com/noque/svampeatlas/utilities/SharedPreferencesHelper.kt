package com.noque.svampeatlas.utilities

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager

object SharedPreferences {

    private const val TOKEN_KEY = "TOKEN_KEY"
    private const val LOCKED_SUBSTRATE_ID = "LOCKED_SUBSTRATE_ID"
    private const val LOCKED_VEGETATIONTYPE_ID = "LOCKED_VEGETATIONTYPE_ID"
    private const val LOCKED_HOSTS = "LOCKED_HOSTS"
    private const val HAS_ACCEPTED_IDENTIFCATION_TERMS = "HAS_ACCEPTED_IDENTIFCATION_TERMS"
    private const val SAVE_IMAGES = "SAVE_IMAGES"
    private const val SAVE_IMAGES_DECIDED = "SAVE_IMAGES_DECIDED"
    private const val HAS_SEEN_WHATS_NEW = "HAS_SEEN_WHATS_NEW"
    private const val HAS_SEEN_IMAGE_DELETION = "HAS_SEEN_IMAGE_DELETION"
    private const val PREFERRED_LANGUAGE = "PREFERRED_LANGUAGE"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

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
            LOCKED_SUBSTRATE_ID
        ).apply()
    }

    fun saveVegetationTypeID(id: Int?) {
        if (id != null) prefs.edit().putInt(LOCKED_VEGETATIONTYPE_ID, id).apply() else prefs.edit().remove(
            LOCKED_VEGETATIONTYPE_ID
        ).apply()
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

    fun hasAcceptedIdentificationTerms(): Boolean {
        return prefs.getBoolean(HAS_ACCEPTED_IDENTIFCATION_TERMS, false)
    }

    fun setHasAcceptedIdentificationTerms(value: Boolean) {
        prefs.edit().putBoolean(HAS_ACCEPTED_IDENTIFCATION_TERMS, value).apply()
    }

    var hasSeenWhatsNew: Boolean get() {
        return prefs.getBoolean(HAS_SEEN_WHATS_NEW, false)
    } set(value) {
        prefs.edit().putBoolean(HAS_SEEN_WHATS_NEW, value).apply()
    }

    var hasSeenImageDeletion: Boolean get() {
        return prefs.getBoolean(HAS_SEEN_IMAGE_DELETION, false)
    } set(value) {
        prefs.edit().putBoolean(HAS_SEEN_IMAGE_DELETION, value).apply()
    }

    var preferredLanguage: String get() {
        return prefs.getString(PREFERRED_LANGUAGE, "not_set") ?: "not_set"
    } set(value) {
        prefs.edit().putString(PREFERRED_LANGUAGE, value).apply()
    }

    fun getSaveImages(): Boolean? {
        return if (!prefs.getBoolean(SAVE_IMAGES_DECIDED, false)) {
            null
        } else {
            prefs.getBoolean(SAVE_IMAGES, false)
        }
    }

    fun setSaveImages(newValue: Boolean) {

        prefs.edit().putBoolean(SAVE_IMAGES, newValue).putBoolean(SAVE_IMAGES_DECIDED, true).apply()
    }
}
