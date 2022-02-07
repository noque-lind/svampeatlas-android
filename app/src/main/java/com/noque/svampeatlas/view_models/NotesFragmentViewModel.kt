package com.noque.svampeatlas.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noque.svampeatlas.models.NewObservation
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.models.UserObservation
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NotesFragmentViewModel: ViewModel() {

    private val _uploadState by lazy { MutableLiveData<State<Void?>>(State.Empty()) }
    val uploadState: LiveData<State<Void?>> = _uploadState


    private val _notes by lazy { MutableLiveData<State<MutableList<NewObservation>>>(State.Empty()) }
    val notes: LiveData<State<MutableList<NewObservation>>> get() = _notes

    init {
        getNotes()
    }


    fun getNotes() {
        _notes.postValue(State.Loading())
        viewModelScope.launch {
            RoomService.notesDao.getAll().apply {
                onSuccess {
                    _notes.value = State.Items(it.toMutableList())
                }

                onError {
                    _notes.value = State.Error(it.toAppError(MyApplication.resources))
                }
            }
        }
    }

    fun deleteNote(note: NewObservation, index: Int) {
        viewModelScope.launch {
            note.images.forEach { File(it).delete() }
            RoomService.notesDao.delete(note).apply {
                onSuccess {
                    UserObservation(note).deleteAllImages()
                    getNotes()
                }
            }
        }
    }

    fun uploadNewObservation(note: NewObservation) {
        viewModelScope.launch(Dispatchers.IO) {
            _uploadState.postValue(State.Loading())
            when (val result = note.createJSON(true)) {
                is Result.Error -> _uploadState.postValue(State.Empty())
                is Result.Success -> {
                    Session.uploadObservation(UserObservation(note)).apply {
                        onError {
                            _uploadState.postValue(State.Empty())
                        }

                        onSuccess {
                            _uploadState.postValue(State.Items(null))
                            RoomService.notesDao.delete(note)
                            getNotes()
                        }
                    }
                }
            }
        }
    }
}