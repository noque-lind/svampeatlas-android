package com.noque.svampeatlas.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.Model.Animal
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Services.DataService
import com.noque.svampeatlas.Utilities.SharedPreferencesHelper
//import io.reactivex.disposables.CompositeDisposable
//import io.reactivex.schedulers.Schedulers

class ListViewModel(application: Application) : AndroidViewModel(application) {

    val mushrooms by lazy { MutableLiveData<List<Animal>>()}
    val loadError by lazy { MutableLiveData<Boolean>() }
    val loading by lazy { MutableLiveData<Boolean>() }

    fun refresh() {
        loading.value = true

        getKey()
    }

    private val preferences = SharedPreferencesHelper(getApplication())

    private fun getMushrooms() {
        DataService.getInstance(getApplication()).getMushrooms(0) {
            it.onSuccess {
                println(it)
            }
        }
    }


    private fun getKey() {
            getMushrooms()


//        val key = preferences.getAPIKey()
//
//        if (key.isNullOrEmpty()) {
//            DataService.getInstance(getApplication()).getAPIKey {
//                it.onFailure { error ->
//                    loading.value = false
//                    loadError.value = true
//
//                    Log.d("ListViewModel", error.message.toString())
//                }
//
//                it.onSuccess {
//                    preferences.saveAPIKey(it.key)
//                    getAnimals(it.key!!)
//                    Log.d("ListViewModel", it.key)
//                }
//            }
//        } else {
//
//            // Just know that when a variable has been checked if nil, Kotlin knows that it is not nul and it is therefore not necesarry to unwrap
//            getAnimals(key)
//        }


        }

    private fun getAnimals(key: String) {
//        DataService.getInstance(getApplication()).getAnimals(key) {
//            it.onSuccess {
//                Log.d("ListViewModel", it.toString())
//                mushrooms.value = it
//                loading.value = false
//            }
//
//            it.onFailure {
//                loadError.value = true
//                loading.value = false
//            }
//        }
    }
}