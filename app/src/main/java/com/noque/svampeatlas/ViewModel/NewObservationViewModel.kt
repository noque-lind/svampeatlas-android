package com.noque.svampeatlas.ViewModel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonArray
import com.google.maps.android.SphericalUtil
import com.noque.svampeatlas.Extensions.toSimpleString
import com.noque.svampeatlas.Model.*
import com.noque.svampeatlas.Services.DataService
import com.noque.svampeatlas.Services.LocationService
import com.noque.svampeatlas.Utilities.Geometry
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class NewObservationViewModel(application: Application) : AndroidViewModel(application) {

    sealed class NewObservationViewModelError(title: String, message: String): AppError(title, message) {
        class NoMushroomError(): NewObservationViewModelError("Du mangler at specificere en art", "For at uploade en observation, skal du enten tilføje billeder, eller identificere en art.")
        class NoSubstrateError(): NewObservationViewModelError("Manglende information", "Du skal opgive en substrat til din observation")
        class NoVegetationTypeError(): NewObservationViewModelError("Manglende information", "Du skal opgive en vegetationtype til din observation")
        class NoLocationDataError(): NewObservationViewModelError("Hvor er du?", "Du skal hjælpe med at fortælle hvad du er i nærheden af")
    }


    companion object {
        val TAG = "NewObservationViewModel"
    }

    enum class DeterminationConfidence(val databaseName: String) {
        CONFIDENT("sikker"),
        LIKELY("sandsynlig"),
        POSSIBLE("mulig");

        companion object {
            val values = values()
        }
    }


    private val _date by lazy {MutableLiveData<Date>()}
    private val _locality by lazy {MutableLiveData<Locality?>()}
    private val _coordinate by lazy {MutableLiveData<LatLng?>()}
    private val _substrate by lazy {MutableLiveData<Pair<Substrate, Boolean>?>()}
    private val _vegetationType by lazy {MutableLiveData<Pair<VegetationType, Boolean>?>()}
    private val _hosts by lazy { MutableLiveData<Pair<MutableList<Host>, Boolean>?>()}
    private val _images by lazy {MutableLiveData<MutableList<Bitmap>>()}
    private val _mushroom by lazy {MutableLiveData<Pair<Mushroom, DeterminationConfidence>?>()}
    private var _notes: String? = null
    private var _ecologyNotes: String? = null

    private val _uploadState by lazy {MutableLiveData<State<Pair<Int, Int>>>()}
    val uploadState: LiveData<State<Pair<Int, Int>>> get() = _uploadState
    private val _localityState by lazy { MutableLiveData<State<List<Locality>>>() }
    val localityState: LiveData<State<List<Locality>>> get() = _localityState



    val date: LiveData<Date> get() = _date
    val substrate: LiveData<Pair<Substrate, Boolean>?> get() = _substrate
    val vegetationType: LiveData<Pair<VegetationType, Boolean>?> get() = _vegetationType
    val hosts: LiveData<Pair<MutableList<Host>, Boolean>?> get() = _hosts
    val locality: LiveData<Locality?> get() = _locality
    val coordinate: LiveData<LatLng?> get() = _coordinate
    val notes get() = _notes
    val ecologyNotes get() = _ecologyNotes
    val images: LiveData<MutableList<Bitmap>> get() = _images
    val mushroom: LiveData<Pair<Mushroom, DeterminationConfidence>?> get() = _mushroom

    init {
        _date.value = Calendar.getInstance().time
    }

    fun setDate(date: Date) {
        _date.value = date
    }

    fun setLocality(locality: Locality) {
        Log.d(TAG, "Locality set to ${locality.toString()}")
        _locality.value = locality
    }

    fun setCoordinate(coordinate: LatLng) {
        _coordinate.value = coordinate
    }

    fun setSubstrate(substrate: Substrate, isLocked: Boolean) {
        _substrate.value = Pair(substrate, isLocked)
    }

    fun setVegetationType(vegetationType: VegetationType, isLocked: Boolean) {
        _vegetationType.value = Pair(vegetationType, isLocked)
    }

    fun appendHost(host: Host, isLocked: Boolean) {
        _hosts.value?.let {
            it.first.add(host)
            _hosts.value = Pair(it.first, isLocked)
            return
        }

        _hosts.value = Pair(mutableListOf(host), isLocked)
    }

    fun removeHost(host: Host, isLocked: Boolean) {
        _hosts.value?.let {
            if (it.first.remove(host)) _hosts.value = Pair(it.first, isLocked)
        }
    }

    fun setNotes(notes: String?) {
        _notes = notes
    }

    fun setEcologyNotes(ecologyNotes: String?) {
        _ecologyNotes = ecologyNotes
    }

    fun appendImage(image: Bitmap) {
        Log.d(TAG, image.toString())

        _images.value?.let {
            it.add(image)
            _images.value = it
            return
        }

        _images.value = mutableListOf(image)
    }


    fun setLocationError(error: LocationService.Error) {
        _localityState.value = State.Error(error)
    }

    fun setMushroom(mushroom: Mushroom?) {
        Log.d(TAG, "Setting Mushroom to: ${mushroom?.fullName} if its value is not the same as it already is.")

        if (this.mushroom.value?.first != mushroom) {
            if (mushroom != null) {
                _mushroom.value = Pair(mushroom, DeterminationConfidence.CONFIDENT)
            } else {
                _mushroom.value = null
            }
        }
    }

    fun setConfidence(confidence: DeterminationConfidence) {
        mushroom.value?.let {
            _mushroom.value = Pair(it.first, confidence)
            Log.d(TAG, "Setting Confidence to ${confidence.toString()}")
        }
    }

    fun resetLocationData() {
        _localityState.value = State.Empty()
        _coordinate.value = null
        _locality.value = null
    }


    fun getLocalities(latLng: LatLng) {
        _localityState.value = State.Loading()

        val geometry = Geometry(
            latLng,
            1000.0,
            Geometry.Type.RECTANGLE
        )

        DataService.getInstance(getApplication()).getLocalities(latLng) { result ->
            result.onSuccess {
                Log.d(TAG, it.toString())
                _localityState.value = State.Items(it)
                assignClosestLocality(latLng, it)
            }

            result.onError {
                _localityState.value = State.Error(it)
            }
        }
    }

    fun reset() {
        _uploadState.value = State.Empty()
        _localityState.value = State.Empty()


        _date.value = Calendar.getInstance().time
        _locality.value = null
        _coordinate.value = null
        _substrate.value = null
        _vegetationType.value = null
        _hosts.value = null
        _images.value = mutableListOf()
        _mushroom.value = null
        _notes = null
        _ecologyNotes = null
    }

    private fun assignClosestLocality(location: LatLng, localities: List<Locality>) {
        var closest = localities.first()

        localities.forEach {
            if (SphericalUtil.computeDistanceBetween(location, it.location) < SphericalUtil.computeDistanceBetween(location, closest.location)) { closest = it }
        }

        _locality.value = closest
    }


    fun upload(primaryUser: User): NewObservationViewModelError? {
        val date = date.value
        val mushroom = mushroom.value
        val substrate = substrate.value?.first
        val vegetationType = vegetationType.value?.first
        val coordinate = coordinate.value
        val locality = locality.value
        val hosts = hosts.value?.first

        if (mushroom == null) {
            return NewObservationViewModelError.NoMushroomError()
        }

        if (vegetationType == null) {
            return NewObservationViewModelError.NoVegetationTypeError()
        }

        if (substrate == null) {
            return NewObservationViewModelError.NoSubstrateError()
        }

        if (coordinate == null) {
            return NewObservationViewModelError.NoLocationDataError()
        }

        if (locality == null) {
            return NewObservationViewModelError.NoLocationDataError()
        }

        _uploadState.value = State.Loading()

       val jsonObject = JSONObject()
        jsonObject.put("observationDate", (date?.toSimpleString()) ?: Calendar.getInstance().time.toSimpleString())
        jsonObject.put("os", "Android")
        jsonObject.put("browser", "Native App")
        jsonObject.put("substrate_id", substrate.id)
        jsonObject.put("vegetationtype_id", vegetationType.id)

        val hostArray = JSONArray()
        hosts?.forEach { hostArray.put(JSONObject().put("_id", it.id))}

        jsonObject.put("associatedOrganisms", hostArray)
        jsonObject.put("ecologynote", ecologyNotes)
        jsonObject.put("note", notes)
        jsonObject.put("decimalLatitude", coordinate.latitude)
        jsonObject.put("decimalLongitude", coordinate.longitude)

        // FIIIX
        jsonObject.put("accuracy", 60)

        val usersArray = JSONArray()

            listOf(primaryUser).forEach { usersArray.put(
            JSONObject()
            .put("_id", it.id)
            .put("Initialer", it.initials)
            .put("facebook", it.facebookID)
            .put("name", it.name))}

        jsonObject.put("users", usersArray)

        if (mushroom == null) {
            jsonObject.put("determination",
                JSONObject()
                .put("confidence", "sikker")
                .put("taxon_id", 60212)
                .put("user_id", primaryUser.id))
        } else {
            jsonObject.put("determination",
                JSONObject()
                    .put("confidence", mushroom.second.databaseName)
                    .put("taxon_id", mushroom.first.id)
                    .put("user_id", primaryUser.id))
        }

        jsonObject.put("locality_id", locality.id)

        DataService.getInstance(getApplication()).uploadObservation(jsonObject, images.value?.toList()) {
            it.onError {
                _uploadState.value = State.Error(it)
            }

            it.onSuccess {
                _uploadState.value = State.Items(it)
            }
        }

        return null
    }


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "NEwObser cleared")
    }

}