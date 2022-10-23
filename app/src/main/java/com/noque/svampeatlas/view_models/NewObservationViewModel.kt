package com.noque.svampeatlas.view_models

import android.app.Application
import android.content.res.Resources
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.google.maps.android.SphericalUtil
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.getExifLocation
import com.noque.svampeatlas.fragments.AddObservationFragment
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RecognitionService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import java.util.*

fun <T> initialObserveMutableLiveData(observer: Observer<T>): MutableLiveData<T> {
    val liveData = MutableLiveData<T>()
    liveData.observeForever(observer)
    return liveData
}

class NewObservationViewModel(application: Application, val context: AddObservationFragment.Context, val id: Long, mushroomId: Int, imageFilePath: String?) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "NewObservationViewModel"
    }

    sealed class Notification(val title: String, val message: String, val action: Pair<String, String>? = null) {
        class LocationInaccessible(resources: Resources, error: AppError): Notification(resources.getString(R.string.newObservationError_noCoordinates_title), error.message)
        class LocalityInaccessible(resources: Resources): Notification(resources.getString(R.string.newObservationError_noLocality_title), resources.getString(R.string.newObservationError_noLocality_message))
        class ObservationUploaded(resources: Resources, id: Int): Notification(resources.getString(R.string.addObservationVC_successfullUpload_title),
            "ID: $id")
        class ObservationUpdated(resources: Resources): Notification("", "")
        class NoteSaved(resources: Resources): Notification(resources.getString(R.string.message_noteSaved),resources.getString(R.string.message_noteSaved_message))
        class Deleted(resource: Resources): Notification("", "")
        class NewObservationError(val error: UserObservation.Error, resources: Resources): Notification(resources.getString(error.title), resources.getString(error.message))
        class Error(error: AppError): Notification(error.title, error.message)
        class UseImageMetadata(resources: Resources, val imageLocation: Location): Notification(resources.getString(R.string.addObservationVC_useImageMetadata_title), resources. getString(R.string.addObservationVC_useImageMetadata_message, imageLocation.accuracy.toString()), Pair(resources.getString(R.string.addObservationVC_useImageMetadata_positive), resources.getString(R.string.addObservationVC_useImageMetadata_negative)))
    }

    sealed class Prompt(val title: String, val message: String, val yes: String, val no: String) {
        class LowAccuraccy(resources: Resources): Prompt(resources.getString(R.string.error_addObservationError_lowAccuracy), resources.getString(R.string.newObservationError_tooInaccurate), resources.getString(R.string.action_findLocation), resources.getString(R.string.action_adjustSelf))
    }

    // If null during session - then we do not want to find predictions
    private var recognitionService: RecognitionService? = null
    private var isAwaitingCoordinatedBeforeSave = false

    val observationDate: LiveData<Date> get() = userObservation.observationDate
    val substrate: LiveData<Pair<Substrate, Boolean>?> get() = userObservation.substrate
    val vegetationType: LiveData<Pair<VegetationType, Boolean>?> get() = userObservation.vegetationType
    val hosts: LiveData<Pair<List<Host>, Boolean>?> get() = userObservation.hosts
    val locality: LiveData<Pair<Locality, Boolean>?> get() = userObservation.locality
    val notes: LiveData<String?> get() = userObservation.notes
    val ecologyNotes: LiveData<String?> get() = userObservation.ecologyNotes
    val images: LiveData<List<UserObservation.Image>> get() = userObservation.images
    val mushroom: LiveData<Pair<Mushroom, DeterminationConfidence>?> get() = userObservation.mushroom

    private val _user by lazy { MutableLiveData<User>() }
    private val _isLoading by lazy { MutableLiveData(false) }
    private val _coordinateState: MutableLiveData<State<Pair<Location, Boolean>>> by lazy { initialObserveMutableLiveData(Observer {
        // Whenever coordinate state is set, we have to update user observation too.
        userObservation.location.value = it.item
    }) }

    private val _localitiesState by lazy {MutableLiveData<State<List<Locality>>>() }
    private val _predictionResultsState by lazy { MutableLiveData<State<List<Prediction>>>(State.Empty()) }

    private val userObservation = ListenableUserObservation {
        resetEvent.call()

            if (it.location != null) {
                it.location?.let { locationPair -> _coordinateState.value = State.Items(locationPair)}
            } else {
                _coordinateState.value = State.Empty()
            }

            if (it.locality != null) {
                _localitiesState.value = State.Items(listOfNotNull(it.locality?.first))
            } else {
                _localitiesState.value = State.Empty()
                it.location?.first?.let { getLocalities(it) }
            }
            recognitionService?.reset()
            _predictionResultsState.value = State.Empty()

        viewModelScope.launch {
            if (it.images.isNotEmpty() && context == AddObservationFragment.Context.UploadNote) {
                it.images.forEach { when (it) {
                    is UserObservation.Image.LocallyStored -> {recognitionService?.addPhotoToRequest(it.file) }
                    is UserObservation.Image.New ->  {}
                    else -> {}
                } }
            }
        }
        }

    val isLoading: LiveData<Boolean> = _isLoading
    val coordinateState: LiveData<State<Pair<Location, Boolean>>> get() = _coordinateState
    val localitiesState: LiveData<State<List<Locality>>> get() = _localitiesState
    val predictionResultsState: LiveData<State<List<Prediction>>> get() = _predictionResultsState
    val user: LiveData<User> get() = _user

    val showNotification by lazy { SingleLiveEvent<Notification>() }
    val showPrompt by lazy { SingleLiveEvent<Prompt>() }
    val resetEvent by lazy { SingleLiveEvent<Void>() }

    init {
        viewModelScope.launch {
            RoomService.users.getUser().onSuccess {
                _user.value = it
            }
        }

        when (context) {
            AddObservationFragment.Context.New -> {
                recognitionService = RecognitionService()
                userObservation.set(UserObservation())
            }
            AddObservationFragment.Context.Note -> {
                userObservation.set(UserObservation())
            }
            AddObservationFragment.Context.FromRecognition -> {
                userObservation.set(UserObservation())
                if (mushroomId != 0) {
                    setMushroom(mushroomId)
                }
                imageFilePath?.let { appendImage(File(imageFilePath)) }
            }
            AddObservationFragment.Context.Edit -> {
               editObservation(id)
            }
            AddObservationFragment.Context.UploadNote -> {
                recognitionService = RecognitionService()
                editNote(id)
            }
            AddObservationFragment.Context.EditNote -> {
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
                setDeterminationNotes(Prediction.getNotes(predictionResult, predictionResults))
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
        userObservation.locality.value = Pair(locality, false)
    }

    fun setLocalityLock(isLocked: Boolean) {
        userObservation.locality.value?.first?.let {  userObservation.locality.value = Pair(it, isLocked) }
    }

    fun setLocationLock(isLocked:Boolean) {
        userObservation.location.value?.let { _coordinateState.value = State.Items(Pair(it.first, isLocked)) }
    }

    // This functions is called by the location manager, when the state changes
    fun setCoordinateState(state: State<Location>) {
        fun setLocation(location: Location) {
            _coordinateState.value = State.Items(Pair(location, false))
            if (isAwaitingCoordinatedBeforeSave) {
               saveAsNote()
            } else {
                getLocalities(location)
            }
        }

        when (state) {
            is State.Error -> {
                if (isAwaitingCoordinatedBeforeSave) {
                    isAwaitingCoordinatedBeforeSave = false
                    _isLoading.postValue(false)
                }
                _coordinateState.postValue(State.Empty())
                showNotification.postValue(Notification.LocationInaccessible(getApplication<MyApplication>().resources, state.error))
            }
            is State.Loading -> _coordinateState.postValue(State.Loading())
            is State.Items -> {
                when (val image = userObservation.images.value?.firstOrNull()) {
                    is UserObservation.Image.New -> {
                        val imageLocation = image.file.getExifLocation()
                        if (imageLocation != null && SphericalUtil.computeDistanceBetween(imageLocation.latLng, state.items.latLng) > imageLocation.accuracy) {
                            showNotification.postValue(Notification.UseImageMetadata(getApplication<MyApplication>().resources, imageLocation))
                        } else {
                            setLocation(state.items)
                            if (state.item != null) userObservation.observationDate.value = state.item.date
                        }
                    }
                    else -> setLocation(state.items)
                }
            }
            else -> {}
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
        viewModelScope.launch {
            recognitionService?.addPhotoToRequest(imageFile)
        }

        userObservation.images.value = ((userObservation.images.value ?: listOf()) + listOf(UserObservation.Image.New(imageFile)))
        imageFile.getExifLocation()?.let { imageLocation ->
            _coordinateState.value?.item?.first?.let { coordinateLocation ->
                if (SphericalUtil.computeDistanceBetween(imageLocation.latLng, coordinateLocation.latLng) > imageLocation.accuracy) {
                   showNotification.postValue(Notification.UseImageMetadata(getApplication<MyApplication>().resources, imageLocation))
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
            recognitionService?.reset()
            viewModelScope.launch {
                images.value?.forEach {
                    when (it) {
                        is UserObservation.Image.Hosted -> {}
                        is UserObservation.Image.LocallyStored -> recognitionService?.addPhotoToRequest(it.file)
                        is UserObservation.Image.New -> recognitionService?.addPhotoToRequest(it.file)
                    }
                }
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
                         /*   showNotification.postValue((Notification.ImageDeletionError(getApplication<MyApplication>().resources, it)))*/
                        }
                        it.onSuccess {
                            userObservation.images.postValue(userObservation.images.value?.toMutableList()?.minusElement(image))
                            handleChange()
                        }
                    }
                }
                else -> {}
            }
        }
    }

    fun promptPositive() {
        when (val prompt = showNotification.value) {
            is Notification.UseImageMetadata -> {
                userObservation.observationDate.value = prompt.imageLocation.date
                _coordinateState.value = State.Items(Pair(prompt.imageLocation, false))
                getLocalities(prompt.imageLocation)
            }
            else -> {}
        }
    }

    fun promptNegative() {
        when (val prompt = showNotification.value) {
            is Notification.UseImageMetadata -> {
                when (_coordinateState.value) {
                    is State.Empty, is State.Error<*>, is State.Loading  -> {
                       /* _coordinateState.value = State.Items(Pair(prompt.userLocation, false))
                        getLocalities(prompt.)*/
                    }
                    is State.Items -> { /* Do nothing */ }
                    else -> {}
                }
            }
            else -> {}
        }
    }

    fun setNotes(notes: String?) {
        userObservation.notes.value = notes
    }

    fun setEcologyNotes(ecologyNotes: String?) {
        userObservation.ecologyNotes.value = ecologyNotes
    }

    fun resetLocation() {
        userObservation.locality.value = null
        userObservation.location.value = null
        _localitiesState.value = State.Empty()
        _coordinateState.value = State.Empty()
    }

    private fun getLocalities(location: Location) {
        // If we are not in right context, we do not want to find locality. But we want to clear saved locality.
        if (context == AddObservationFragment.Context.Note || context == AddObservationFragment.Context.EditNote) {
            _localitiesState.value = State.Empty()
            userObservation.locality.value = null
            return
        }
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

                        userObservation.locality.postValue(Pair(locality, false))
                    }
                    result.onError {
                        _localitiesState.value = State.Error(it)
                        if (context != AddObservationFragment.Context.Note)
                        showNotification.postValue(Notification.LocalityInaccessible(MyApplication.applicationContext.resources))
                    }
                }
        }
    }

    var getPredictionsJob: Job? = null

    fun getPredictions() {
        getPredictionsJob?.cancel(null)
        getPredictionsJob = viewModelScope.launch {
            val substrate = substrate.value?.first
            val vegetationType = vegetationType.value?.first
            try {
                if (substrate != null && vegetationType != null) {
                    recognitionService?.addMetadataToRequest(vegetationType, substrate, observationDate.value ?: Date())
                }

                _predictionResultsState.postValue(State.Loading())
                val predictionResults = mutableListOf<Prediction>()
                val result = recognitionService?.getResults()
                if (result != null) {
                    for (index in result.taxonIds.indices) {
                        DataService.getInstance(MyApplication.applicationContext).mushroomsRepository.getMushroom(result.taxonIds[index]).onSuccess {
                            predictionResults.add(Prediction(it,result.conf[index]))
                        }
                    }
                    _predictionResultsState.postValue(State.Items(predictionResults))
                } else {
                    _predictionResultsState.postValue(State.Empty())
                }
            } catch (error: Exception) {
                _predictionResultsState.postValue(State.Empty())
            }
        }
    }

    fun uploadNew(): Boolean {
        val error = userObservation.userObservation.isValid()
        if (error != null) {
            showNotification.postValue(
                Notification.NewObservationError(
                    error,
                    MyApplication.resources
                )
            )
            return false
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
            return true
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
        /*if (coordinateState.value !is State.Items) {
            isAwaitingCoordinatedBeforeSave = true
            return
        } else {
            isAwaitingCoordinatedBeforeSave = false
        }*/

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
        when (context) {
            AddObservationFragment.Context.New, AddObservationFragment.Context.FromRecognition -> userObservation.set(UserObservation())
            AddObservationFragment.Context.Edit -> {
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
            AddObservationFragment.Context.Note -> userObservation.set(UserObservation())
            AddObservationFragment.Context.EditNote, AddObservationFragment.Context.UploadNote -> {
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