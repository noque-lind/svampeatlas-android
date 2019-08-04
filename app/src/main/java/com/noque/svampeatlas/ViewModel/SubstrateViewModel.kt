package com.noque.svampeatlas.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.Model.Locality
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.Model.SubstrateGroup
import com.noque.svampeatlas.Services.DataService

class SubstrateViewModel(application: Application) : AndroidViewModel(application) {

    private val _state by lazy {MutableLiveData<State<List<SubstrateGroup>>>()}
    val state: LiveData<State<List<SubstrateGroup>>> get() = _state

    fun getSubstrateGroups() {
        _state.value = State.Loading()

        DataService.getInstance(getApplication()).getSubstrateGroups { result ->
            result.onSuccess {
                _state.value = State.Items(it)
            }

            result.onError {
                _state.value = State.Error(it)
            }
        }
    }
}