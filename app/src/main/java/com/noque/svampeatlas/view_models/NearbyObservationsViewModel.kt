package com.noque.svampeatlas.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.Geometry

class NearbyObservationsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = "NearbyObservationsViewModel"
    }

    private val _observationsState by lazy { MutableLiveData<State<List<Observation>>>() }
    val observationsState: LiveData<State<List<Observation>>> get() = _observationsState


    fun getObservationsNearby(latLng: LatLng) {
        _observationsState.value = State.Loading()

        val geometry = Geometry(latLng, 5000.0, Geometry.Type.CIRCLE)

        DataService.getInstance(getApplication()).getObservationsWithin(TAG, geometry, null, 2) {
            it.onSuccess {
                _observationsState.value = State.Items(it)
            }

            it.onError {
                _observationsState.value = State.Error(it)
            }
        }
    }

}