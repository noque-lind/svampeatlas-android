package com.noque.svampeatlas.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.Services.DataService



class MushroomsViewModel(application: Application) : AndroidViewModel(application) {

    val state by lazy {MutableLiveData<State<List<Mushroom>>>()}

    fun search(entry: String) {
        state.value = State.Loading()

        DataService.getInstance(getApplication()).getMushrooms(entry) {
            it.onSuccess {
                state.value = State.Items(it)
            }

            it.onError {
                state.value = State.Error(it)
            }
        }
    }

    fun start() {
        state.value = State.Loading()

        DataService.getInstance(getApplication()).getMushrooms(0) {
            it.onSuccess {
                state.value = State.Items(it)
            }

            it.onError {
                state.value = State.Error(it)
            }
        }
    }
}