package com.noque.svampeatlas.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.Services.DataService



class MushroomsViewModel(application: Application) : AndroidViewModel(application) {


    private val _mushroomsState by lazy {MutableLiveData<State<List<Mushroom>>>()}
    val mushroomsState: LiveData<State<List<Mushroom>>> get() = _mushroomsState

    private val _mushroomState by lazy {MutableLiveData<State<Mushroom>>()}
    val mushroomState: LiveData<State<Mushroom>> get() = _mushroomState

    private val _selectedMushroom by lazy {MutableLiveData<Mushroom?>()}
    val selectedMushroom: LiveData<Mushroom?> get() = _selectedMushroom

    fun search(entry: String) {
        _mushroomsState.value = State.Loading()

        DataService.getInstance(getApplication()).getMushrooms(entry) {
            it.onSuccess {
                _mushroomsState.value = State.Items(it)
            }

            it.onError {
                _mushroomsState.value = State.Error(it)
            }
        }
    }

    fun start() {
        _mushroomsState.value = State.Loading()

        DataService.getInstance(getApplication()).getMushrooms(0) {
            it.onSuccess {
                _mushroomsState.value = State.Items(it)
            }

            it.onError {
                _mushroomsState.value = State.Error(it)
            }
        }
    }

    fun getMushroom(id: Int) {
        Log.d("MushroomsViewModel",  mushroomsState.value.toString())

        var mushroom: Mushroom? = null



        mushroomsState.value?.let {
            Log.d("MushroomsViewModel", it.toString())


            mushroom = when (it) {
                is State.Items -> {
                    Log.d("MushroomsViewModel", it.items.toString())

                    it.items.find { it.id == id }}
                else -> { null }
            }
        }

        if (mushroom != null) {
            _mushroomState.value = State.Items(mushroom!!)
        } else {
            // Fetch from dataservice
        }
    }

    fun setMushroom(mushroom: Mushroom?) {
        _selectedMushroom.value = mushroom
    }

    fun clearData() {
        _mushroomState.value = State.Empty()
        _mushroomsState.value = State.Empty()
    }

    override fun onCleared() {
        Log.d("MushroomsViewMod", "Cleard")
        super.onCleared()
    }
}