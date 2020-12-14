package com.noque.svampeatlas.view_models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.OnProgressListener
import com.downloader.PRDownloader
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.FileManager
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.api.SpeciesQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader


class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    sealed class LoadingState(val message: String) {
        object Downloading: LoadingState("Downloading")
        object FinishedDownloading: LoadingState("Finished Downloading")
        object ReadingFile: LoadingState("Reading File")
        object SavingFile: LoadingState("Saving File to database")
    }


    private val _loadingState by lazy { MutableLiveData<LoadingState>() }
    val loadingState: LiveData<LoadingState> get() = _loadingState

    private val _state by lazy { MutableLiveData<State<Void?>>(State.Empty()) }
    val state: LiveData<State<Void?>> get() = _state


    private val onProgressListener = OnProgressListener {

    }

    init {
        val file = FileManager.createDocumentFile("Taxon", application)
        val api = API(APIType.Request.Mushrooms(null, listOf(SpeciesQueries.Attributes(null), SpeciesQueries.Images(false), SpeciesQueries.Statistics, SpeciesQueries.DanishNames), 0, null))

        _state.value = State.Loading()
        _loadingState.value = LoadingState.Downloading
         PRDownloader.download(api.url(), file.parent, file.name).build().start(object: OnDownloadListener {
            override fun onDownloadComplete() {
                viewModelScope.launch {
                    readFile(file)
                }
            }

            override fun onError(error: Error?) {
                Log.d("Some", error?.serverErrorMessage)
                Log.d("Some", error?.isConnectionError.toString())
            }

        })
    }

    suspend fun readFile(file: File) = withContext(Dispatchers.IO) {
        _loadingState.postValue(LoadingState.ReadingFile)
        val gson = GsonBuilder()
            .create()
        val json = FileReader(file)
        val some = gson.fromJson<List<Mushroom>>(json, object : TypeToken<List<Mushroom>>() {}.type)
        Log.d("Some", some.count().toString())
        file.delete()
        _loadingState.postValue(LoadingState.SavingFile)
        RoomService.saveMushrooms(some)
        _state.postValue(State.Items(null))
    }
}