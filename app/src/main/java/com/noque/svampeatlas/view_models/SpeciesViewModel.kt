package com.noque.svampeatlas.view_models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.copyTo
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.Geometry
import com.noque.svampeatlas.views.BlankActivity
import kotlinx.coroutines.launch
import java.io.File

class SpeciesViewModel(val id: Int, application: Application) : AndroidViewModel(application) {

    sealed class Error(title: String, message: String) : AppError(title, message) {
        class NoObservationsNearby(application: Application): Error(application.getString(R.string.detailsFragment_noObservationsNearbyError_title), application.getString(R.string.detailsFragment_noObservationsNearbyError_message))
        class NoObservations(application: Application): Error(application.getString(R.string.detailsFragment_noObservationsError_title), application.getString(R.string.detailsFragment_noObservationsError_message))
    }

    companion object {
        val TAG = "SpeciesViewModel"
    }

    private val _mushroomState by lazy { MutableLiveData<State<Mushroom>>() }
    val mushroomState: LiveData<State<Mushroom>> get() = _mushroomState

    private val _heatMapObservationCoordinates by lazy { MutableLiveData<State<List<LatLng>>>() }
    val heatMapObservationCoordinates: LiveData<State<List<LatLng>>> get() = _heatMapObservationCoordinates

    private val _recentObservationsState by lazy { MutableLiveData<State<List<Observation>>>() }
    val recentObservationsState: LiveData<State<List<Observation>>> get() = _recentObservationsState

    private val _observationImageSaveState by lazy { MutableLiveData<State<File>>() }
    val observationImageSaveState: LiveData<State<File>> get() = _observationImageSaveState

    init {
        getMushroom(id)
        getRecentObservations(id)
    }


    private fun getMushroom(id: Int) {
        _mushroomState.value = State.Loading()


        viewModelScope.launch {
           val result = RoomService.getInstance(getApplication()).getMushroomWithID(id)
            result.onSuccess {
                _mushroomState.value = State.Items(it)
            }

            result.onError {
                DataService.getInstance(getApplication()).getMushroom(TAG, id) { result ->
                    result.onError {
                        _mushroomState.value = State.Error(it)
                    }

                    result.onSuccess {
                        _mushroomState.value = State.Items(it)
                    }
                }
            }
        }
    }

    private fun getRecentObservations(taxonID: Int) {
        _recentObservationsState.value = State.Loading()

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getRecentObservations(TAG, 0, taxonID) { result ->
                result.onSuccess {
                    if (it.count() == 0) {
                        _recentObservationsState.value = State.Error(Error.NoObservations(getApplication()))
                    } else {
                        _recentObservationsState.value = State.Items(it)
                    }
                }

                result.onError {
                    _recentObservationsState.value = State.Error(it)
                }
            }
        }
    }

    fun getHeatMapObservations(geometry: Geometry) {
        _heatMapObservationCoordinates.value = State.Loading()

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getObservationsWithin(TAG,
                geometry,
                id,
                null
            )  {

                it.onSuccess {
                    if (it.count() == 0) {
                        _heatMapObservationCoordinates.value = State.Error(Error.NoObservationsNearby(getApplication()))
                    } else {
                        _heatMapObservationCoordinates.value = State.Items(it.map { it.coordinate })
                    }
                }

                it.onError {
                    _heatMapObservationCoordinates.value = State.Error(it)
                }
            }
        }
    }

    fun saveObservationImage(imageFile: File) {
        _observationImageSaveState.value = State.Loading()

        viewModelScope.launch {
            when (val result = imageFile.copyTo(BlankActivity.createFile(getApplication()))) {
                is Result.Success -> {
                    _observationImageSaveState.value = State.Items(result.value)
                }
                is Result.Error -> {
                    _observationImageSaveState.value = State.Error(result.error)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "On Cleared")
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
    }
}