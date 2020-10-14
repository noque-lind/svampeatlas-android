package com.noque.svampeatlas.view_models

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.toSimpleString
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import java.text.SimpleDateFormat


class NewObservationViewModel(application: Application) : AndroidViewModel(application) {

    sealed class Error(title: String, message: String, recoveryAction: RecoveryAction? = null): AppError(title, message, recoveryAction) {
        class NoMushroomError(context: Context): Error(context.getString(R.string.error_newObservation_missingInformation_title), context.getString(R.string.error_newObservation_noMushroom_message))
        class NoSubstrateError(context: Context): Error(context.getString(R.string.error_newObservation_missingInformation_title), context.getString(R.string.error_newObservation_noSubstrategroup_message))
        class NoVegetationTypeError(context: Context): Error(context.getString(R.string.error_newObservation_missingInformation_title), context.getString(R.string.error_newObservation_noVegetationType_message))
        class NoLocationDataError(context: Context): Error(context.getString(R.string.error_newObservation_noCoordinates_title), context.getString(R.string.error_newObservation_noCoordinates_message))
        class LocalityFetchError(context: Context): Error(context.getString(R.string.error_newObservation_noLocality_title), context.getString(R.string.error_newObservation_noLocality_message), RecoveryAction.TRYAGAIN)
    }

    companion object {
        private const val TAG = "NewObservationViewModel"
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
    private val _substrate by lazy {MutableLiveData<Pair<Substrate, Boolean>?>()}
    private val _vegetationType by lazy {MutableLiveData<Pair<VegetationType, Boolean>?>()}
    private val _hosts by lazy { MutableLiveData<Pair<MutableList<Host>, Boolean>?>()}
    private val _images by lazy {MutableLiveData<MutableList<File>?>()}
    private val _mushroom by lazy {MutableLiveData<Pair<Mushroom, DeterminationConfidence>?>()}

    private var _notes: String? = null
    private var _ecologyNotes: String? = null
    private var _determinationNotes: String? = null

    private val _coordinateState by lazy { MutableLiveData<State<com.noque.svampeatlas.models.Location>>() }
    private val _localityState by lazy { MutableLiveData<State<List<Locality>>>() }
    private val _predictionResultsState by lazy { MutableLiveData<State<List<PredictionResult>>>() }
    private val _useImageLocationPromptState by lazy { MutableLiveData<State<Pair<com.noque.svampeatlas.models.Location, com.noque.svampeatlas.models.Location?>>>() }

    val date: LiveData<Date> get() = _date
    val substrate: LiveData<Pair<Substrate, Boolean>?> get() = _substrate
    val vegetationType: LiveData<Pair<VegetationType, Boolean>?> get() = _vegetationType
    val hosts: LiveData<Pair<MutableList<Host>, Boolean>?> get() = _hosts
    val locality: LiveData<Locality?> get() = _locality
    val notes get() = _notes
    val ecologyNotes get() = _ecologyNotes
    val images: LiveData<MutableList<File>?> get() = _images
    val mushroom: LiveData<Pair<Mushroom, DeterminationConfidence>?> get() = _mushroom

    val coordinateState: LiveData<State<com.noque.svampeatlas.models.Location>> get() = _coordinateState
    val localityState: LiveData<State<List<Locality>>> get() = _localityState
    val predictionResultsState: LiveData<State<List<PredictionResult>>> get() = _predictionResultsState
    val useImageLocationPromptState: LiveData<State<Pair<com.noque.svampeatlas.models.Location, com.noque.svampeatlas.models.Location?>>> get() = _useImageLocationPromptState


    init {
        setup()
    }

    private fun setup() {
        _date.value = Calendar.getInstance().time
        _substrate.value = null
        _vegetationType.value = null
        _hosts.value = null
        _locality.value = null
        _notes = null
        _ecologyNotes = null
        _images.value = null
        _mushroom.value = null
        _determinationNotes = null

        _coordinateState.value = State.Empty()
        _localityState.value = State.Empty()
        _predictionResultsState.value = State.Empty()
        _useImageLocationPromptState.value = State.Empty()

        viewModelScope.launch {
        SharedPreferences.getSubstrateID()?.let {
                RoomService.getInstance(getApplication()).getSubstrateWithID(it).onSuccess {
                    _substrate.value = Pair(it, true)
                }
            }

            SharedPreferences.getVegetationTypeID()?.let {
                RoomService.getInstance(getApplication()).getVegetationTypeWithID(it).onSuccess {
                    _vegetationType.value = Pair(it, true)
                }
            }

            SharedPreferences.getHosts()?.let {
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

    fun setCoordinateState(state: State<com.noque.svampeatlas.models.Location>) {
        when (state) {
            is State.Loading -> {
                _coordinateState.value = state
            }

            is State.Error -> {
                _localityState.value = State.Error(state.error)
            }

            is State.Items -> {
                if (_useImageLocationPromptState.value == null || _useImageLocationPromptState.value is State.Empty) {
                    val imageLocation = returnImageLocationIfNecessary(state.items)
                    if (imageLocation != null) {
                        _useImageLocationPromptState.value = State.Items(Pair(imageLocation, state.items))
                    } else {
                        _date.value = state.items.date
                        _coordinateState.value = state
                        getLocalities(state.items.latLng)
                    }
                } else {
                    _date.value = state.items.date
                    _coordinateState.value = state
                    getLocalities(state.items.latLng)
                    _useImageLocationPromptState.value = State.Empty()
                }
            }
        }

    }

    fun setSubstrate(substrate: Substrate, isLocked: Boolean) {
        _substrate.value = Pair(substrate, isLocked)

        SharedPreferences.saveSubstrateID(if (isLocked) substrate.id else null)
        if (isLocked) {
            viewModelScope.launch {
                RoomService.getInstance(getApplication()).saveSubstrate(substrate)
            }
        }
    }

    fun setVegetationType(vegetationType: VegetationType, isLocked: Boolean) {
        _vegetationType.value = Pair(vegetationType, isLocked)

        SharedPreferences.saveVegetationTypeID(if (isLocked) vegetationType.id else null)

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

        SharedPreferences.saveHostsID(hosts.map { it.id })

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

        SharedPreferences.saveHostsID(hosts.value?.first?.map { it.id })
    }

    fun appendImage(imageFile: File) {
        var images = images.value

        if (images != null && images.isNotEmpty()) {
            images.add(imageFile)
        } else {
            images = mutableListOf(imageFile)
            if (mushroom.value?.first == null) getPredictions(imageFile)
            returnImageLocationIfNecessary(imageFile)?.let {
                _useImageLocationPromptState.value = State.Items(Pair(it, null))
            }
        }

        _images.value = images
    }

    private fun returnImageLocationIfNecessary(imageFile: File): com.noque.svampeatlas.models.Location? {

        val exif = ExifInterface(imageFile.inputStream())
        val lat = exif.latLong?.first()
        val lng = exif.latLong?.last()
//            val accuracy =  exif.getAttribute(ExifInterface.TAG_GPS_DOP)
        val date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(it)
        }?: kotlin.run {
            Date()
        }

        when (val state = _coordinateState.value) {
            is State.Items -> {
                val location = state.items
                return if (lat != null && lng != null) {
                    if (SphericalUtil.computeDistanceBetween(location.latLng, LatLng(lat, lng)) > 500) Location(date, LatLng(lat, lng), -1F) else null
                } else {
                    null
                }
            }

            is State.Error -> {
                return if (lat != null && lng != null) {
                    Location(date, LatLng(lat, lng), -1F)
                } else {
                    null
                }
            }

            else -> { return null}
        }
    }

    private fun returnImageLocationIfNecessary(location: com.noque.svampeatlas.models.Location): com.noque.svampeatlas.models.Location? {
        val image = _images.value?.first()

        return if (image != null) {
            val exif = ExifInterface(image.inputStream())
            val lat = exif.latLong?.first()
            val lng = exif.latLong?.last()
//            val accuracy =  exif.getAttribute(ExifInterface.TAG_GPS_DOP)
            val date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(it)
            }?: kotlin.run {
                Date()
            }

            return if (lat != null && lng != null) {
                val latLng = LatLng(lat, lng)
                return if (SphericalUtil.computeDistanceBetween(latLng, location.latLng) > 500) Location(date, latLng, -1F) else null
            } else {
                null
            }
        } else {
             null
        }
    }

    fun removeImageAt(position: Int) {
        if (_images.value != null && _images.value!!.lastIndex >= position) {
            _images.value?.removeAt(position)?.delete()

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

    fun resetLocationData() {
        _localityState.value = State.Empty()
        _coordinateState.value = State.Empty()
        _locality.value = null
    }

    fun reset() {
        _date.value = null
        _locality.value = null
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
        val location = (_coordinateState.value as? State.Items)?.items
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

        if (location == null) {
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
        jsonObject.put("decimalLatitude", location.latLng.latitude)
        jsonObject.put("decimalLongitude", location.latLng.longitude)
        jsonObject.put("accuracy", location.accuracy)

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