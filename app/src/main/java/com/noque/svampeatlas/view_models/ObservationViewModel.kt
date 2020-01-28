package com.noque.svampeatlas.view_models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.noque.svampeatlas.models.Comment
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import kotlinx.coroutines.launch

class ObservationViewModel(val id: Int, val getSpecies: Boolean,  application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = "ObservationViewModel"
    }

    private val _observationState by lazy { MutableLiveData<State<Observation>>() }
    val observationState: LiveData<State<Observation>> get() = _observationState

    private val _mushroomState by lazy { MutableLiveData<State<Mushroom>>() }
    val mushroomState: LiveData<State<Mushroom>> get() = _mushroomState

    init {
        getObservation(id)
    }

    private fun getObservation(id: Int) {
        _observationState.value = State.Loading()

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getObservation(TAG, id) {
                it.onSuccess {
                    _observationState.value = State.Items(it)

                    if (getSpecies) {
                        getMushroom(it.speciesProperties.id)
                    }
                }

                it.onError {
                    _observationState.value = State.Error(it)
                }
            }
        }
    }

    fun getMushroom(id: Int) {
        _mushroomState.value = State.Loading()

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getMushroom(TAG, id) {
                it.onError {
                    _mushroomState.value = State.Error(it)
                }

                it.onSuccess {
                    _mushroomState.value = State.Items(it)
                }
            }
        }
    }

    fun addComment(comment: Comment) {
        (observationState.value as? State.Items)?.items?.addComment(comment)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "On cleared")
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
    }
}