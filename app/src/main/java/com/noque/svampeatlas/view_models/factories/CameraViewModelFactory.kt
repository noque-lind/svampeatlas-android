package com.noque.svampeatlas.view_models.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.noque.svampeatlas.fragments.CameraFragment
import com.noque.svampeatlas.fragments.add_observation.DetailsPickerFragment
import com.noque.svampeatlas.view_models.CameraViewModel
import com.noque.svampeatlas.view_models.DetailsPickerViewModel

class CameraViewModelFactory(private val type: CameraFragment.Type, private val application: Application): ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return CameraViewModel(type, application) as T
    }
}