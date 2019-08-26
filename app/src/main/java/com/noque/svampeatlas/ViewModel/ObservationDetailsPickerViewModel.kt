package com.noque.svampeatlas.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.Model.Host
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.Model.SubstrateGroup
import com.noque.svampeatlas.Model.VegetationType
import com.noque.svampeatlas.Services.DataService

class ObservationDetailsPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val _substrateGroupsState by lazy { MutableLiveData<State<List<SubstrateGroup>>>() }
    val substrateGroupsState get() = _substrateGroupsState

    private val _vegetationTypesState by lazy { MutableLiveData<State<List<VegetationType>>>() }
    val vegetationTypesState get() = _vegetationTypesState

    private val _hostsState by lazy { MutableLiveData<State<List<Host>>>() }
    val hostsState get() = _hostsState


    fun getSubstrateGroups() {
        _substrateGroupsState.value = State.Loading()

        DataService.getInstance(getApplication()).getSubstrateGroups { result ->
            result.onSuccess {
                _substrateGroupsState.value = State.Items(it)
            }

            result.onError {
                _substrateGroupsState.value = State.Error(it)
            }
        }
    }

    fun getVegetationTypes() {
        _substrateGroupsState.value = State.Loading()

        DataService.getInstance(getApplication()).getVegetationTypes { result ->
            result.onSuccess {
                _vegetationTypesState.value = State.Items(it)
            }

            result.onError {
                _vegetationTypesState.value = State.Error(it)
            }
        }
    }


    fun getHosts() {
        _hostsState.value = State.Loading()

            DataService.getInstance(getApplication()).getHosts(null) { result ->
                result.onSuccess {
                    _hostsState.value = State.Items(it)
                }

                result.onError {
                    _hostsState.value = State.Error(it)
                }
            }
    }
}