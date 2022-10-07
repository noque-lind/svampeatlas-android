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
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RecognitionService
import com.noque.svampeatlas.utilities.MyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*




class CameraViewModel(private val type: CameraFragment.Context, application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAG = "CameraViewModel"
    }

    private val recognitionService = RecognitionService()

    private val _imageFileState by lazy { MutableLiveData<State<File>>(State.Empty()) }
    val imageFileState: LiveData<State<File>> get() = _imageFileState

    private val _predictionResultsState by lazy { MutableLiveData<State<Pair<List<Prediction>, Boolean>>>(State.Empty()) }
    val predictionResultsState: LiveData<State<Pair<List<Prediction>, Boolean>>> get() = _predictionResultsState

    fun setImageFile(imageFile: File) {
        _imageFileState.postValue(State.Items(imageFile))
        if (type == CameraFragment.Context.IDENTIFY) {
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
                _imageFileState.postValue(State.Error(AppError(res.getString(R.string.elPhotosError_unknownFetchError_title), res.getString(R.string.elPhotosError_unknownFetchError_message), null)))
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
        recognitionService.reset()
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
    }

    private suspend fun getPredictions(imageFile: File) = withContext(Dispatchers.Default) {
       try {
           _predictionResultsState.postValue(State.Loading())
           recognitionService.addPhotoToRequest(imageFile)
           delay(2000)
           val predictionResults = mutableListOf<Prediction>()
           val result = recognitionService.getResults()
           if (result != null) {
               for (index in result.taxonIds.indices) {
                   DataService.getInstance(MyApplication.applicationContext).mushroomsRepository.getMushroom(result.taxonIds[index]).onSuccess {
                       predictionResults.add(Prediction(it,result.conf[index]))
                   }
               }
               _predictionResultsState.postValue(State.Items(Pair(predictionResults, result.reliablePrediction)))
           } else {

           }
       } catch (error: Exception) {
           Log.d(TAG, error.toString())
       }
    }
}