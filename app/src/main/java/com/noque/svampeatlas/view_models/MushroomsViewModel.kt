package com.noque.svampeatlas.view_models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.api.SpeciesQueries
import kotlinx.coroutines.launch


class MushroomsViewModel(category: Category?, application: Application) :
    AndroidViewModel(application) {

    companion object {
        val TAG = "MushroomsViewModel"
    }

    enum class Category {
        FAVORITES,
        SPECIES;

        companion object {
            val values = Category.values()
        }
    }

    private val _selectedCategory by lazy { MutableLiveData<Category>() }
    val selectedCategory: LiveData<Category> get() = _selectedCategory

    private val _mushroomsState by lazy { MutableLiveData<State<List<Mushroom>>>() }
    val mushroomsState: LiveData<State<List<Mushroom>>> get() = _mushroomsState

    private val _favoringState by lazy { MutableLiveData<State<Mushroom>>() }
    val favoringState: LiveData<State<Mushroom>> by lazy { _favoringState }

    init {
        category?.let { selectCategory(category) }
    }

    fun selectCategory(category: Category, forceReload: Boolean = false) {
        if (category == selectedCategory.value && !forceReload) return
        _selectedCategory.value = category
        getData(category)
    }

    private fun getData(category: Category) {
        when (category) {
            Category.FAVORITES -> getFavorites()
            Category.SPECIES -> getAllSpecies()
        }
    }

    private fun getAllSpecies() {
        _mushroomsState.value = State.Loading()

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getMushrooms(
                TAG,
                null,
                listOf(
                    SpeciesQueries.Attributes(true),
                    SpeciesQueries.DanishNames,
                    SpeciesQueries.Statistics,
                    SpeciesQueries.Images(true)
                )
            ) {
                it.onSuccess {
                    _mushroomsState.value = State.Items(it)
                }

                it.onError {
                    _mushroomsState.value = State.Error(it)
                }
            }
        }
    }

    private fun getFavorites() {
        _mushroomsState.value = State.Loading()

        viewModelScope.launch {
            val result = RoomService.mushrooms.getFavoritedMushrooms()
            result.onError {
                _mushroomsState.value = State.Error(it.toAppError(getApplication<MyApplication>().resources))
            }

            result.onSuccess {
                _mushroomsState.value = State.Items(it)
            }
        }
    }

    fun searchOffline(entry: String) {
        _mushroomsState.value = State.Loading()

        viewModelScope.launch {
            RoomService.mushrooms.getMushroomsFromSearch(entry).apply {
                onSuccess {
                    _mushroomsState.value = State.Items(it)
                }
                onError {
                    _mushroomsState.value = State.Error(it.toAppError(getApplication<MyApplication>().resources))
                }
            }
        }
    }

    fun search(entry: String, detailed: Boolean, allowGenus: Boolean = false) {
        _mushroomsState.value = State.Loading()

        val queries = mutableListOf(SpeciesQueries.DanishNames, SpeciesQueries.Images(false), SpeciesQueries.Attributes(null))

        if (detailed) queries.add(SpeciesQueries.Statistics)

        viewModelScope.launch {
            DataService.getInstance(getApplication()).getMushrooms(TAG, entry, queries) {
                it.onSuccess {
                    if (!allowGenus) {
                        _mushroomsState.value = State.Items(it.filterNot { it.isGenus })
                    } else {
                        _mushroomsState.value = State.Items(it)
                    }
                }

                it.onError {
                    _mushroomsState.value = State.Error(it)
                }
            }
        }
    }

    fun favoriteMushroomAt(index: Int) {
        (mushroomsState.value as? State.Items)?.items?.getOrNull(index)?.let {
            _favoringState.value = State.Loading()
            viewModelScope.launch {
                DataService.getInstance(getApplication()).mushroomsRepository.getMushroom(it.id).apply {
                    onSuccess {
                        viewModelScope.launch {
                            it.isUserFavorite = true
                            RoomService.mushrooms.saveMushroom(it)
                            _favoringState.value = State.Items(it)
                        }
                    }

                    onError {
                        _favoringState.value = State.Error(it)
                    }
                }
            }


        }
    }

    fun unFavoriteMushroomAt(index: Int) {
        (mushroomsState.value as? State.Items)?.items?.getOrNull(index)?.let { mushroom ->
            viewModelScope.launch {
                mushroom.isUserFavorite = false
                RoomService.mushrooms.saveMushroom(mushroom)
                RoomService.mushrooms.deleteMushroom(mushroom)
                _mushroomsState.value = (_mushroomsState.value as? State.Items)?.let { state ->
                    State.Items(state.items.minus(mushroom))
                }
            }
        }
    }

    fun reloadData() {
        val selectedCategory = selectedCategory.value

        if (selectedCategory == null) {
            _mushroomsState.value = State.Empty()
        } else {
            getData(selectedCategory)
        }
    }

    fun resetFavoritizingState() {
        _favoringState.value = State.Empty()
    }

    override fun onCleared() {
        Log.d(TAG, "Cleared")
        super.onCleared()
        DataService.getInstance(getApplication()).clearRequestsWithTag(TAG)
    }
}