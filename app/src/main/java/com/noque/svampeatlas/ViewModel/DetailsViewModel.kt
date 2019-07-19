package com.noque.svampeatlas.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Services.DataService
import com.noque.svampeatlas.Utilities.Geometry

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    val selected = MutableLiveData<Mushroom>()
    val heatMapObservationCoordinates = MutableLiveData<State<List<LatLng>>>()

    fun select(mushroom: Mushroom) {
        selected.value = mushroom
    }

    init {
        Log.d("DetailsViewModel", "Was inited")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("DetailsViewModel", "Was Cleared")
    }

    fun getHeatMapObservations(taxonID: Int, geometry: Geometry) {
        heatMapObservationCoordinates.value = State.Loading()

        DataService.getInstance(getApplication()).getObservationsWithin(
            geometry,
            taxonID,
            null
        )  {
            it.onSuccess {
                heatMapObservationCoordinates.value = State.Items(it.map { it.coordinate })
            }
        }
    }
}