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

class Observable {
    companion object {
        fun <X>observe(
            source: MutableLiveData<X>, onCallback: ((X) -> Unit)
        ): MutableLiveData<X> {
            source.observeForever {
                onCallback(it)
            }
            return source
        }
    }
}

class ListenableUserObservation(private val onChanged: (UserObservation) -> Unit) {

     var userObservation = UserObservation()

     val images = Observable.observe(MutableLiveData<List<UserObservation.Image>>()) {
         userObservation.images = it
     }

    // Page 1 properties
     val mushroom = Observable.observe(MutableLiveData<Pair<Mushroom, DeterminationConfidence>?>()) {
           userObservation.mushroom = it
    }

    var determinationNotes: String?
    get() = userObservation.determinationNotes
    set(value) {
        userObservation.determinationNotes = value
    }


    // Page 2 properties
    val observationDate = Observable.observe(MutableLiveData<Date>()) {
        userObservation.observationDate
    }
    val substrate = Observable.observe(MutableLiveData<Pair<Substrate, Boolean>?>()) {
        userObservation.substrate = it
    }
     val vegetationType = Observable.observe(MutableLiveData<Pair<VegetationType, Boolean>?>()) {
         userObservation.vegetationType = it
     }

     val hosts = Observable.observe(MutableLiveData<Pair<List<Host>, Boolean>?>()) {
         userObservation.hosts = it
     }

    val notes = Observable.observe(MutableLiveData<String?>()) {
        userObservation.notes = it
    }
     val ecologyNotes = Observable.observe(MutableLiveData<String?>()) {
         userObservation.ecologyNotes = it
     }

    // Page 3 properties
     val locality = Observable.observe(MutableLiveData<Pair<Locality?, Boolean>?>()) {
        userObservation.locality = it
    }
     val location = Observable.observe(MutableLiveData<Pair<Location?, Boolean>?>()) {
         userObservation.location = it
     }


    fun set(value: UserObservation) {
        userObservation.deleteTempimages()

        userObservation = value
        onChanged.invoke(value)
        images.value = value.images
        mushroom.value = value.mushroom
        observationDate.value = value.observationDate
        substrate.value = value.substrate
        vegetationType.value = value.vegetationType
        hosts.value = value.hosts
        notes.value = value.notes
        ecologyNotes.value = value.ecologyNotes
        locality.value = value.locality
        location.value = value.location
    }
}

class UserObservation(private val creationDate: Date = Date()) {

    sealed class Error(title: Int, message: Int) :
        AppError2(title, message, null) {
        object NoMushroomError : Error(
            R.string.error_newObservation_missingInformation_title,
            R.string.error_newObservation_noMushroom_message
        )

        object NoSubstrateError : Error(
            R.string.error_newObservation_missingInformation_title,
            R.string.error_newObservation_noSubstrategroup_message
        )

        object NoVegetationTypeError : Error(
            R.string.error_newObservation_missingInformation_title,
            R.string.error_newObservation_noVegetationType_message
        )

        object NoLocationDataError : Error(
            R.string.error_newObservation_noCoordinates_title,
            R.string.error_newObservation_noCoordinates_message
        )
        object NoLocalityDataError: Error(
            R.string.error_newObservation_noLocality_title,
            R.string.error_newObservation_noLocality_message
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
    var locality: Pair<Locality?, Boolean>? = null
    var location: Pair<Location?, Boolean>? = null

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
            if (species != null) {
                mushroom = Pair(
                    species,
                    DeterminationConfidence.fromDatabaseName(
                        newObservation.confidence
                            ?: DeterminationConfidence.CONFIDENT.databaseName
                    )
                )
            } else {
                mushroom = null
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
            newObservation.locality?.let {Pair(it, false) }
            newObservation.coordinate?.let { Pair(it, false) }
    }

    fun isValid(): Error? {
        if (mushroom == null) return Error.NoMushroomError
        if (substrate == null) return Error.NoSubstrateError
        if (vegetationType == null) return Error.NoVegetationTypeError
        if (locality == null || locality == null) return Error.NoLocalityDataError
        return null
    }

    fun asJSON(includeTaxon: Boolean): JSONObject {
        return JSONObject().apply {
            put("os", "Android")
            put("browser", "Native App - ${BuildConfig.VERSION_NAME}")
            put("observationDate", (observationDate?.toDatabaseName()))
            put("substrate_id", substrate?.first?.id)
            put("vegetationtype_id", vegetationType?.first?.id)
            put("associatedOrganisms", JSONArray().apply {
                hosts?.first?.map { JSONObject().put("_id", it.id) }?.forEach { this.put(it) }
            })
            put("ecologynote", ecologyNotes)
            put("note", notes)
            put("decimalLatitude", location?.first?.latLng?.latitude)
            put("decimalLongitude", location?.first?.latLng?.longitude)
            put("accuracy", location?.first?.accuracy)

            if (locality?.first?.geoName != null) {
                put("geonameId", locality?.first?.geoName?.geonameId)
                put(
                    "geoname", JSONObject()
                        .put("geonameId", locality?.first?.geoName?.geonameId)
                        .put("name", locality?.first?.geoName?.name)
                        .put("adminName1", locality?.first?.geoName?.adminName1)
                        .put("lat", locality?.first?.geoName?.lat)
                        .put("lng", locality?.first?.geoName?.lng)
                        .put("countryName", locality?.first?.geoName?.countryName)
                        .put("countryCode", locality?.first?.geoName?.countryCode)
                        .put("fcodeName", locality?.first?.geoName?.fcodeName)
                        .put("fclName", locality?.first?.geoName?.fclName)
                )
            } else {
                put("locality_id", locality?.first?.id)
            }

            if (includeTaxon) {
                put(
                    "determination",
                    JSONObject()
                        .put(
                            "confidence",
                            mushroom?.second?.databaseName
                                ?: DeterminationConfidence.CONFIDENT.databaseName
                        )
                        .put("taxon_id", mushroom?.first?.id)
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
            observationDate ?: Date(),
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