package com.noque.svampeatlas.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.Observation
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.Services.DataService
import com.noque.svampeatlas.Utilities.Geometry

class ObservationsViewModel(application: Application) : AndroidViewModel(application) {

    private val _observationState by lazy { MutableLiveData<State<Observation>>() }
    val observationState: LiveData<State<Observation>> get() = _observationState

    private val _heatMapObservationCoordinates by lazy { MutableLiveData<State<List<LatLng>>>() }
    val heatMapObservationCoordinates: LiveData<State<List<LatLng>>> get() = _heatMapObservationCoordinates


    fun getObservation(id: Int) {
        DataService.getInstance(getApplication()).getObservation(id) {
            it.onSuccess {
                _observationState.value = State.Items(it)
            }

            it.onError {
                _observationState.value = State.Error(it)
            }
        }
    }

    fun getHeatMapObservations(taxonID: Int, geometry: Geometry) {
        _heatMapObservationCoordinates.value = State.Loading()

        DataService.getInstance(getApplication()).getObservationsWithin(
            geometry,
            taxonID,
            null
        )  {

            it.onSuccess {
                _heatMapObservationCoordinates.value = State.Items(it.map { it.coordinate })
            }

            it.onError {
                _heatMapObservationCoordinates.value = State.Error(it)
            }
        }
    }
}