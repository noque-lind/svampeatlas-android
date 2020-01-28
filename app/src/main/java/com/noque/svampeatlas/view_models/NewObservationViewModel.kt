package com.noque.svampeatlas.view_models

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import com.google.maps.android.SphericalUtil
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.getBitmap
import com.noque.svampeatlas.extensions.toSimpleString
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.Geometry
import com.noque.svampeatlas.utilities.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

class NewObservationViewModel(application: Application) : AndroidViewModel(application) {

    sealed class Error(title: String, message: String): AppError(title, message) {
        class NoMushroomError(context: Context): Error(context.getString(R.string.newObservationViewModelError_noMushroom_title), context.getString(R.string.newObservationViewModelError_noMushroom_message))
        class NoSubstrateError(context: Context): Error(context.getString(R.string.newObservationViewModelError_noSubstrate_title), context.getString(R.string.newObservationViewModelError_noSubstrate_message))
        class NoVegetationTypeError(context: Context): Error(context.getString(R.string.newObservationViewModelError_noVegetationType_title), context.getString(R.string.newObservationViewModelError_noVegetationType_message))
        class NoLocationDataError(context: Context): Error(context.getString(R.string.newObservationViewModelError_noLocation_title), context.getString(R.string.newObservationViewModelError_noLocation_message))
        class LocalityFetchError(context: Context): Error(context.getString(R.string.newObservationViewModelError_localityFetch_title), context.getString(R.string.newObservationViewModelError_localityFetch_message))
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
    private val _images by lazy {MutableLiveData<MutableList<File>?>()}
    private val _mushroom by lazy {MutableLiveData<Pair<Mushroom, DeterminationConfidence>?>()}

    private var coordinateAccuracy: Float = 60F
    private var _notes: String? = null
    private var _ecologyNotes: String? = null
    private var _determinationNotes: String? = null


    private val _localityState by lazy { MutableLiveData<State<List<Locality>>>() }
    val localityState: LiveData<State<List<Locality>>> get() = _localityState

    private val _predictionResultsState by lazy { MutableLiveData<State<List<PredictionResult>>>() }
    val predictionResultsState: LiveData<State<List<PredictionResult>>> get() = _predictionResultsState


    val date: LiveData<Date> get() = _date
    val substrate: LiveData<Pair<Substrate, Boolean>?> get() = _substrate
    val vegetationType: LiveData<Pair<VegetationType, Boolean>?> get() = _vegetationType
    val hosts: LiveData<Pair<MutableList<Host>, Boolean>?> get() = _hosts
    val locality: LiveData<Locality?> get() = _locality
    val coordinate: LiveData<LatLng?> get() = _coordinate
    val notes get() = _notes
    val ecologyNotes get() = _ecologyNotes
    val images: LiveData<MutableList<File>?> get() = _images
    val mushroom: LiveData<Pair<Mushroom, DeterminationConfidence>?> get() = _mushroom

    init {
        setup()
    }

    private fun setup() {
        _date.value = Calendar.getInstance().time
        _coordinate.value = null
        _mushroom.value = null
        _localityState.value = State.Empty()
        _predictionResultsState.value = State.Empty()

        viewModelScope.launch {
            SharedPreferencesHelper(getApplication()).getSubstrateID()?.let {
                RoomService.getInstance(getApplication()).getSubstrateWithID(it).onSuccess {
                    _substrate.value = Pair(it, true)
                }
            }

            SharedPreferencesHelper(getApplication()).getVegetationTypeID()?.let {
                RoomService.getInstance(getApplication()).getVegetationTypeWithID(it).onSuccess {
                    _vegetationType.value = Pair(it, true)
                }
            }

            SharedPreferencesHelper(getApplication()).getHosts()?.let {
                RoomService.getInstance(getApplication()).getHostsWithIds(it).onSuccess {
                    _hosts.value = Pair(it.toMutableList(), true)
                }
            }
        }
    }

    fun setDate(date: Date) {
        _date.value = date
    }

    fun setLocality(locality: Locality) {
        _locality.value = locality
    }

    fun setCoordinate(coordinate: LatLng, accuracy: Float) {
        _coordinate.value = coordinate
        this.coordinateAccuracy = accuracy
        getLocalities(coordinate)
    }

    fun setSubstrate(substrate: Substrate, isLocked: Boolean) {

        _substrate.value = Pair(substrate, isLocked)

        SharedPreferencesHelper(getApplication()).saveSubstrateID(if (isLocked) substrate.id else null)
        if (isLocked) {
            viewModelScope.launch {
                RoomService.getInstance(getApplication()).saveSubstrate(substrate)
            }
        }
    }

    fun setVegetationType(vegetationType: VegetationType, isLocked: Boolean) {
        _vegetationType.value = Pair(vegetationType, isLocked)

        SharedPreferencesHelper(getApplication()).saveVegetationTypeID(if (isLocked) vegetationType.id else null)

        if (isLocked) {
            viewModelScope.launch {
                RoomService.getInstance(getApplication()).saveVegetationType(vegetationType)
            }
        }
    }

    fun appendHost(host: Host, isLocked: Boolean) {
        var hosts = hosts.value?.first

        if (hosts == null) {
            hosts = mutableListOf(host)
        } else {
            hosts.add(host)
        }

        _hosts.value = Pair(hosts, isLocked)

        SharedPreferencesHelper(getApplication()).saveHostsID(hosts.map { it.id })

        if (isLocked) {
            viewModelScope.launch {
                RoomService.getInstance(getApplication()).saveHosts(hosts)
            }
        }
    }

    fun setHostsLockedState(value: Boolean) {
        if (hosts.value?.second != value)
        _hosts.value = Pair(hosts.value?.first ?: mutableListOf(), value)
    }

    fun removeHost(host: Host, isLocked: Boolean) {
        _hosts.value?.let {
            if (it.first.remove(host)) _hosts.value = Pair(it.first, isLocked)
        }

        SharedPreferencesHelper(getApplication()).saveHostsID(hosts.value?.first?.map { it.id })
    }

    fun appendImage(imageFile: File) {
        var images = images.value

        if (images == null) {
            images = mutableListOf(imageFile)
            if (mushroom.value?.first == null) getPredictions(imageFile)
        } else {
            images.add(imageFile)
        }

        _images.value = images
    }

    fun removeImageAt(position: Int) {
        if (_images.value != null && _images.value!!.lastIndex >= position) {
            _images.value?.removeAt(position)

            if (_images.value?.count() == 0) {
                _images.value = null
                _predictionResultsState.value = State.Empty()
            }
        }
    }

    fun setMushroom(mushroom: Mushroom?) {
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
        }
    }

    fun setDeterminationNotes(notes: String?) {
        _determinationNotes = notes
    }

    fun setNotes(notes: String?) {
        _notes = notes
    }

    fun setEcologyNotes(ecologyNotes: String?) {
        _ecologyNotes = ecologyNotes
    }


    fun setLocationError(error: LocationService.Error) {
        _localityState.value = State.Error(error)
    }

    fun resetLocationData() {
        _localityState.value = State.Empty()
        _coordinate.value = null
        _locality.value = null
    }

    fun reset() {
        _date.value = null
        _locality.value = null
        _coordinate.value = null
        coordinateAccuracy = 60F
        _substrate.value = null
        _vegetationType.value = null
        _hosts.value = null
        _images.value = null
        _mushroom.value = null
        _notes = null
        _ecologyNotes = null
        _determinationNotes = null

        setup()
    }

    private fun getLocalities(latLng: LatLng) {
        _localityState.value = State.Loading()

            DataService.getInstance(getApplication()).getLocalities(TAG, latLng) { result ->
                result.onSuccess {
                    if (it.isNotEmpty()) {
                        _localityState.value = State.Items(it)
                        assignClosestLocality(latLng, it)
                    } else {
                        _localityState.value = State.Error(Error.LocalityFetchError(getApplication()))
                    }
                }

                result.onError {
                    _localityState.value = State.Error(it)
                }
            }
    }

    private fun assignClosestLocality(location: LatLng, localities: List<Locality>) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                var closest = localities.first()

                localities.forEach {
                    if (SphericalUtil.computeDistanceBetween(location, it.location) < SphericalUtil.computeDistanceBetween(location, closest.location)) { closest = it }
                }

                _locality.postValue(closest)
            }
        }
    }

    private fun getPredictions(imageFile: File) {
        _predictionResultsState.value = State.Loading()

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getPredictions(imageFile) { it ->
                it.onError { _predictionResultsState.value = State.Error(it) }
                it.onSuccess { _predictionResultsState.value = State.Items(it) }
            }
        }
    }

    fun prepareForUpload(userID: Int): Result<Pair<JSONObject, List<File>?>, Error> {
        val date = date.value
        val mushroom = mushroom.value
        val substrate = substrate.value?.first
        val vegetationType = vegetationType.value?.first
        val coordinate = coordinate.value
        val locality = locality.value
        val hosts = hosts.value?.first

        if (mushroom == null) {
            return Result.Error(Error.NoMushroomError(getApplication()))
        }

        if (vegetationType == null) {
            return Result.Error(Error.NoVegetationTypeError(getApplication()))
        }

        if (substrate == null) {
            return Result.Error(Error.NoSubstrateError(getApplication()))
        }

        if (coordinate == null) {
            return Result.Error(Error.NoLocationDataError(getApplication()))
        }

        if (locality == null) {
            return Result.Error(Error.NoLocationDataError(getApplication()))
        }

        if (predictionResultsState.value is State.Items && _determinationNotes == null) {
            val predictionResults = predictionResultsState.value as State.Items
            val selectedPrediction = predictionResults.items.firstOrNull { it.mushroom.id == mushroom.first.id }
            _determinationNotes = PredictionResult.getNotes(selectedPrediction, predictionResults.items)
        }

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
        jsonObject.put("accuracy", coordinateAccuracy)

        if (locality.geoName != null) {
            jsonObject.put("geonameId", locality.geoName.geonameId)
            jsonObject.put("geoname", JSONObject()
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
            jsonObject.put("locality_id", locality.id)
        }


        jsonObject.put("determination",
            JSONObject()
                .put("confidence", mushroom.second.databaseName)
                .put("taxon_id", mushroom.first.id)
                .put("user_id", userID)
                .put("notes", _determinationNotes ?: ""))

        return Result.Success(Pair(jsonObject, images.value?.toList()))
    }


    override fun onCleared() {
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
        super.onCleared()
    }
}