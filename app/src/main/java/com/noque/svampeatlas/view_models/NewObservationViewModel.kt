package com.noque.svampeatlas.view_models

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.*
import com.google.maps.android.SphericalUtil
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.*
import com.noque.svampeatlas.extensions.Date
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.SingleLiveEvent
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*


class NewObservationViewModel(application: Application) : AndroidViewModel(application) {

    sealed class Notification(val title: String, val message: String) {
        class LocationFound(resources: Resources, locality: Locality, location: Location): Notification(resources.getString(R.string.prompt_localityDetermined_title), resources.getString(R.string.prompt_localityDetermined_message, locality.name, location.latLng.latitude, location.latLng.longitude, location.accuracy))
        class LocationInaccessible(resources: Resources, error: AppError): Notification(resources.getString(R.string.prompt_localityDeterminedError_title), error.message)
        class LocalityInaccessible(resources: Resources): Notification(resources.getString(R.string.error_newObservation_noLocality_title), resources.getString(R.string.error_newObservation_noLocality_message))
        class ObservationUploaded(resources: Resources, id: Int): Notification(resources.getString(R.string.prompt_successRecordCreation_title),
            "ID: $id")
        class ImageDeletionError(resources: Resources, error: AppError): Notification(resources.getString(R.string.prompt_imagedeletion_error_title), error.message)
    }

    sealed class Prompt(val title: String, val message: String, val yes: String, val no: String) {
        class UseImageMetadata(resources: Resources, val imageLocation: Location, val userLocation: Location): Prompt(resources.getString(R.string.prompt_useImageMetadata_title), resources. getString(R.string.prompt_useImageMetadata_message, imageLocation.accuracy), resources.getString(R.string.prompt_useImageMetadata_positive), resources.getString(R.string.prompt_useImageMetadata_negative))
    }

    sealed class Error(title: String, message: String, recoveryAction: RecoveryAction? = null): AppError(title, message, recoveryAction) {
        class NoMushroomError(context: Context): Error(context.getString(R.string.error_newObservation_missingInformation_title), context.getString(R.string.error_newObservation_noMushroom_message))
        class NoSubstrateError(context: Context): Error(context.getString(R.string.error_newObservation_missingInformation_title), context.getString(R.string.error_newObservation_noSubstrategroup_message))
        class NoVegetationTypeError(context: Context): Error(context.getString(R.string.error_newObservation_missingInformation_title), context.getString(R.string.error_newObservation_noVegetationType_message))
        class NoLocationDataError(context: Context): Error(context.getString(R.string.error_newObservation_noCoordinates_title), context.getString(R.string.error_newObservation_noCoordinates_message))
        class LocalityFetchError(context: Context): Error(context.getString(R.string.error_newObservation_noLocality_title), context.getString(R.string.error_newObservation_noLocality_message), RecoveryAction.TRYAGAIN)
    }

    sealed class Image {
        class New(val file: File): Image()
        class Hosted(val id: Int, val url: String, val creationDate: Date?, val userIsValidator: Boolean): Image() {
            val isDeletable: Boolean get() {
                return when {
                    userIsValidator -> true
                    (creationDate != null && creationDate.difDays() <= 7) -> true
                    else -> false
                }
            }
        }
    }

    companion object {
        private const val TAG = "NewObservationViewModel"
    }

    private val _date by lazy {MutableLiveData<Date>(Calendar.getInstance().time)}
    private val _locality by lazy {MutableLiveData<Locality?>(null)}
    private val _substrate by lazy {MutableLiveData<Pair<Substrate, Boolean>?>(null)}
    private val _vegetationType by lazy {MutableLiveData<Pair<VegetationType, Boolean>?>(null)}
    private val _hosts by lazy { MutableLiveData<Pair<MutableList<Host>, Boolean>?>(null)}
    private val _images by lazy { MutableLiveData<MutableList<Image>>(mutableListOf())}
    private val _mushroom by lazy {MutableLiveData<Pair<Mushroom, DeterminationConfidence>?>(null)}
    private val _notes by lazy { MutableLiveData<String?>(null) }
    private val _ecologyNotes by lazy { MutableLiveData<String?>(null) }
    private var _determinationNotes: String? = null

    private val _coordinateState by lazy { MutableLiveData<State<Location>>(State.Empty()) }
    private val _localitiesState by lazy { MutableLiveData<State<List<Locality>>>(State.Empty()) }
    private val _predictionResultsState by lazy { MutableLiveData<State<List<PredictionResult>>>(State.Empty()) }

    val date: LiveData<Date> get() = _date
    val substrate: LiveData<Pair<Substrate, Boolean>?> get() = _substrate
    val vegetationType: LiveData<Pair<VegetationType, Boolean>?> get() = _vegetationType
    val hosts: LiveData<Pair<MutableList<Host>, Boolean>?> get() = _hosts
    val locality: LiveData<Locality?> get() = _locality
    val notes: LiveData<String?> get() = _notes
    val ecologyNotes: LiveData<String?> get() = _ecologyNotes
    val images: LiveData<MutableList<Image>> get() = _images
    val mushroom: LiveData<Pair<Mushroom, DeterminationConfidence>?> get() = _mushroom

    val coordinateState: LiveData<State<Location>> get() = _coordinateState
    val localitiesState: LiveData<State<List<Locality>>> get() = _localitiesState
    val predictionResultsState: LiveData<State<List<PredictionResult>>> get() = _predictionResultsState

    val removedImage by lazy { SingleLiveEvent<Int>() }

    val showNotification by lazy { SingleLiveEvent<Notification>() }
    val showPrompt by lazy { SingleLiveEvent<Prompt>() }

    fun editObservation(observation: Observation, user: User) {
        _date.value = observation.observationDate
        _mushroom.value = Pair(Mushroom(observation.id, observation.determination.fullName, VernacularNameDK(observation.determination.localizedName, null)), observation.determination.confidence ?: DeterminationConfidence.CONFIDENT)
        observation.substrate?.let { _substrate.value = Pair(it, false) }
        observation.vegetationType?.let { _vegetationType.value = Pair(it, false) }
        _hosts.value = Pair(observation.hosts.toMutableList(), false)
        _locality.value = observation.locality
        _notes.value = observation.note
        _ecologyNotes.value = observation.ecologyNote
        _images.value = observation.images.map {
            Image.Hosted(it.id, it.url, Date(it.createdAt), user.isValidator)
        }.toMutableList()
        _determinationNotes = null
        observation.location?.let { _coordinateState.value = State.Items(it) }
        observation.locality?.let { _localitiesState.value = State.Items(listOf(it)) }
        _predictionResultsState.value = State.Empty()
    }

    fun setDate(date: Date) {
        _date.value = date
    }

    fun setLocality(locality: Locality) {
        _locality.value = locality
    }

    fun setCoordinateState(state: State<Location>) {
        fun setLocation(location: Location) {
            _date.value = location.date
            _coordinateState.value = state
            getLocalities(location)
        }

        fun promptToUseImageLocation(imageLocation: Location, userLocation: Location) {
            showPrompt.postValue(Prompt.UseImageMetadata(getApplication<MyApplication>().resources, imageLocation, userLocation))
        }

        when (state) {
            is State.Error -> {
                _coordinateState.postValue(state)
                showNotification.postValue(Notification.LocationInaccessible(getApplication<MyApplication>().resources, state.error))
            }
            is State.Loading -> _coordinateState.postValue(state)
            is State.Items -> {
                when (val image = images.value?.firstOrNull()) {
                    is Image.New -> {
                        val imageLocation = image.file.getExifLocation()
                        if (imageLocation != null && SphericalUtil.computeDistanceBetween(imageLocation.latLng, state.items.latLng) > imageLocation.accuracy) {
                            promptToUseImageLocation(imageLocation, state.items)
                        } else {
                            setLocation(state.items)
                        }
                    }
                    else -> setLocation(state.items)
                }
            }
        }
    }

    fun setSubstrate(substrate: Substrate, isLocked: Boolean) {
        _substrate.value = Pair(substrate, isLocked)

        SharedPreferences.saveSubstrateID(if (isLocked) substrate.id else null)
        if (isLocked) {
            viewModelScope.launch {
                RoomService.saveSubstrate(substrate)
            }
        }
    }

    fun setVegetationType(vegetationType: VegetationType, isLocked: Boolean) {
        _vegetationType.value = Pair(vegetationType, isLocked)

        SharedPreferences.saveVegetationTypeID(if (isLocked) vegetationType.id else null)

        if (isLocked) {
            viewModelScope.launch {
                RoomService.saveVegetationType(vegetationType)
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
                RoomService.saveHosts(hosts)
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
        Log.d(TAG, _images.value?.toString() ?: "")
        if (images.value?.count() == 0 && mushroom.value == null) {
            getPredictions(imageFile)
        }

        _images.value?.add(Image.New(imageFile))
        _images.value = _images.value
        imageFile.getExifLocation()?.let { imageLocation ->
            _coordinateState.value?.item?.let { coordinateLocation ->
                if (SphericalUtil.computeDistanceBetween(imageLocation.latLng, coordinateLocation.latLng) > imageLocation.accuracy) {
                    showPrompt.postValue(Prompt.UseImageMetadata(getApplication<MyApplication>().resources, imageLocation, coordinateLocation))
                }
            }
        }
    }

    fun removeImageAt(position: Int) {
        fun handleChange() {
            removedImage.postValue(position)
            if (images.value?.count() == 0) {
                _predictionResultsState.value = State.Empty()
            }
        }


        _images.value?.getOrNull(position).let {
            when (it) {
                is Image.New -> {
                    it.file.delete()
                    _images.value?.removeAt(position)
                    handleChange()
                }
                is Image.Hosted -> {
                    Session.deleteImage(it.id) {
                        it.onError {
                            _images.postValue(_images.value)
                            showNotification.postValue((Notification.ImageDeletionError(getApplication<MyApplication>().resources, it)))
                        }
                        it.onSuccess {
                            _images.value?.removeAt(position)
                            handleChange()
                        }
                    }
                }
            }
        }
    }

    fun promptPositive() {
        when (val prompt = showPrompt.value) {
            is Prompt.UseImageMetadata -> {
                _date.value = prompt.imageLocation.date
                _coordinateState.value = State.Items(prompt.imageLocation)
                getLocalities(prompt.imageLocation)
            }
            null -> {}
        }
    }

    fun promptNegative() {
        when (val prompt = showPrompt.value) {
            is Prompt.UseImageMetadata -> {
                if (_coordinateState.value?.item?.latLng?.latitude != prompt.userLocation.latLng.latitude && _coordinateState.value?.item?.latLng?.longitude != prompt.userLocation.latLng.longitude) {
                    _coordinateState.value = State.Items(prompt.userLocation)
                    getLocalities(prompt.userLocation)
                }
            }
            null -> {}
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
        _notes.value = notes
    }

    fun setEcologyNotes(ecologyNotes: String?) {
        _ecologyNotes.value = ecologyNotes
    }

    fun resetLocationData() {
        _localitiesState.value = State.Empty()
        _coordinateState.value = State.Empty()
        _locality.value = null
    }

    fun reset() {
        _date.value = Calendar.getInstance().time
        _substrate.value = null
        _vegetationType.value = null
        _hosts.value = null
        _locality.value = null
        _notes.value = null
        _ecologyNotes.value = null
        _mushroom.value = null
        _determinationNotes = null
        _images.value = mutableListOf()

        _coordinateState.value = State.Empty()
        _localitiesState.value = State.Empty()
        _predictionResultsState.value = State.Empty()

        viewModelScope.launch {
            SharedPreferences.getSubstrateID()?.let {
                RoomService.getSubstrateWithID(it).onSuccess {
                    _substrate.value = Pair(it, true)
                }
            }

            SharedPreferences.getVegetationTypeID()?.let {
                RoomService.getVegetationTypeWithID(it).onSuccess {
                    _vegetationType.value = Pair(it, true)
                }
            }

            SharedPreferences.getHosts()?.let {
                RoomService.getHostsWithIds(it).onSuccess {
                    _hosts.value = Pair(it.toMutableList(), true)
                }
            }
        }
    }


    private fun getLocalities(location: Location) {
        _localitiesState.value = State.Loading()
        viewModelScope.launch {
            DataService.getInstance(getApplication())
                .getLocalities(TAG, location.latLng) { result ->
                    result.onSuccess {
                        _localitiesState.postValue(State.Items(it))
                        val locality = it.maxWith(
                            kotlin.Comparator { a, b ->
                                SphericalUtil.computeDistanceBetween(
                                    a.location,
                                    b.location
                                ).toInt()
                            }
                        )
                        if (locality != null) {
                            _locality.postValue(locality)
                            showNotification.postValue(
                                Notification.LocationFound(
                                    MyApplication.applicationContext.resources,
                                    locality,
                                    location
                                )
                            )
                        } else {
                            _locality.postValue(null)
                            showNotification.postValue(
                                Notification.LocalityInaccessible(
                                    MyApplication.applicationContext.resources
                                )
                            )
                        }
                    }
                    result.onError {
                        _localitiesState.value = State.Error(it)
                        showNotification.postValue(Notification.LocalityInaccessible(MyApplication.applicationContext.resources))
                    }
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

    fun prepareForUpload(userID: Int, isEdit: Boolean): Result<Pair<JSONObject, List<File>?>, Error> {
        val date = date.value
        val mushroom = mushroom.value
        val substrate = substrate.value?.first
        val vegetationType = vegetationType.value?.first
        val location = (_coordinateState.value as? State.Items)?.items
        val locality = locality.value
        val hosts = hosts.value?.first

        if (vegetationType == null) return Result.Error(Error.NoVegetationTypeError(getApplication()))
        if (substrate == null) return Result.Error(Error.NoSubstrateError(getApplication()))
        if (location == null) return Result.Error(Error.NoLocationDataError(getApplication()))
        if (locality == null) return Result.Error(Error.NoLocationDataError(getApplication()))

       val jsonObject = JSONObject()
        jsonObject.put("observationDate", (date?.toSimpleString()) ?: Calendar.getInstance().time.toSimpleString())
        jsonObject.put("os", "Android")
        jsonObject.put("browser", "Native App")
        jsonObject.put("substrate_id", substrate.id)
        jsonObject.put("vegetationtype_id", vegetationType.id)

        val hostArray = JSONArray()
        hosts?.forEach { hostArray.put(JSONObject().put("_id", it.id))}

        jsonObject.put("associatedOrganisms", hostArray)
        jsonObject.put("ecologynote", ecologyNotes.value)
        jsonObject.put("note", notes.value)
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

        if (!isEdit) {
            if (mushroom == null) {
                return Result.Error(Error.NoMushroomError(getApplication()))
            }
            if (predictionResultsState.value is State.Items && _determinationNotes == null) {
                val predictionResults = predictionResultsState.value as State.Items
                val selectedPrediction = predictionResults.items.firstOrNull { it.mushroom.id == mushroom.first.id }
                _determinationNotes = PredictionResult.getNotes(selectedPrediction, predictionResults.items)
            }

            jsonObject.put("determination",
                JSONObject()
                    .put("confidence", mushroom.second.databaseName)
                    .put("taxon_id", mushroom.first.id)
                    .put("user_id", userID)
                    .put("notes", _determinationNotes ?: ""))
        }

        return Result.Success(Pair(jsonObject, images.value?.mapNotNull {
            when (it) {
                is Image.New -> it.file
                else -> null
            }
        }))
    }


    override fun onCleared() {
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
        super.onCleared()
    }
}