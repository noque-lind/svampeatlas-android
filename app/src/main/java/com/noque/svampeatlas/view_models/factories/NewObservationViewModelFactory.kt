package com.noque.svampeatlas.view_models.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.noque.svampeatlas.fragments.AddObservationFragment
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.view_models.NewObservationViewModel

@Suppress("UNCHECKED_CAST")
class NewObservationViewModelFactory(val type: AddObservationFragment.Context, val id: Long, val mushroomId: Int, val imageFilePath: String?,  val application: Application):
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NewObservationViewModel(application, type, id, mushroomId, imageFilePath) as T
    }

}