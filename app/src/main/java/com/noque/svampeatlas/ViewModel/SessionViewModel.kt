package com.noque.svampeatlas.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.Model.*
import com.noque.svampeatlas.Services.DataService
import com.noque.svampeatlas.Utilities.SharedPreferencesHelper

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = "SessionViewModel"
    }


    private var token: String?

    private val _user by lazy { MutableLiveData<User?>() }
    private val _loggedInState by lazy { MutableLiveData<State<Boolean>>() }
    private val _notificationsState by lazy { MutableLiveData<State<Pair<List<Notification>, Int>>>() }
    private val _observationsState by lazy { MutableLiveData<State<Pair<List<Observation>, Int>>>() }

    private val _commentUploadState by lazy { MutableLiveData<State<Comment>>() }

    private var notificationsCount = 0
    private var observationsCount = 0


    val isLoggedIn: Boolean get() {
        val value = loggedInState.value
        when (value) {
            is State.Items -> { return value.items }
            else -> { return false }
        }
    }

    val user: LiveData<User?> get() = _user
    val loggedInState: LiveData<State<Boolean>> get() = _loggedInState
    val notificationsState: LiveData<State<Pair<List<Notification>, Int>>> get() = _notificationsState
    val observationsState: LiveData<State<Pair<List<Observation>, Int>>> get() = _observationsState

    val commentUploadState: LiveData<State<Comment>> get() = _commentUploadState

    init {
        val token = SharedPreferencesHelper(application).getToken()

        if (token != null) {
            getUser(token)
        } else {
            _loggedInState.value = State.Items(false)
            _user.value = null
        }

        this.token = token
    }

    fun login(initials: String, password: String) {
        DataService.getInstance(getApplication()).login(initials, password) {
            it.onError {
                _loggedInState.value = State.Error(it)
            }

            it.onSuccess {
                token = it
                SharedPreferencesHelper(getApplication()).saveToken(it)
                getUser(it)
            }
        }
    }



    private fun getUser(token: String) {
        DataService.getInstance(getApplication()).getUser(token) {
            it.onSuccess {
                _user.value = it
                _loggedInState.value = State.Items(true)
                getNotifications(token)
                getObservations(it)
            }

            it.onError {
                _loggedInState.value = State.Items(false)
                _user.value = null
            }
        }
    }

    private fun getNotifications(token: String) {
            DataService.getInstance(getApplication()).getUserNotificationCount(token) {
                it.onSuccess {
                    notificationsCount = it
                    DataService.getInstance(getApplication()).getNotifications(token, 8, 0) {

                        it.onSuccess {
                            _notificationsState.value = State.Items(Pair(it, notificationsCount))
                        }

                        it.onError {
                            _notificationsState.value = State.Error(it)
                        }
                    }
                }

                it.onError {
                    _notificationsState.value = State.Error(it)
                }
            }
    }

    private fun getObservations(user: User) {
            DataService.getInstance(getApplication()).getObservationCountForUser(user.id) {
                it.onSuccess {
                    observationsCount = it
                    DataService.getInstance(getApplication()).getObservationsForUser(user.id, 0, 16) {

                        it.onSuccess {
                            _observationsState.value = State.Items(Pair(it, observationsCount))
                        }

                        it.onError {
                            _observationsState.value = State.Error(it)
                        }
                    }
                }

                it.onError {
                    _observationsState.value = State.Error(it)
                }
            }
    }

    fun getAdditionalNotifications(offset: Int) {
        token?.let {
            DataService.getInstance(getApplication()).getNotifications(it, 8 + offset, 0) {

                it.onSuccess {
                    _notificationsState.value = State.Items(Pair(it, notificationsCount))
                }

                it.onError {
                    _notificationsState.value = State.Error(it)
                }
            }
        }
    }

    fun getAdditionalObservations(offset: Int) {
        user.value?.let {
            DataService.getInstance(getApplication()).getObservationsForUser(it.id, 0, offset + 16) {
                it.onSuccess {
                    _observationsState.value = State.Items(Pair(it, observationsCount))
                }

                it.onError {
                    _observationsState.value = State.Error(it)
                }
            }
        }
    }

    fun uploadComment(observationID: Int, comment: String) {
        token?.let {
            _commentUploadState.value = State.Loading()

            DataService.getInstance(getApplication()).postComment(observationID, comment, it) {
                it.onError {
                    _commentUploadState.value = State.Error(it)
                }

                it.onSuccess {
                    _commentUploadState.value = State.Items(it)
                }

                _commentUploadState.value = State.Empty()
            }
        }
    }


    fun logout() {
        SharedPreferencesHelper(getApplication()).removeToken()
        token = null
        _user.value = null
        _loggedInState.value = State.Items(false)

        notificationsCount = 0
        observationsCount = 0
        _notificationsState.value = State.Empty()
        _observationsState.value = State.Empty()
    }

    override fun onCleared() {
        Log.d(TAG, "On cleared")
        super.onCleared()
    }

}