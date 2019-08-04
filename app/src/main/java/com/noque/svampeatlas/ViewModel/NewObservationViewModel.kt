package com.noque.svampeatlas.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.Model.Locality
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.NewObservation
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.Services.DataService
import com.noque.svampeatlas.Utilities.Geometry

class NewObservationViewModel(application: Application) : AndroidViewModel(application) {

    private val _locality by lazy {MutableLiveData<Locality>()}
    private val _coordinate by lazy {MutableLiveData<LatLng>()}


    val locality: LiveData<Locality> get() = _locality
    val coordinate: LiveData<LatLng> get() = _coordinate

    private val _localityState by lazy { MutableLiveData<State<List<Locality>>>() }
    val localityState: LiveData<State<List<Locality>>> get() = _localityState

    fun setLocality(locality: Locality) {
        Log.d("ViewModel", "Locality set to ${locality.toString()}")
        _locality.value = locality
    }

    fun setCoordinate(coordinate: LatLng) {
        _coordinate.value = coordinate
    }




    fun getLocalities(latLng: LatLng) {
        _localityState.value = State.Loading()

        val geometry = Geometry(
            latLng,
            5000.0,
            Geometry.Type.RECTANGLE
        )

        DataService.getInstance(getApplication()).getLocalities(geometry) { result ->
            result.onSuccess {
                Log.d("DataService", it.toString())
                _localityState.value = State.Items(it)
            }

            result.onError {
                _localityState.value = State.Error(it)
            }
        }
    }


}