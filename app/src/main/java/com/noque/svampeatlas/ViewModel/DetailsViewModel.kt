package com.noque.svampeatlas.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.Model.Mushroom

class DetailsViewModel(application: Application) : AndroidViewModel(application) {
    val selected = MutableLiveData<Mushroom>()

    fun select(mushroom: Mushroom) {
        selected.value = mushroom
    }

    init {
        Log.d("DetailsViewModel", "Was inited")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("DetailsViewModel", "Was Cleared")
    }
}