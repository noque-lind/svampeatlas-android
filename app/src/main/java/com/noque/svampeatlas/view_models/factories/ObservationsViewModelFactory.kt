package com.noque.svampeatlas.view_models.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.noque.svampeatlas.view_models.ObservationViewModel

class ObservationsViewModelFactory(val id: Int, private val  showSpecies: Boolean, val application: Application): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ObservationViewModel(id, showSpecies, application) as T
    }
}