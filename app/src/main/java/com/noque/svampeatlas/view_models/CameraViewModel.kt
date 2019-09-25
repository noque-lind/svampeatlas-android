package com.noque.svampeatlas.view_models

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.noque.svampeatlas.fragments.CameraFragment
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.PredictionResult
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.ExifUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

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
        viewModelScope.launch {
            getApplication<Application>().contentResolver.openInputStream(imageUri)?.let {
                val outputStream = FileOutputStream(file)
                copyStream(it, outputStream)
                setImageFile(file)
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
                copyStream(it.inputStream(), FileOutputStream(file))
                _imageSaveState.value = State.Items(file)
            }
        }
    }

    private fun getPredictions(imageFile: File) {
        _predictionResultsState.value = State.Loading()

        viewModelScope.launch {
            val bitmap = getBitmap(imageFile)
            DataService.getInstance(getApplication()).getPredictions(bitmap) {
                it.onError { _predictionResultsState.value = State.Error(it) }
                it.onSuccess { _predictionResultsState.value = State.Items(it) }
            }
        }
    }

    fun getPredictionNotes(selectedPrediction: PredictionResult): String {
            var string = ""

        string += "#imagevision_score: ${selectedPrediction.mushroom.fullName} ${String.format("%.1f", selectedPrediction.score)}; "

        (predictionResultsState.value as? State.Items)?.items?.let {
            string += "#imagevision_list: "

            it.forEach {
                string += "${it.mushroom.fullName} ${String.format("%.1f", it.score)}, "
            }

            string.dropLast(2)
        }

        return string
    }

    private suspend fun copyStream(inputStream: InputStream, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        var length = inputStream.read(buffer)

        //Transferring data
        while(length != -1) {
            outputStream.write(buffer, 0, length)
            length = inputStream.read(buffer)
        }

        //Finalizing
        outputStream.flush()
        outputStream.close()
        inputStream.close()
    }

    suspend private fun getBitmap(imageFile: File) = withContext(Dispatchers.IO) {
        return@withContext getRotatedBitmap(BitmapFactory.decodeFile(imageFile.absolutePath), imageFile)
    }

    private suspend fun getRotatedBitmap(bitmap: Bitmap, imageFile: File) = withContext(Dispatchers.Default) {
        return@withContext ExifUtil.rotateBitmap(imageFile.absolutePath, bitmap)
    }

    override fun onCleared() {
        reset()
        super.onCleared()
    }
}