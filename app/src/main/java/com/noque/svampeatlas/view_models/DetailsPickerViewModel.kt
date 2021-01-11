package com.noque.svampeatlas.view_models

import android.app.Application
import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.fragments.add_observation.DetailsPickerFragment
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.Database
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import kotlinx.coroutines.launch

class DetailsPickerViewModel(private val type: DetailsPickerFragment.Type, application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = "PickerViewModel"
    }

    private val _substrateGroupsState by lazy { MutableLiveData<State<List<SubstrateGroup>>>() }
    val substrateGroupsState get() = _substrateGroupsState

    private val _vegetationTypesState by lazy { MutableLiveData<State<List<VegetationType>>>() }
    val vegetationTypesState get() = _vegetationTypesState

    private val _hostsState by lazy { MutableLiveData<State<Pair<List<Host>, Boolean>>>() }
    val hostsState get() = _hostsState

    init {
        when (type) {
            DetailsPickerFragment.Type.SUBSTRATEPICKER -> getSubstrateGroups()
            DetailsPickerFragment.Type.VEGETATIONTYPEPICKER -> getVegetationTypes()
            DetailsPickerFragment.Type.HOSTPICKER -> getHosts(null)
        }
    }

    private fun getSubstrateGroups() {
        _substrateGroupsState.value = State.Loading()
        viewModelScope.launch {
            RoomService.substrates.getSubstrates().apply {
                onError {
                    DataService.getInstance(getApplication()).getSubstrateGroups(TAG) { result ->
                        result.onSuccess {
                            _substrateGroupsState.value = State.Items(it)
                        }

                        result.onError {
                            _substrateGroupsState.value = State.Error(it)
                        }
                    }
                }

                onSuccess {
                    _substrateGroupsState.postValue(State.Items(SubstrateGroup.createFromSubstrates(it)))
                }
            }
        }
    }

    private fun getVegetationTypes() {
        _vegetationTypesState.value = State.Loading()

        viewModelScope.launch {
            RoomService.vegetationTypes.getAll().apply {
                onSuccess { _vegetationTypesState.postValue(State.Items(it)) }
                onError {
                    DataService.getInstance(getApplication()).getVegetationTypes(TAG) { result ->
                        result.onSuccess {
                            _vegetationTypesState.value = State.Items(it)
                        }

                        result.onError {
                            _vegetationTypesState.value = State.Error(it)
                        }
                    }
                }
            }
        }
    }


    fun getHosts(searchTerm: String?) {
        _hostsState.value = State.Loading()

        viewModelScope.launch {
            if (searchTerm != null) {
                DataService.getInstance(getApplication()).getHosts(TAG, searchTerm) { result ->
                    result.onSuccess {
                        _hostsState.postValue(State.Items(Pair(it, false)))
                    }

                    result.onError {
                        _hostsState.postValue(State.Error(it))
                    }
                }
            } else {
                RoomService.hosts.getAll().apply {
                    onSuccess { _hostsState.postValue(State.Items(Pair(it, true))) }
                    onError {
                        DataService.getInstance(getApplication()).getHosts(TAG, null) { result ->
                            result.onSuccess {
                                _hostsState.postValue(State.Items(Pair(it, true)))
                            }

                            result.onError {
                                _hostsState.postValue(State.Error(it))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
    }
}