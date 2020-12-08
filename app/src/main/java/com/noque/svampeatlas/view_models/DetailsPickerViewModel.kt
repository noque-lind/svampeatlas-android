package com.noque.svampeatlas.view_models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.models.SubstrateGroup
import com.noque.svampeatlas.models.VegetationType
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.fragments.add_observation.DetailsPickerFragment
import kotlinx.coroutines.launch

class DetailsPickerViewModel(private val type: DetailsPickerFragment.Type, application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = "PickerViewModel"
    }

    private val _substrateGroupsState by lazy { MutableLiveData<State<List<SubstrateGroup>>>() }
    val substrateGroupsState get() = _substrateGroupsState

    private val _vegetationTypesState by lazy { MutableLiveData<State<List<VegetationType>>>() }
    val vegetationTypesState get() = _vegetationTypesState

    private val _hostsState by lazy { MutableLiveData<State<List<Host>>>() }
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
            DataService.getInstance(getApplication()).getSubstrateGroups(TAG) { result ->
                result.onSuccess {
                    _substrateGroupsState.value = State.Items(it)
                }

                result.onError {
                    _substrateGroupsState.value = State.Error(it)
                }
            }
        }
    }

    private fun getVegetationTypes() {
        _substrateGroupsState.value = State.Loading()

        viewModelScope.launch {
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


    fun getHosts(searchTerm: String?) {
        _hostsState.value = State.Loading()

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getHosts(TAG, searchTerm) { result ->
                result.onSuccess {
                    _hostsState.value = State.Items(it)
                }

                result.onError {
                    _hostsState.value = State.Error(it)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
        Log.d(TAG, "On cleared")
    }
}