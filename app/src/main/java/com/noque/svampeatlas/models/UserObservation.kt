package com.noque.svampeatlas.models

import android.util.Log
import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.noque.svampeatlas.BuildConfig
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.Date
import com.noque.svampeatlas.extensions.difDays
import com.noque.svampeatlas.extensions.toDatabaseName
import com.noque.svampeatlas.services.FileManager
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.view_models.Session
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.properties.Delegates


class UserObservation(private val creationDate: Date = Date()) {

    sealed class Error(title: Int, message: Int) :
        AppError2(title, message, null) {
        object NoMushroomError : Error(
            R.string.newObservationError_missingInformation,
            R.string.newObservationError_noMushroom_message
        )

        object NoSubstrateError : Error(
            R.string.newObservationError_missingInformation,
            R.string.newObservationError_noSubstrateGroup_message
        )

        object NoVegetationTypeError : Error(
            R.string.newObservationError_missingInformation,
            R.string.newObservationError_noVegetationType_message
        )

        object NoLocationDataError : Error(
            R.string.newObservationError_noCoordinates_title,
            R.string.newObservationError_noCoordinates_message
        )
        object NoLocalityDataError: Error(
            R.string.newObservationError_noLocality_title,
            R.string.newObservationError_noLocality_message
        )

        object LowAccuracy: Error(
            R.string.error_addObservationError_lowAccuracy,
            R.string.newObservationError_tooInaccurate
        )
    }

    // This class is a container for the different types of images encountered when dealing with a user observation.
    // Some images are new, and only stored locally, and some could be stored online.
    sealed class Image {
        class LocallyStored(val file: File) : Image()
        class New(val file: File) : Image()
        class Hosted(
            val id: Int,
            val url: String,
            val creationDate: Date?,
            val userIsValidator: Boolean
        ) : Image() {
            val isDeletable: Boolean
                get() {
                    return when {
                        userIsValidator -> true
                        (creationDate != null && creationDate.difDays() <= 7) -> true
                        else -> false
                    }
                }
        }
    }

    // General properties
    var images: List<Image> = listOf()

    // Page 1 properties
    var mushroom: Pair<Mushroom, DeterminationConfidence>? = null
    var determinationNotes: String? = null

    // Page 2 properties
    var observationDate = Date()
    var substrate: Pair<Substrate, Boolean>? = null
    var vegetationType: Pair<VegetationType, Boolean>? = null
    var hosts: Pair<List<Host>, Boolean>? = null
    var notes: String? = null
    var ecologyNotes: String? = null

    // Page 3 properties
    var locality: Pair<Locality, Boolean>? = null
    var location: Pair<Location, Boolean>? = null

    constructor(): this(creationDate = Date()) {
        SharedPreferences.getSubstrateID()?.let {
            RoomService.substrates.getSubstrateWithID(it).onSuccess {
                substrate = Pair(it, true)
            }
        }

        SharedPreferences.getVegetationTypeID()?.let {
            RoomService.vegetationTypes.getVegetationTypeWithID(it).onSuccess {
                vegetationType = Pair(it, true)
            }
        }

        SharedPreferences.getHosts()?.let {
            RoomService.hosts.getHostsWithIds(it).onSuccess {
                hosts = Pair(it.toMutableList(), true)
            }
        }

        SharedPreferences.lockedLocality?.let {
            locality = Pair(it, true)
        }

        SharedPreferences.lockedLocation?.let {
            location = Pair(it, true)
        }
    }

    constructor(observation: Observation) : this(creationDate = observation.createdAt ?: Date()) {
        observationDate = observation.observationDate ?: Date()
        mushroom = Pair(
            Mushroom(
                observation.id,
                observation.determination.fullName,
                VernacularNameDK(observation.determination.localizedName, null)
            ), observation.determination.confidence ?: DeterminationConfidence.CONFIDENT
        )
        observation.substrate?.let { substrate = Pair(it, false) }
        observation.vegetationType?.let { vegetationType = Pair(it, false) }
        hosts = Pair(observation.hosts.toMutableList(), false)
        notes = observation.note
        ecologyNotes = observation.ecologyNote
        images = observation.images.map {
            Image.Hosted(
                it.id,
                it.url,
                Date(it.createdAt),
                Session.user.value?.isValidator ?: false
            )
        }.toMutableList()
        determinationNotes = null
        observation.location?.let { location = Pair(it, false) }
        observation.locality?.let { locality = Pair(it, false)  }
    }

    constructor(newObservation: NewObservation): this(creationDate = newObservation.creationDate) {
        observationDate = newObservation.observationDate
        val species = newObservation.species
        mushroom = if (species != null) {
            Pair(
                species,
                DeterminationConfidence.fromDatabaseName(
                    newObservation.confidence
                        ?: DeterminationConfidence.CONFIDENT.databaseName
                )
            )
        } else {
            null
        }

        newObservation.substrate?.let { substrate = Pair(it, false) }
        newObservation.vegetationType?.let { vegetationType = Pair(it, false) }
        notes = newObservation.note
        ecologyNotes = newObservation.ecologyNote
        RoomService.hosts.getHostsWithIds(newObservation.hostIDs).apply {
            onSuccess {
                hosts = Pair(it, false)
            }
        }

        images = newObservation.images.map {
            Image.LocallyStored(File(it))
        }.toMutableList()
        newObservation.locality?.let { locality =  Pair(it, SharedPreferences.lockedLocality?.id == it.id) }
        newObservation.coordinate?.let { location =  Pair(it, SharedPreferences.lockedLocation?.latLng == it.latLng) }
    }

    fun isValid(): Error? {
        val location = location
        if (location == null || locality == null) return Error.NoLocalityDataError
        if (location.first.accuracy > 150) return Error.LowAccuracy
        if (substrate == null) return Error.NoSubstrateError
        if (vegetationType == null) return Error.NoVegetationTypeError
        if (mushroom == null) return Error.NoMushroomError
        return null
    }

    fun asJSON(includeTaxon: Boolean): JSONObject? {
        val substrate = substrate?.first
        val vegetationType = vegetationType?.first
        val locality = locality?.first
        val location = location?.first
        val hosts = hosts?.first
        if (substrate == null || vegetationType == null || locality == null || location == null) return null

        return JSONObject().apply {
            put("os", "Android")
            put("browser", "Native App - ${BuildConfig.VERSION_NAME}")
            put("observationDate", (observationDate.toDatabaseName()))
            put("substrate_id", substrate.id)
            put("vegetationtype_id", vegetationType.id)
            put("decimalLatitude", location.latLng.latitude)
            put("decimalLongitude", location.latLng.longitude)
            put("accuracy", location.accuracy)

            if (ecologyNotes?.isEmpty() == false) put("ecologynote", ecologyNotes)
            if (notes?.isEmpty() == false) put("note", notes)
            if (hosts?.isEmpty() == false) put("associatedOrganisms", JSONArray().apply {
                hosts.map { JSONObject().put("_id", it.id) }.forEach { this.put(it) }
            })

            if (locality.geoName != null) {
                put("geonameId", locality.geoName?.geonameId)
                put(
                    "geoname", JSONObject()
                        .put("geonameId", locality.geoName.geonameId)
                        .put("name", locality.geoName.name)
                        .put("adminName1", locality.geoName.adminName1)
                        .put("lat", locality.geoName.lat)
                        .put("lng", locality.geoName.lng)
                        .put("countryName", locality.geoName.countryName)
                        .put("countryCode", locality.geoName.countryCode)
                        .put("fcodeName", locality.geoName.fcodeName)
                        .put("fclName", locality.geoName.fclName)
                )
            } else {
                put("locality_id", locality.id)
            }

            val mushroom = mushroom
            if (includeTaxon && mushroom != null) {
                put(
                    "determination",
                    JSONObject()
                        .put(
                            "confidence",
                            mushroom.second.databaseName
                                ?: DeterminationConfidence.CONFIDENT.databaseName
                        )
                        .put("taxon_id", mushroom.first.id)
                        .put("notes", determinationNotes ?: "")
                )
            }
        }
    }

    suspend fun asNewObservation(): NewObservation {
        val locallyStoredImages = images.mapNotNull { (it as? Image.LocallyStored)?.file }
        val newImages = saveNewImagesLocally()
        val combinedImages = locallyStoredImages + newImages
        return NewObservation(
            creationDate,
            observationDate,
            mushroom?.first,
            locality?.first,
            substrate?.first,
            vegetationType?.first,
            location?.first,
            ecologyNotes,
            notes,
            mushroom?.second?.databaseName,
            determinationNotes,
            hosts?.first?.map { it.id } ?: listOf(),
            combinedImages.map { it.absolutePath }
        )
    }

    fun getImagesForUpload(): List<File> {
        return images.mapNotNull {
            when (it) {
                is Image.LocallyStored -> it.file
                is Image.New -> it.file
                is Image.Hosted -> null
            }
        }
    }

    private suspend fun saveNewImagesLocally(): List<File> {
        return images.mapNotNull {
            when (it) {
                is Image.LocallyStored -> null
                is Image.New -> {
                    when (val result = FileManager.saveAsNotesImage(it.file)) {
                        is Result.Success -> {
                            it.file.delete()
                            result.value
                        }
                        is Result.Error -> null
                    }

                }
                is Image.Hosted -> null
            }
        }
    }

    fun deleteTempimages() {
        images.forEach {
            when (it) {
                is Image.New -> it.file.delete()
                is Image.Hosted, is Image.LocallyStored -> return
            }
        }
    }

    fun deleteAllImages() {
        images.forEach {
            when (it) {
                is Image.New -> it.file.delete()
                is Image.LocallyStored -> FileManager.deleteImageGalleryFile(it.file)
                is Image.Hosted -> return
            }
        }
    }
}