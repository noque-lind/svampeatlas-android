package com.noque.svampeatlas.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.fragments.NearbyFragment
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.api.Geometry

class NearbyObservationsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = "NearbyObservationsViewModel"
    }

    private val observations = mutableListOf<Observation>()
    private val geometries = mutableListOf<Geometry>()

    private val _observationsState by lazy { MutableLiveData<State<Pair<List<Observation>, List<Geometry>>>>() }
    val observationsState: LiveData<State<Pair<List<Observation>, List<Geometry>>>> get() = _observationsState

    init {
        _observationsState.value = State.Empty()
    }

    fun getObservationsNearby(latLng: LatLng, settings: NearbyFragment.Settings) {

        if (settings.clearAll) {
            observations.clear()
            geometries.clear()
        }

        _observationsState.value = State.Loading()

        val geometry = Geometry(latLng, settings.radius, Geometry.Type.CIRCLE)

        DataService.getInstance(getApplication()).getObservationsWithin(TAG, geometry, null, settings.ageInYears) {
            it.onSuccess {
                observations.addAll(it)
                geometries.add(geometry)
                _observationsState.value = State.Items(Pair(observations, geometries))
            }

            it.onError {
                _observationsState.value = State.Error(it)
            }
        }
    }

}