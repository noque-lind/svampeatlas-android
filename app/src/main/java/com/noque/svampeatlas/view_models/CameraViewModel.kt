package com.noque.svampeatlas.view_models

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.camera.core.CameraX
import androidx.core.net.toFile
import androidx.lifecycle.*
import com.noque.svampeatlas.R
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

    fun start() {
        _imageFileState.value = State.Empty()
    }

    fun setImageFile(imageFile: File) {
        _imageFileState.postValue(State.Items(imageFile))
        if (type == CameraFragment.Type.IDENTIFY) {
            getPredictions(imageFile)
        }
    }

    fun setImageFile(imageUri: Uri, file: File) {
        _imageFileState.value = State.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(imageUri)?.use {
                    it.copyTo(file)
                    setImageFile(file)
                }
            } catch (exception: FileNotFoundException) {
                val res = getApplication<Application>().resources
                _imageFileState.value = State.Error(AppError(res.getString(R.string.error_photosManager_unknownFetchError_title), res.getString(R.string.error_photosManager_unknownFetchError_message), null))
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

    private fun getPredictions(imageFile: File) {
        _predictionResultsState.postValue(State.Loading())

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getPredictions(imageFile) {
                it.onError { _predictionResultsState.value = State.Error(it) }
                it.onSuccess { _predictionResultsState.value = State.Items(it) }
            }
        }
    }
}