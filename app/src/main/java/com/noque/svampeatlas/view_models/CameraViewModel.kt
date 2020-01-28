package com.noque.svampeatlas.view_models

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.noque.svampeatlas.extensions.copyTo
import com.noque.svampeatlas.extensions.getBitmap
import com.noque.svampeatlas.fragments.CameraFragment
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.PredictionResult
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.ExifUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

class CameraViewModel(private val type: CameraFragment.Type, application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = "CameraViewModel"
    }

    private val _imageFileState by lazy { MutableLiveData<State<File>>() }
    val imageFileState: LiveData<State<File>> get() = _imageFileState

    private val _predictionResultsState by lazy { MutableLiveData<State<List<PredictionResult>>>() }
    val predictionResultsState: LiveData<State<List<PredictionResult>>> get() = _predictionResultsState

    private val _imageSaveState by lazy { MutableLiveData<State<File>>() }
    val imageSaveState: LiveData<State<File>> get() = _imageSaveState


    fun start() {
        _imageFileState.value = State.Empty()
    }

    fun setImageFile(imageFile: File) {
        _imageFileState.value = State.Items(imageFile)

        if (type == CameraFragment.Type.IDENTIFY) {
            getPredictions(imageFile)
        }
    }

    fun setImageFile(imageUri: Uri, file: File) {
        _imageFileState.value = State.Loading()

        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.openInputStream(imageUri)?.let {
                    it.copyTo(file)
                    setImageFile(file)
                }
            } catch (exception: FileNotFoundException) {
                _imageFileState.value = State.Error(AppError("Der skete en fejl", "Den valgte fil kunne ikke findes"))
            }
        }
    }

    fun setImageFileError(error: AppError) {
        _imageFileState.value = State.Error(error)
    }

    fun reset() {
        _imageFileState.value?.let {
            if (it is State.Items) it.items.delete()
        }

        _imageFileState.value = State.Empty()
        _predictionResultsState.value = State.Empty()
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
    }

    fun saveImage(file: File) {
        (imageFileState.value as? State.Items)?.items?.let {
            _imageSaveState.value = State.Loading()

            viewModelScope.launch {
                val result = it.copyTo(file)
                result.onError {
                    _imageSaveState.value = State.Error(it)
                }

                result.onSuccess {
                    _imageSaveState.value = State.Items(it)
                }
            }
        }
    }

    private fun getPredictions(imageFile: File) {
        _predictionResultsState.value = State.Loading()

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getPredictions(imageFile) {
                it.onError { _predictionResultsState.value = State.Error(it) }
                it.onSuccess { _predictionResultsState.value = State.Items(it) }
            }
        }
    }

    override fun onCleared() {
        reset()
        super.onCleared()
    }
}