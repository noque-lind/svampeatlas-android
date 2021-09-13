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
import com.noque.svampeatlas.fragments.AddObservationFragment
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.FileManager
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.DispatchGroup
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

class NewObservationViewModel(application: Application, val type: AddObservationFragment.Type, val id: Long, mushroomId: Int, imageFilePath: String?) : AndroidViewModel(application) {

    sealed class Notification(val title: String, val message: String) {
        class LocationFound(resources: Resources, val locality: Locality, location: Location): Notification(resources.getString(R.string.prompt_localityDetermined_title), resources.getString(R.string.prompt_localityDetermined_message, locality.name, location.latLng.latitude, location.latLng.longitude, location.accuracy))
        class LocationInaccessible(resources: Resources, error: AppError): Notification(resources.getString(R.string.prompt_localityDeterminedError_title), error.message)
        class LocalityInaccessible(resources: Resources): Notification(resources.getString(R.string.error_newObservation_noLocality_title), resources.getString(R.string.error_newObservation_noLocality_message))
        class ObservationUploaded(resources: Resources, id: Int): Notification(resources.getString(R.string.prompt_successRecordCreation_title),
            "ID: $id")
        class ImageDeletionError(resources: Resources, error: AppError): Notification(resources.getString(R.string.prompt_imagedeletion_error_title), error.message)
        class NewObservationError(val error: com.noque.svampeatlas.models.NewObservationError, resources: Resources): Notification(resources.getString(error.title), resources.getString(error.message))
        class Error(error: AppError): Notification(error.title, error.message)
    }

    sealed class Error(title: String, message: String, recoveryAction: RecoveryAction?) :
        AppError(title, message, recoveryAction) {
        class MissingCoordinatesBeforeSave: Error("", "", null)
    }

    sealed class Prompt(val title: String, val message: String, val yes: String, val no: String) {
        class UseImageMetadata(resources: Resources, val imageLocation: Location, val userLocation: Location): Prompt(resources.getString(R.string.prompt_useImageMetadata_title), resources. getString(R.string.prompt_useImageMetadata_message, imageLocation.accuracy), resources.getString(R.string.prompt_useImageMetadata_positive), resources.getString(R.string.prompt_useImageMetadata_negative))
    }

    sealed class Image {
        companion object {
            suspend fun saveToNotebookAlbum(images: List<Image>): List<File> {
                val newFiles = mutableListOf<File>()
                images.forEach {
                    (it as? New)?.file?.let {
                        FileManager.saveAsNotesImage(it).onSuccess { newFiles.add(it) }
                        it.delete()
                    }
                }
                return newFiles.toList()
            }

            fun getImagesForUpload(images: List<Image>): List<File> {
                return images.map {
                    return when (it) {
                        is LocallyStored -> listOf(it.file)
                        is New -> listOf(it.file)
                        is Hosted -> emptyList()
                    }
                }
            }
        }

        class LocallyStored(val file: File): Image()
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

    private var isAwaitingCoordinatedBeforeSave = false

    private val _date by lazy { MutableLiveData<Date>() }
    private val _locality by lazy {MutableLiveData<Locality?>()}
    private val _substrate by lazy {MutableLiveData<Pair<Substrate, Boolean>?>()}
    private val _vegetationType by lazy {MutableLiveData<Pair<VegetationType, Boolean>?>()}
    private val _hosts by lazy { MutableLiveData<Pair<List<Host>, Boolean>?>()}
    private val _images by lazy { MutableLiveData<MutableList<Image>>()}
    private val _mushroom by lazy{ MutableLiveData<Pair<Mushroom, DeterminationConfidence>?>()}
    private val _notes by lazy { MutableLiveData<String?>() }
    private val _ecologyNotes by lazy { MutableLiveData<String?>(null) }
    private var _determinationNotes: String? = null

    private val _setupState by lazy {MutableLiveData<State<Void?>>(State.Empty())}
    private val _coordinateState by lazy { MutableLiveData<State<Location>>() }
    private val _localitiesState by lazy {MutableLiveData<State<List<Locality>>>(State.Empty()) }
    private val _predictionResultsState by lazy { MutableLiveData<State<List<PredictionResult>>>(State.Empty()) }
    private val _saveState by lazy { MutableLiveData<State<Void?>>(State.Empty()) }

    val date: LiveData<Date> get() = _date
    val substrate: LiveData<Pair<Substrate, Boolean>?> get() = _substrate
    val vegetationType: LiveData<Pair<VegetationType, Boolean>?> get() = _vegetationType
    val hosts: LiveData<Pair<List<Host>, Boolean>?> get() = _hosts
    val locality: LiveData<Locality?> get() = _locality
    val notes: LiveData<String?> get() = _notes
    val ecologyNotes: LiveData<String?> get() = _ecologyNotes
    val images: LiveData<MutableList<Image>> get() = _images
    val mushroom: LiveData<Pair<Mushroom, DeterminationConfidence>?> get() = _mushroom

    val setupState: LiveData<State<Void?>> get() = _setupState
    val coordinateState: LiveData<State<Location>> get() = _coordinateState
    val localitiesState: LiveData<State<List<Locality>>> get() = _localitiesState
    val predictionResultsState: LiveData<State<List<PredictionResult>>> get() = _predictionResultsState
    val saveState: LiveData<State<Void?>> get() = _saveState

    val removedImage by lazy { SingleLiveEvent<Int>() }

    val showNotification by lazy { SingleLiveEvent<Notification>() }
    val showPrompt by lazy { SingleLiveEvent<Prompt>() }


    init {
        when (type) {
            AddObservationFragment.Type.New, AddObservationFragment.Type.Note -> {
                setupAsNew()
            }
            AddObservationFragment.Type.FromRecognition -> {
                setupAsNew()
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
        _setupState.value = State.Loading()
        if (id != 0L) {
            DataService.getInstance(getApplication()).getObservation(TAG, id.toInt()) {
                it.onSuccess {observation ->
                    _date.value = observation.observationDate
                    _mushroom.value = Pair(Mushroom(observation.id, observation.determination.fullName, VernacularNameDK(observation.determination.localizedName, null)), observation.determination.confidence ?: DeterminationConfidence.CONFIDENT)
                    observation.substrate?.let { _substrate.value = Pair(it, false) }
                    observation.vegetationType?.let { _vegetationType.value = Pair(it, false) }
                    _hosts.value = Pair(observation.hosts.toMutableList(), false)
                    _locality.value = observation.locality
                    _notes.value = observation.note
                    _ecologyNotes.value = observation.ecologyNote
                    _images.value = observation.images.map {
                        Image.Hosted(it.id, it.url, Date(it.createdAt), Session.user.value!!.isValidator)
                    }.toMutableList()
                    _determinationNotes = null
                    observation.location?.let { _coordinateState.value = State.Items(it) }
                    observation.locality?.let { _localitiesState.value = State.Items(listOf(it)) }
                    _predictionResultsState.value = State.Empty()

                    _setupState.value = State.Items(null)
                }
                it.onError {
                    _setupState.value = State.Error(it)
                }
            }
        }
    }

    private fun editNote(id: Long) {
        _setupState.value = State.Loading()

        viewModelScope.launch {
            RoomService.notesDao.getById(id).apply {
                onSuccess {newObservation ->
                    _setupState.value = State.Items(null)
                    _date.value = newObservation.observationDate
                    val species = newObservation.species
                    if (species != null) {
                        _mushroom.value = Pair(species, DeterminationConfidence.fromDatabaseName(newObservation.confidence ?: DeterminationConfidence.CONFIDENT.databaseName))
                    } else {
                        _mushroom.value = null
                    }

                    newObservation.substrate?.let { _substrate.value = Pair(it, false) }
                    newObservation.vegetationType?.let { _vegetationType.value = Pair(it, false) }
                    _locality.value = newObservation.locality
                    _notes.value = newObservation.note
                    _ecologyNotes.value = newObservation.ecologyNote


                    viewModelScope.launch {
                        RoomService.hosts.getHostsWithIds(newObservation.hostIDs).apply {
                            onSuccess {
                                _hosts.value = Pair(it, false)
                            }
                        }
                    }

                    _images.value = newObservation.images.map {
                        Image.LocallyStored(File(it))
                    }.toMutableList()

                    val coordinate = newObservation.coordinate
                    val locality = newObservation.locality
                    if (coordinate != null) {
                        _coordinateState.value = State.Items(coordinate)
                        if (locality != null) {
                            _localitiesState.value = State.Items(listOf(locality))
                            _locality.value = newObservation.locality
                        } else {
                            getLocalities(coordinate)
                        }
                    } else {
                        _coordinateState.value = State.Empty()
                    }
                }
                onError {
                    _setupState.value = State.Error(it.toAppError(getApplication<MyApplication>().resources))
                }
            }
        }
    }

    fun setupAsNew() {
        _saveState.postValue(State.Empty())


        _date.value = Calendar.getInstance().time
        _locality.value = null
        _substrate.value = null
        _vegetationType.value = null
        _hosts.value = null
        _images.value = mutableListOf()
        _mushroom.value = null
        _notes.value = null
        _ecologyNotes.value = null
        _determinationNotes = null

        _coordinateState.value = State.Empty()
        _localitiesState.value = State.Empty()
        _predictionResultsState.value = State.Empty()

        viewModelScope.launch {
            SharedPreferences.getSubstrateID()?.let {
                RoomService.substrates.getSubstrateWithID(it).onSuccess {
                    _substrate.value = Pair(it, true)
                }
            }

            SharedPreferences.getVegetationTypeID()?.let {
                RoomService.vegetationTypes.getVegetationTypeWithID(it).onSuccess {
                    _vegetationType.value = Pair(it, true)
                }
            }

            SharedPreferences.getHosts()?.let {
                RoomService.hosts.getHostsWithIds(it).onSuccess {
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

    fun setCoordinateState(state: State<Location>) {
        fun setLocation(location: Location) {
            _date.value = location.date
            _coordinateState.value = state
            if (isAwaitingCoordinatedBeforeSave) {
                saveToLocal()
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
                    _saveState.postValue(State.Empty())
                }
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
                RoomService.substrates.saveSubstrate(substrate)
            }
        }
    }

    fun setVegetationType(vegetationType: VegetationType, isLocked: Boolean) {
        _vegetationType.value = Pair(vegetationType, isLocked)

        SharedPreferences.saveVegetationTypeID(if (isLocked) vegetationType.id else null)

        if (isLocked) {
            viewModelScope.launch {
                RoomService.vegetationTypes.saveVegetationType(vegetationType)
            }
        }
    }

    fun appendHost(host: Host, isLocked: Boolean) {
        val value = _hosts.value?.first ?: listOf()
        _hosts.value = Pair(value + listOf(host), isLocked)

        GlobalScope.launch {
            RoomService.hosts.saveHosts(listOf(host))
        }
        if (isLocked) {
            SharedPreferences.saveHostsID(_hosts.value?.first?.map { it.id })
        } else {
            SharedPreferences.saveHostsID(null)
        }
    }

    fun setHostsLockedState(value: Boolean) {
        if (hosts.value?.second != value)
        _hosts.value = Pair(hosts.value?.first ?: mutableListOf(), value)
    }

    fun removeHost(host: Host, isLocked: Boolean) {
        val value = _hosts.value?.first
        _hosts.value = Pair(value?.filter { it.id != host.id } ?: listOf(), isLocked)

        if (isLocked) {
            SharedPreferences.saveHostsID(_hosts.value?.first?.map { it.id })
        } else {
            SharedPreferences.saveHostsID(null)
        }
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
                is Image.LocallyStored -> {
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

    fun setMushroom(taxonID: Int?) {
        if (taxonID != null && taxonID != 0) {
            viewModelScope.launch(Dispatchers.IO) {
                DataService.getInstance(getApplication()).mushroomsRepository.getMushroom(taxonID).apply {
                    onSuccess {
                        _mushroom.postValue(Pair(it, DeterminationConfidence.CONFIDENT))
                    }
                    onError {}
                }
            }
        } else {
            _mushroom.value = null
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
                            _locality.postValue(locality)
                            if (type != AddObservationFragment.Type.Note && type != AddObservationFragment.Type.EditNote)
                            showNotification.postValue(
                                Notification.LocationFound(
                                    MyApplication.applicationContext.resources,
                                    locality,
                                    location
                                )
                            )
                        } else {
                            _locality.postValue(null)
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
                        if (type != AddObservationFragment.Type.Note && type != AddObservationFragment.Type.EditNote)
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

    fun upload() {
        _saveState.value = State.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            NewObservation(Date(),
                date.value ?: Date(),
                mushroom.value?.first,
                locality.value,
                substrate.value?.first,
                vegetationType.value?.first,
                (coordinateState.value as? State.Items)?.items,
                ecologyNotes.value,
                notes.value,
                mushroom.value?.second?.databaseName,
                _determinationNotes ?: (_determinationNotes),
                hosts.value?.first?.map { it.id } ?: listOf(),
                Image.getImagesForUpload(images.value?.toList() ?: listOf()).map { it.absolutePath }
            ).also {
                val isNew = when (type) {
                    AddObservationFragment.Type.New -> true
                    AddObservationFragment.Type.Edit -> false
                    AddObservationFragment.Type.FromRecognition -> true
                    AddObservationFragment.Type.Note -> true
                    AddObservationFragment.Type.EditNote -> true
                }
                when (val result = it.createJSON(isNew)) {
                    is Result.Error -> {
                        showNotification.postValue(
                            Notification.NewObservationError(
                                result.error,
                                MyApplication.resources
                            )
                        )
                        _saveState.postValue(State.Empty())
                    }
                    is Result.Success -> {
                        if (isNew) {
                            Session.uploadObservation(result.value, it.images.map { File(it) }).apply {
                                onError {
                                    _saveState.postValue(State.Empty())
                                    showNotification.postValue(Notification.Error(it)) }
                                onSuccess {
                                    _saveState.postValue(State.Items(null))
                                    showNotification.postValue(Notification.ObservationUploaded(MyApplication.resources, it.first))
                                }
                            }
                        } else {
                             Session.editObservation(id.toInt(), result.value, it.images.map { File(it) }).apply {
                                 onError {
                                     _saveState.postValue(State.Empty())
                                     showNotification.postValue(Notification.Error(it)) }
                                 onSuccess {
                                     _saveState.postValue(State.Items(null))
                                 }
                             }
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        _images.value?.forEach { (it as? Image.New)?.file?.delete() }
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
        super.onCleared()
    }

    fun delete() {
        when (type) {
            AddObservationFragment.Type.New, AddObservationFragment.Type.FromRecognition -> setupAsNew()
            AddObservationFragment.Type.Edit -> {
                _saveState.value = State.Loading()
                viewModelScope.launch {
                    Session.deleteObservation(id.toInt()).apply {
                        onError {
                            showNotification.postValue(Notification.Error(it))
                            _saveState.postValue(State.Empty())
                        }
                        onSuccess {
                            _saveState.postValue(State.Items(null))
                        }
                    }
                }
            }
            AddObservationFragment.Type.Note -> setupAsNew()
            AddObservationFragment.Type.EditNote -> {
                _saveState.value = State.Loading()
                viewModelScope.launch {
                    RoomService.notesDao.delete(NewObservation(Date(id), Date(), null, null, null, null, null, null, null, null, null, listOf(), listOf()))
                        .apply {
                        onError {
                            showNotification.postValue(Notification.Error(it.toAppError(MyApplication.resources)))
                            _saveState.postValue(State.Empty())
                        }
                        onSuccess {
                            _saveState.postValue(State.Items(null))
                        }
                    }
                }
            }
        }
    }


    fun saveToLocal() {
        _saveState.value = State.Loading()
        if (coordinateState.value !is State.Items) {
            isAwaitingCoordinatedBeforeSave = true
            return
        } else {
            isAwaitingCoordinatedBeforeSave = false
        }

        viewModelScope.launch(Dispatchers.IO) {
                val creationDate = if (type == AddObservationFragment.Type.EditNote) Date(id) else Date()
                val locallyStoredImages = _images.value?.mapNotNull { (it as? Image.LocallyStored)?.file } ?: listOf()
                val newImages = Image.saveToNotebookAlbum(_images.value?.toList() ?: listOf())
                val combinedImages = locallyStoredImages + newImages
                val newObservation = NewObservation(
                    creationDate,
                    date.value ?: Date(),
                    mushroom.value?.first,
                    locality.value,
                    substrate.value?.first,
                    vegetationType.value?.first,
                    coordinateState.value?.item,
                    ecologyNotes.value,
                    notes.value,
                    mushroom.value?.second?.databaseName,
                    _determinationNotes,
                    hosts.value?.first?.map { it.id } ?: listOf(),
                    combinedImages.map { it.absolutePath }
                )
                RoomService.notesDao.save(newObservation).apply {
                    onError {
                        _saveState.postValue(State.Error(it.toAppError(getApplication<MyApplication>().resources)))
                    }

                    onSuccess {
                        _saveState.postValue(State.Items(null))
                    }
                }
        }
    }
}