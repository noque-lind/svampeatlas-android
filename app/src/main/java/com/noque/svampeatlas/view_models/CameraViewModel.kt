package com.noque.svampeatlas.view_models

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.copyTo
import com.noque.svampeatlas.extensions.getExifLocation
import com.noque.svampeatlas.fragments.CameraFragment
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Location
import com.noque.svampeatlas.models.PredictionResult
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*




class CameraViewModel(private val type: CameraFragment.Type, application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = "CameraViewModel"
    }

    private val _imageFileState by lazy { MutableLiveData<State<File>>() }
    val imageFileState: LiveData<State<File>> get() = _imageFileState

    private val _predictionResultsState by lazy { MutableLiveData<State<List<PredictionResult>>>() }
    val predictionResultsState: LiveData<State<List<PredictionResult>>> get() = _predictionResultsState

    init {
        _imageFileState.value = State.Empty()
        _predictionResultsState.value = State.Empty()
    }

    fun setImageFile(imageFile: File) {
        _imageFileState.postValue(State.Items(imageFile))
        if (type == CameraFragment.Type.IDENTIFY) {
            viewModelScope.launch { getPredictions(imageFile) }
        }
    }

    fun setImageFile(imageUri: Uri, file: File) {
        _imageFileState.postValue(State.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(imageUri)?.use {
                    val output: OutputStream = FileOutputStream(file)
                    it.copyTo(output)
                    output.flush()
                    setImageFile(file)
                    it.close()
                }
            } catch (exception: FileNotFoundException) {
                val res = getApplication<Application>().resources
                _imageFileState.postValue(State.Error(AppError(res.getString(R.string.error_photosManager_unknownFetchError_title), res.getString(R.string.error_photosManager_unknownFetchError_message), null)))
            }
        }
    }

    fun setImageFileError(error: AppError) {
        _imageFileState.postValue(State.Error(error))
    }

    fun reset() {
        _imageFileState.value?.let {
            if (it is State.Items) it.items.delete()
        }

        _imageFileState.value = State.Empty()
        _predictionResultsState.value = State.Empty()
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
    }

    private suspend fun getPredictions(imageFile: File) = withContext(Dispatchers.Default) {
        _predictionResultsState.postValue(State.Loading())
            DataService.getInstance(getApplication()).getPredictions(imageFile) {
                it.onError { _predictionResultsState.postValue(State.Error(it)) }
                it.onSuccess { _predictionResultsState.postValue(State.Items(it)) }
        }
    }
}