package com.noque.svampeatlas.view_models.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.noque.svampeatlas.view_models.MushroomsViewModel


class MushroomsViewModelFactory(private val category: MushroomsViewModel.Category?, private val application: Application): ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MushroomsViewModel(category, application) as T
    }
}
