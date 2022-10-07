package com.noque.svampeatlas.utilities

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.noque.svampeatlas.extensions.Date
import com.noque.svampeatlas.models.Locality
import java.util.*
import com.google.gson.Gson
import com.noque.svampeatlas.BuildConfig
import com.noque.svampeatlas.extensions.difDays
import com.noque.svampeatlas.extensions.difHours
import com.noque.svampeatlas.models.Location


object SharedPreferences {

    private const val TOKEN_KEY = "TOKEN_KEY"
    private const val LOCKED_SUBSTRATE_ID = "LOCKED_SUBSTRATE_ID"
    private const val LOCKED_VEGETATIONTYPE_ID = "LOCKED_VEGETATIONTYPE_ID"
    private const val LOCKED_HOSTS = "LOCKED_HOSTS"
    private const val HAS_ACCEPTED_IDENTIFCATION_TERMS = "HAS_ACCEPTED_IDENTIFCATION_TERMS"
    private const val SAVE_IMAGES = "SAVE_IMAGES"
    private const val SAVE_IMAGES_DECIDED = "SAVE_IMAGES_DECIDED"
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


    /// Keeps track of how many sent observations it has been since user was last reminded about precision importance
    private var positionReminderObservationCount: Int get() {
            return prefs.getInt("positionReminderObservationCount", -1)
        } set(value) {
            prefs.edit().putInt("positionReminderObservationCount", value).apply()
        }

    fun decreasePositionReminderCounter() {
        positionReminderObservationCount -= 1
    }

     fun shouldShowPositionReminder(): Boolean {
        return (shouldShowPositionReminderToggle && positionReminderObservationCount <= 0)
    }

     fun setHasShownPositionReminder() {
        prefs.edit().putInt("positionReminderObservationCount", 20).apply()
    }

    /// Wether the user would like to recieve position reminders, toggleable in settings.
    var shouldShowPositionReminderToggle: Boolean get() {
        return if (prefs.contains("shouldShowPositionReminderToggle")) prefs.getBoolean("shouldShowPositionReminderToggle", true) else true
    } set(value) {
        prefs.edit().putBoolean("shouldShowPositionReminderToggle", value).apply()
    }

    var hasSeenWhatsNew: Boolean get() {
        var lastOpenedVersion  = prefs.getString("lastOpenedVersion", "")
        if (lastOpenedVersion?.startsWith(BuildConfig.VERSION_NAME.first()) == true) lastOpenedVersion = BuildConfig.VERSION_NAME
        return lastOpenedVersion == BuildConfig.VERSION_NAME
    } set(_) {
        prefs.edit().putString("lastOpenedVersion", BuildConfig.VERSION_NAME).apply()
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

    var locationLockedDate: Date? get() {
        val longDate = prefs.getLong("locationLockedDate", 0)
        return if (longDate != 0L) Date(longDate) else null
    } set(value) {
        if (value != null) {
            prefs.edit().putLong("locationLockedDate", value.time).apply()
        } else {
            prefs.edit().remove("locationLockedDate").apply()
        }
    }

    var localityLockedDate: Date? get() {
        val longDate = prefs.getLong("localityLockedDate", 0)
        return if (longDate != 0L) Date(longDate) else null
    } set(value) {
        if (value != null) {
            prefs.edit().putLong("localityLockedDate", value.time).apply()
        } else {
            prefs.edit().remove("localityLockedDate").apply()
        }
    }

    var lockedLocality: Locality? get() {
        val date = localityLockedDate
        if (date != null && date.difHours() >= 1L) {
          return null
        }
        val json = prefs.getString(LOCALITY_LOCKED, null)
        return if(json != null) Gson().fromJson(json, Locality::class.java) else null
    } set(value) {
        if (value != null) {
            prefs.edit().putString(LOCALITY_LOCKED, Gson().toJson(value)).apply()
            localityLockedDate = Date()
        } else {
            prefs.edit().remove(LOCALITY_LOCKED).apply()
            localityLockedDate = null
        }
    }


    var lockedLocation: Location? get() {
        val date = locationLockedDate
        if (date != null && date.difHours() >= 1L) {
            return null
        }

        val json = prefs.getString(LOCATION_LOCKED, null)
        return if(json != null) Gson().fromJson(json, Location::class.java) else null
    } set(value) {
        locationLockedDate = if (value != null) {
            prefs.edit().putString(LOCATION_LOCKED, Gson().toJson(value)).apply()
            Date()
        } else {
            prefs.edit().remove(LOCATION_LOCKED).apply()
            null
        }
    }

    // Taxon data

    var databaseShouldUpdate: Boolean get()  {
        val lastDataUpdateDate = lastDataUpdateDate
        return !(lastDataUpdateDate != null && lastDataUpdateDate.difDays() < 30)
    } set(value) {
        lastDataUpdateDate = if (value) Date(3) else Date()
    }

    fun databaseWasUpdated() {
        lastDataUpdateDate = Date()
    }

    fun databasePresent(): Boolean {
        return lastDataUpdateDate != null
    }

    private var lastDataUpdateDate: Date?
        get() {
        val long = prefs.getLong(LAST_DOWNLOAD_TAXON, 0L)
            return if (long != 0L) {
                Date(long)
            } else {
                null
            }
    } set(value) {
        return prefs.edit().putLong(LAST_DOWNLOAD_TAXON, value?.time ?: 0L).apply()
    }

    // Soem

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
