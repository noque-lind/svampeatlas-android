package com.noque.svampeatlas.utilities

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.noque.svampeatlas.extensions.Date
import com.noque.svampeatlas.models.Locality
import java.util.*
import com.google.gson.Gson
import com.noque.svampeatlas.models.Location


object SharedPreferences {

    private const val TOKEN_KEY = "TOKEN_KEY"
    private const val LOCKED_SUBSTRATE_ID = "LOCKED_SUBSTRATE_ID"
    private const val LOCKED_VEGETATIONTYPE_ID = "LOCKED_VEGETATIONTYPE_ID"
    private const val LOCKED_HOSTS = "LOCKED_HOSTS"
    private const val HAS_ACCEPTED_IDENTIFCATION_TERMS = "HAS_ACCEPTED_IDENTIFCATION_TERMS"
    private const val SAVE_IMAGES = "SAVE_IMAGES"
    private const val SAVE_IMAGES_DECIDED = "SAVE_IMAGES_DECIDED"
    private const val HAS_SEEN_WHATS_NEW = "HAS_SEEN_WHATS_NEW_2.0"
    private const val HAS_SEEN_IMAGE_DELETION = "HAS_SEEN_IMAGE_DELETION"
    private const val PREFERRED_LANGUAGE = "PREFERRED_LANGUAGE"
    private const val LAST_DOWNLOAD_TAXON = "LAST_DOWNLOAD_TAXON"
    private const val LOCALITY_LOCKED = "LOCALITY_LOCKED"
    private const val LOCATION_LOCKED = "LOCATION_LOCKED"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    var token: String? get() = prefs.getString(TOKEN_KEY, null)
    set(value) = prefs.edit().putString(TOKEN_KEY, value).apply()

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

    var lockedLocality: Locality? get() {
        val json = prefs.getString(LOCALITY_LOCKED, null)
        return if(json != null) Gson().fromJson(json, Locality::class.java) else null
    } set(value) {
        if (value != null) {
            prefs.edit().putString(LOCALITY_LOCKED, Gson().toJson(value)).apply()
        } else {
            prefs.edit().remove(LOCALITY_LOCKED).apply()
        }
    }

    var lockedLocation: Location? get() {
        val json = prefs.getString(LOCATION_LOCKED, null)
        return if(json != null) Gson().fromJson(json, Location::class.java) else null
    } set(value) {
        if (value != null) {
            prefs.edit().putString(LOCATION_LOCKED, Gson().toJson(value)).apply()
        } else {
            prefs.edit().remove(LOCATION_LOCKED).apply()
        }
    }

    var lastDownloadOfTaxon: Date?
        get() {
        val long = prefs.getLong(LAST_DOWNLOAD_TAXON, 0L)
            if (long != 0L) {
                return Date(long)
            } else {
                return null
            }
    } set(value) {
        return prefs.edit().putLong(LAST_DOWNLOAD_TAXON, value?.time ?: 0L).apply()
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
