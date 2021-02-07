package com.noque.svampeatlas.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noque.svampeatlas.models.NewObservation
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.SingleLiveEvent
import kotlinx.coroutines.launch

class NotesFragmentViewModel: ViewModel() {

    private val _notes by lazy {MutableLiveData<State<List<NewObservation>>>(State.Empty()) }
    val notes: LiveData<State<List<NewObservation>>> get() = _notes

    val noteDeleted = SingleLiveEvent<Int>()

    init {
        getNotes()
    }


    fun getNotes() {
        _notes.value = State.Loading()
        viewModelScope.launch {
            RoomService.notesDao.getAll().apply {
                onSuccess {
                    _notes.value = State.Items(it)
                }

                onError {
                    _notes.value = State.Error(it.toAppError(MyApplication.resources))
                }
            }
        }
    }

    fun deleteNote(note: NewObservation, index: Int) {
        viewModelScope.launch {
            RoomService.notesDao.delete(note).apply {
                onSuccess {
                    getNotes()
                }
            }
        }
    }
}