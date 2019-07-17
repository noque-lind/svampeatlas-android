package com.noque.svampeatlas.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Services.DataService

sealed class State<T> {
    class Items<T>(val items: T): State<T>()
    class Empty<T>(): State<T>()
    class Loading<T>(): State<T>()
}


class MushroomsViewModel(application: Application) : AndroidViewModel(application) {

    val state by lazy {MutableLiveData<State<List<Mushroom>>>()}


    fun start() {
        state.value = State.Loading()

        DataService.getInstance(getApplication()).getMushrooms(0) {
            it.onSuccess {
                state.value = State.Items(it)
            }
        }
    }
}