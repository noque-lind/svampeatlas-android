package com.noque.svampeatlas.view_models

import android.app.Application
import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.*
import com.google.maps.android.SphericalUtil
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.*
import com.noque.svampeatlas.fragments.AddObservationFragment
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class NewObservationViewModel(application: Application, val type: AddObservationFragment.Type, val id: Long, mushroomId: Int, imageFilePath: String?) : AndroidViewModel(application) {

    sealed class Notification(val title: String, val message: String) {
        class LocationFound(resources: Resources, val locality: Locality, location: Location): Notification(resources.getString(R.string.prompt_localityDetermined_title), resources.getString(R.string.prompt_localityDetermined_message, locality.name, location.latLng.latitude, location.latLng.longitude, location.accuracy))
        class LocationInaccessible(resources: Resources, error: AppError): Notification(resources.getString(R.string.prompt_localityDeterminedError_title), error.message)
        class LocalityInaccessible(resources: Resources): Notification(resources.getString(R.string.error_newObservation_noLocality_title), resources.getString(R.string.error_newObservation_noLocality_message))
        class ObservationUploaded(resources: Resources, id: Int): Notification(resources.getString(R.string.prompt_successRecordCreation_title),
            "ID: $id")
        class ObservationUpdated(resources: Resources): Notification("", "")
        class NoteSaved(resources: Resources): Notification(resources.getString(R.string.promp_noteSaved_title),resources.getString(R.string.prompt_noteSaved_message))
        class Deleted(resource: Resources): Notification("", "")
        class ImageDeletionError(resources: Resources, error: AppError): Notification(resources.getString(R.string.prompt_imagedeletion_error_title), error.message)
        class NewObservationError(val error: UserObservation.Error, resources: Resources): Notification(resources.getString(error.title), resources.getString(error.message))
        class Error(error: AppError): Notification(error.title, error.message)
    }

    sealed class Error(title: String, message: String, recoveryAction: RecoveryAction?) :
        AppError(title, message, recoveryAction) {
        class MissingCoordinatesBeforeSave: Error("", "", null)
    }

    sealed class Prompt(val title: String, val message: String, val yes: String, val no: String) {
        class UseImageMetadata(resources: Resources, val imageLocation: Location, val userLocation: Location): Prompt(resources.getString(R.string.prompt_useImageMetadata_title), resources. getString(R.string.prompt_useImageMetadata_message, imageLocation.accuracy), resources.getString(R.string.prompt_useImageMetadata_positive), resources.getString(R.string.prompt_useImageMetadata_negative))
    }

    companion object {
        private const val TAG = "NewObservationViewModel"
    }

    private val _lockedLocality = MutableLiveData<Locality?>()
    private val _lockedLocation = MutableLiveData<Location?>()

    val lockedLocality: LiveData<Locality?> = _lockedLocality
    val lockedLocation: LiveData<Location?> = _lockedLocation

    private var isAwaitingCoordinatedBeforeSave = false



    val observationDate: LiveData<Date>  get() = userObservation.observationDate
    val substrate: LiveData<Pair<Substrate, Boolean>?> get() = userObservation.substrate
    val vegetationType: LiveData<Pair<VegetationType, Boolean>?> get() = userObservation.vegetationType
    val hosts: LiveData<Pair<List<Host>, Boolean>?> get() = userObservation.hosts
    val locality: LiveData<Pair<Locality?, Boolean>?> get() = userObservation.locality
    val notes: LiveData<String?> get() = userObservation.notes
    val ecologyNotes: LiveData<String?> get() = userObservation.ecologyNotes
    val images: LiveData<List<UserObservation.Image>> get() = userObservation.images
    val mushroom: LiveData<Pair<Mushroom, DeterminationConfidence>?> get() = userObservation.mushroom

    private var lastShownLocalityNotificationID: Int? = null

    private val _isLoading by lazy { MutableLiveData<Boolean>(false) }
    private val _coordinateState by lazy { MutableLiveData<State<Location>>() }
    private val _localitiesState by lazy {MutableLiveData<State<List<Locality>>>() }
    private val _predictionResultsState by lazy { MutableLiveData<State<List<PredictionResult>>>(State.Empty()) }

    private var userObservation = ListenableUserObservation {
        if (it.locality == null && lockedLocality.value != null) {
            it.locality = Pair(lockedLocality.value, true)
        }

        if (it.location == null && lockedLocation.value != null) {
            it.location = Pair(lockedLocation.value, true)
        }

        if (it.locality != null) {
            _localitiesState.value = State.Items(listOfNotNull(it.locality?.first))
        } else {
            _localitiesState.value = State.Empty()
            it.location?.first?.let { getLocalities(it) }
        }

        if (it.location != null) {
            it.location?.first?.let { _coordinateState.value = State.Items(it) }
        } else {
            _coordinateState.value = State.Empty()
        }

        _predictionResultsState.value = State.Empty()
    }

    val isLoading: LiveData<Boolean> = _isLoading
    val coordinateState: LiveData<State<Location>> get() = _coordinateState
    val localitiesState: LiveData<State<List<Locality>>> get() = _localitiesState
    val predictionResultsState: LiveData<State<List<PredictionResult>>> get() = _predictionResultsState

    val showNotification by lazy { SingleLiveEvent<Notification>() }
    val showPrompt by lazy { SingleLiveEvent<Prompt>() }


    init {
        when (type) {
            AddObservationFragment.Type.New, AddObservationFragment.Type.Note -> {
                userObservation.set(UserObservation())
            }
            AddObservationFragment.Type.FromRecognition -> {
                userObservation.set(UserObservation())
                if (mushroomId != 0) {
                    setMushroom(mushroomId)
                }
                imageFilePath?.let { appendImage(File(imageFilePath)) }
            }
            AddObservationFragment.Type.Edit -> {
               editObservation(id)
            }
            AddObservationFragment.Type.EditNote -> {
               editNote(id)
            }
        }
    }

    private fun editObservation(id: Long) {
        _isLoading.value = true
        if (id != 0L) {
            DataService.getInstance(getApplication()).getObservation(TAG, id.toInt()) {
                it.onSuccess { observation ->
                    userObservation.set(UserObservation(observation))
                    _isLoading.postValue(false)
                }
                it.onError {
                    showNotification.postValue(Notification.Error(it))
                    _isLoading.postValue(false)
                }
            }
        }
    }

    private fun editNote(id: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            RoomService.notesDao.getById(id).apply {
                onSuccess {
                    userObservation.set(UserObservation(newObservation = it))
                    _isLoading.postValue(false)
                }
            }
        }
    }

    fun setMushroom(taxonID: Int?) {
        if (taxonID != null && taxonID != 0) {
            val predictionResults = predictionResultsState.value?.item
            val predictionResult = predictionResults?.find { it.mushroom.id == taxonID }
            if (predictionResult != null && !predictionResults.isNullOrEmpty()) {
                setDeterminationNotes(PredictionResult.getNotes(predictionResult, predictionResults))
            } else if (!predictionResults.isNullOrEmpty()) {
                setDeterminationNotes(null)
            }

            viewModelScope.launch(Dispatchers.IO) {
                DataService.getInstance(getApplication()).mushroomsRepository.getMushroom(taxonID).apply {
                    onSuccess {
                        userObservation.mushroom.postValue(Pair(it, DeterminationConfidence.CONFIDENT))
                    }
                    onError {}
                }
            }
        } else {
            userObservation.mushroom.value = null
            setDeterminationNotes(null)
        }
    }

    fun setDeterminationNotes(notes: String?) {
        userObservation.determinationNotes = notes
    }

    fun setConfidence(confidence: DeterminationConfidence) {
        userObservation.mushroom.value?.let {
            userObservation.mushroom.value = Pair(it.first, confidence)
        }
    }

    fun setObservationDate(date: Date) {
        userObservation.observationDate.value = date
    }

    fun setLocality(locality: Locality) {
        userObservation.locality.value = Pair(locality, _lockedLocality.value != null)

        if (_lockedLocality.value != null) _lockedLocality.value = locality
    }

    fun setLocalityLock(isLocked: Boolean) {
        _lockedLocality.value = if (isLocked) {
            locality.value?.first
        } else {
            null
        }
    }

    fun setLocationLock(isLocked:Boolean) {
        _lockedLocation.value = if (isLocked) {
            coordinateState.value?.item
        } else {
            null
        }
    }

    fun setCoordinateState(state: State<Location>) {
        fun setLocation(location: Location) {
            userObservation.observationDate.value = location.date
            userObservation.location.value = Pair(location, _lockedLocation.value != null)
            _coordinateState.value = state
            if (_lockedLocation.value != null) {
                _lockedLocation.value = location
            }
            if (isAwaitingCoordinatedBeforeSave) {
               saveAsNote()
            } else {
                getLocalities(location)
            }
        }

        fun promptToUseImageLocation(imageLocation: Location, userLocation: Location) {
            showPrompt.postValue(Prompt.UseImageMetadata(getApplication<MyApplication>().resources, imageLocation, userLocation))
        }

        when (state) {
            is State.Error -> {
                if (isAwaitingCoordinatedBeforeSave) {
                    isAwaitingCoordinatedBeforeSave = false
                    _isLoading.postValue(false)
                }
                _coordinateState.postValue(state)
                showNotification.postValue(Notification.LocationInaccessible(getApplication<MyApplication>().resources, state.error))
            }
            is State.Loading -> _coordinateState.postValue(state)
            is State.Items -> {
                when (val image = userObservation.images.value?.firstOrNull()) {
                    is UserObservation.Image.New -> {
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
        userObservation.substrate.value = Pair(substrate, isLocked)
        SharedPreferences.saveSubstrateID(if (isLocked) substrate.id else null)
        if (isLocked) {
            viewModelScope.launch {
                RoomService.substrates.saveSubstrate(substrate)
            }
        }
    }

    fun setVegetationType(vegetationType: VegetationType, isLocked: Boolean) {
        userObservation.vegetationType.value = Pair(vegetationType, isLocked)
        SharedPreferences.saveVegetationTypeID(if (isLocked) vegetationType.id else null)
        if (isLocked) {
            viewModelScope.launch {
                RoomService.vegetationTypes.saveVegetationType(vegetationType)
            }
        }
    }

    fun appendHost(host: Host, isLocked: Boolean) {
        val value = userObservation.hosts.value?.first ?: listOf()
        userObservation.hosts.value = Pair(value + listOf(host), isLocked)

        if (isLocked) {
            SharedPreferences.saveHostsID(userObservation.hosts.value?.first?.map { it.id })
            viewModelScope.launch {
                RoomService.hosts.saveHosts(listOf(host))
            }
        } else {
            SharedPreferences.saveHostsID(null)
        }
    }

    fun setHostsLockedState(value: Boolean) {
        if (userObservation.hosts.value?.second != value)
            userObservation.hosts.value = Pair(userObservation.hosts.value?.first ?: mutableListOf(), value)
    }

    fun removeHost(host: Host, isLocked: Boolean) {
        val value = userObservation.hosts.value?.first
        userObservation.hosts.value = Pair(value?.filter { it.id != host.id } ?: listOf(), isLocked)

        if (isLocked) {
            SharedPreferences.saveHostsID(userObservation.hosts.value?.first?.map { it.id })
        } else {
            SharedPreferences.saveHostsID(null)
        }
    }

    fun appendImage(imageFile: File) {
        if (userObservation.images.value?.count() == 0 && userObservation.mushroom.value == null) {
            getPredictions(imageFile)
        }

        userObservation.images.value = ((userObservation.images.value ?: mutableListOf()) + listOf(UserObservation.Image.New(imageFile))).toMutableList()
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
//            removedImage.postValue(position)
            if (userObservation.images.value?.count() == 0) {
                _predictionResultsState.value = State.Empty()
            }
        }

        userObservation.images.value?.getOrNull(position).let { image ->
            when (image) {
                is UserObservation.Image.New -> {
                    image.file.delete()
                    userObservation.images.value = userObservation.images.value?.toMutableList()?.minusElement(image)
                    handleChange()
                }
                is UserObservation.Image.LocallyStored -> {
                    image.file.delete()
                    userObservation.images.value = userObservation.images.value?.toMutableList()?.minusElement(image)
                    handleChange()
                }
                is UserObservation.Image.Hosted -> {
                    Session.deleteImage(image.id) {
                        it.onError {
                            userObservation.images.postValue(userObservation.images.value)
                            showNotification.postValue((Notification.ImageDeletionError(getApplication<MyApplication>().resources, it)))
                        }
                        it.onSuccess {
                            userObservation.images.postValue(userObservation.images.value?.toMutableList()?.minusElement(image))
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
                userObservation.observationDate.value = prompt.imageLocation.date
                _coordinateState.value = State.Items(prompt.imageLocation)
                getLocalities(prompt.imageLocation)
            }
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

    fun setNotes(notes: String?) {
        userObservation.notes.value = notes
    }

    fun setEcologyNotes(ecologyNotes: String?) {
        userObservation.ecologyNotes.value = ecologyNotes
    }

    fun resetLocationData() {

        val location = coordinateState.value?.item
        if (location != null && locality.value == null) {
            // When location is found but not locality, we assume the user wants to try and find locality
            getLocalities(location)
        } else {
            userObservation.locality.value = null
            _localitiesState.value = State.Empty()
            _coordinateState.value = State.Empty()
        }
    }

    private fun getLocalities(location: Location) {
        _localitiesState.value = State.Loading()
        viewModelScope.launch {
            DataService.getInstance(getApplication())
                .getLocalities(TAG, location.latLng) { result ->
                    result.onSuccess {
                        _localitiesState.postValue(State.Items(it))
                        val locality = it.minBy {
                            SphericalUtil.computeDistanceBetween(
                                location.latLng,
                                it.location
                            ).toInt()
                        }

                            if (locality != null) {
                            userObservation.locality.postValue(Pair(locality, lockedLocality.value != null))
                                if (lockedLocality.value != null) {
                                    _lockedLocality.postValue(locality)
                                }
                            if (type != AddObservationFragment.Type.Note && type != AddObservationFragment.Type.EditNote)
                                if(lastShownLocalityNotificationID != locality.id) {
                                    lastShownLocalityNotificationID = locality.id
                                    showNotification.postValue(
                                        Notification.LocationFound(
                                            MyApplication.applicationContext.resources,
                                            locality,
                                            location
                                        )     )
                                }

                        } else {
                            userObservation.locality.postValue(null)
                            if (type != AddObservationFragment.Type.Note && type != AddObservationFragment.Type.EditNote)
                            showNotification.postValue(
                                Notification.LocalityInaccessible(
                                    MyApplication.applicationContext.resources
                                )
                            )
                        }
                    }
                    result.onError {
                        _localitiesState.value = State.Error(it)
                        if (type != AddObservationFragment.Type.Note)
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

    fun uploadNew() {
        val error = userObservation.userObservation.isValid()
        if (error != null) {
            showNotification.postValue(
                Notification.NewObservationError(
                    error,
                    MyApplication.resources
                )
            )
        } else {
            viewModelScope.launch {
                _isLoading.value = true
                Session.uploadObservation(userObservation.userObservation).apply {
                    onError {
                        showNotification.postValue(Notification.Error(it)) }
                    onSuccess {
                        showNotification.postValue(Notification.ObservationUploaded(MyApplication.resources, it.first))
                    }
                    _isLoading.postValue(false)
                }
            }
        }
                    }

    fun uploadChanges() {
        _isLoading.value = true
        viewModelScope.launch {
            Session.editObservation(id.toInt(), userObservation.userObservation).apply {
                onError {
                    showNotification.postValue(Notification.Error(it)); _isLoading.postValue(false) }
                onSuccess {
                    showNotification.postValue(Notification.ObservationUpdated(MyApplication.resources))
                    _isLoading.postValue(false)
                }
            }
        }
    }


    fun saveAsNote() {
        _isLoading.value = true
        if (coordinateState.value !is State.Items) {
            isAwaitingCoordinatedBeforeSave = true
            return
        } else {
            isAwaitingCoordinatedBeforeSave = false
        }

        viewModelScope.launch(Dispatchers.IO) {
            RoomService.notesDao.save(userObservation.userObservation.asNewObservation()).apply {
                onError {
                    showNotification.postValue(Notification.Error(it.toAppError(MyApplication.resources)))
                }

                onSuccess {
                    showNotification.postValue(Notification.NoteSaved(MyApplication.resources))
                }

                _isLoading.postValue(false)
            }
        }
    }

    override fun onCleared() {
        userObservation.userObservation.deleteTempimages()
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
        super.onCleared()
    }

    fun delete() {
        when (type) {
            AddObservationFragment.Type.New, AddObservationFragment.Type.FromRecognition -> userObservation.set(UserObservation())
            AddObservationFragment.Type.Edit -> {
                viewModelScope.launch {
                    Session.deleteObservation(id.toInt()).apply {
                        onError {
                            showNotification.postValue(Notification.Error(it))
                        }
                        onSuccess {
                            showNotification.postValue(Notification.Deleted(MyApplication.resources))
                        }
                    }
                }
            }
            AddObservationFragment.Type.Note -> userObservation.set(UserObservation())
            AddObservationFragment.Type.EditNote -> {
                viewModelScope.launch {
                    RoomService.notesDao.delete(NewObservation(Date(id), Date(), null, null, null, null, null, null, null, null, null, listOf(), listOf()))
                        .apply {
                        onError {
                            showNotification.postValue(Notification.Error(it.toAppError(MyApplication.resources)))
                        }
                        onSuccess {
                            userObservation.userObservation.deleteAllImages()
                            showNotification.postValue(Notification.Deleted(MyApplication.resources))
                        }
                    }
                }
            }
        }
    }
}