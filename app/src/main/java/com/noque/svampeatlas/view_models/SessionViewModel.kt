package com.noque.svampeatlas.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.SharedPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

object Session {
    const val TAG = "SessionViewModel"

    private var token: String?
    private var notificationsCount = 0
    private var observationsCount = 0

    private val _loggedInState by lazy { MutableLiveData<State<Boolean>>() }
    private val _user by lazy { MutableLiveData<User?>() }

    private var lastUpdated = Date(System.currentTimeMillis())
    private val _notificationsState by lazy { MutableLiveData<State<Pair<List<Notification>, Int>>>() }
    private val _observationsState by lazy { MutableLiveData<State<Pair<List<Observation>, Int>>>() }

    private val _commentUploadState by lazy { MutableLiveData<State<Comment>>() }
    private val _observationUploadState by lazy { MutableLiveData<State<Pair<Int, Int>>>() }

    val isLoggedIn: Boolean
        get() {
            return if (loggedInState.value is State.Items) (loggedInState.value as State.Items).items else false
        }

    val user: LiveData<User?> get() = _user
    val loggedInState: LiveData<State<Boolean>> get() = _loggedInState

    val notificationsState: LiveData<State<Pair<List<Notification>, Int>>> get() = _notificationsState
    val observationsState: LiveData<State<Pair<List<Observation>, Int>>> get() = _observationsState

    val commentUploadState: LiveData<State<Comment>> get() = _commentUploadState
    val observationUploadState: LiveData<State<Pair<Int, Int>>> get() = _observationUploadState

    init {
        this.token = SharedPreferences.getToken()
        evaluateLoginState(this.token)
    }

    private fun evaluateLoginState(token: String?) {
        if (token != null) {
            getUser(token)
        } else {
            _loggedInState.value = State.Items(false)
            _user.value = null
        }
    }

    fun getUser(token: String) {
        GlobalScope.launch {
            val result = RoomService.getUser()

            result.onError {
                    DataService.getInstance(MyApplication.applicationContext).getUser(TAG, token) {
                        it.onSuccess {
                            GlobalScope.launch {
                                RoomService.saveUser(it)
                            }

                            _user.postValue(it)
                            _loggedInState.postValue(State.Items(true))
                            getNotifications(token)
                            getObservations(it)
                        }

                        it.onError {
                            _loggedInState.postValue(State.Items(false))
                            _user.postValue(null)
                        }
                    }
                }
            result.onSuccess {
                _user.postValue(it)
                _loggedInState.postValue(State.Items(true))
                getNotifications(token)
                getObservations(it)
            }
        }
    }

    private fun getNotifications(token: String) {
        _notificationsState.postValue(State.Loading())

        DataService.getInstance(MyApplication.applicationContext).getUserNotificationCount(TAG, token) {
            it.onSuccess {
                notificationsCount = it
                lastUpdated = Date(System.currentTimeMillis())
                DataService.getInstance(MyApplication.applicationContext)
                    .getNotifications(TAG, token, if (it >= 8) 8 else it, 0) {
                        it.onSuccess {
                            _notificationsState.postValue(State.Items(Pair(it, notificationsCount)))
                        }

                        it.onError {
                            _notificationsState.postValue(State.Error(it))
                        }
                    }
            }

            it.onError {
                _notificationsState.postValue(State.Error(it))
            }
        }
    }

    private fun getObservations(user: User) {
        _observationsState.postValue(State.Loading())

        DataService.getInstance(MyApplication.applicationContext).getObservationCountForUser(TAG, user.id) {
            it.onSuccess {
                lastUpdated = Date(System.currentTimeMillis())
                observationsCount = it
                DataService.getInstance(MyApplication.applicationContext)
                    .getObservationsForUser(TAG, user.id, 0, 16) {
                        it.onSuccess {
                            _observationsState.postValue(State.Items(Pair(it, observationsCount)))
                        }

                        it.onError {
                            _observationsState.postValue(State.Error(it))
                        }
                    }
            }

            it.onError {
                _observationsState.postValue(State.Error(it))
            }
        }
    }

    fun login(initials: String, password: String) {
        DataService.getInstance(MyApplication.applicationContext).login(initials, password) {
            it.onError {
                _loggedInState.value = State.Error(it)
            }

            it.onSuccess {
                token = it
                SharedPreferences.saveToken(it)
                getUser(it)
            }
        }
    }


    fun getAdditionalNotifications(offset: Int) {
        token?.let {
            _notificationsState.value = State.Loading()

            DataService.getInstance(MyApplication.applicationContext).getNotifications(
                TAG,
                it,
                if (offset + 8 <= notificationsCount) offset + 8 else notificationsCount,
                0
            ) {

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
            _observationsState.value = State.Loading()

            DataService.getInstance(MyApplication.applicationContext)
                .getObservationsForUser(TAG, it.id, 0, offset + 16) {
                    it.onSuccess {
                        _observationsState.value = State.Items(Pair(it, observationsCount))
                    }

                    it.onError {
                        _observationsState.value = State.Error(it)
                    }
                }
        }
    }

    fun reloadData(isNeededNow: Boolean) {
        if (isNeededNow) {
            DataService.getInstance(MyApplication.applicationContext).clearRequestsWithTag(TAG)

            token?.let { getNotifications(it) }
            user.value?.let { getObservations(it) }
        } else {
            val diff = Date(System.currentTimeMillis()).time - (lastUpdated.time)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            Log.d(TAG, "Data is $hours old")

            if (hours > 0) {
                DataService.getInstance(MyApplication.applicationContext).clearRequestsWithTag(TAG)

                token?.let { getNotifications(it) }
                user.value?.let { getObservations(it) }
            }
        }
    }

    fun uploadComment(observationID: Int, comment: String) {
        token?.let {
            _commentUploadState.value = State.Loading()

            DataService.getInstance(MyApplication.applicationContext).postComment(observationID, comment, it) {
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

    fun deleteImage(
        id: Int,
        completion: (Result<Void?, DataService.Error>) -> Unit
        ) {
        token?.let {
            GlobalScope.launch {
                DataService.getInstance(MyApplication.applicationContext).deleteImage(
                    TAG,
                    id,
                    it,
                    completion
                )
            }
        }
    }

    fun deleteObservation(id: Int, completion: (Result<Void?, DataService.Error>) -> Unit) {
        token?.let {
            GlobalScope.launch {
                DataService.getInstance(MyApplication.applicationContext).deleteObservation(
                    TAG,
                    id,
                    it,
                    completion
                )
            }
        }
    }

    fun markNotificationAsRead(notification: Notification) {
        token?.let {
            GlobalScope.launch {
                DataService.getInstance(MyApplication.applicationContext)
                    .markNotificationAsRead(TAG, notification.observationID, it)
                notificationsState.value?.let {
                    (it as? State.Items)?.items?.let {
                        val notifications = it.first.toMutableList()
                        notifications.removeAll { it.observationID == notification.observationID }
                        notificationsCount -= it.first.count() - notifications.count()
                        _notificationsState.postValue(State.Items(Pair(notifications, notificationsCount)))
                    }
                }
            }
        }
    }

    fun uploadObservation(jsonObject: JSONObject, imageFiles: List<File>?) {
        val token = token
        val user = user.value

        if (token != null && user != null) {
            _observationUploadState.value = State.Loading()

            val usersArray = JSONArray()
            listOf(user).forEach {
                usersArray.put(
                    JSONObject()
                        .put("_id", it.id)
                        .put("Initialer", it.initials)
                        .put("facebook", it.facebookID)
                        .put("name", it.name)
                )
            }
            jsonObject.put("users", usersArray)

            DataService.getInstance(MyApplication.applicationContext)
                .uploadObservation(TAG, token, jsonObject) {
                    it.onError {
                        _observationUploadState.value = State.Error(it)

                        // Reset state after it has been posted once
                        _observationUploadState.value = State.Empty()
                    }

                    it.onSuccess { id ->
                        if (imageFiles != null && imageFiles.isNotEmpty()) {
                            GlobalScope.launch {
                                DataService.getInstance(MyApplication.applicationContext)
                                    .uploadImages(TAG, id, imageFiles, token) {
                                        it.onSuccess {
                                            _observationUploadState.value =
                                                State.Items(Pair(id, it))
                                            // Reset state after it has been posted once
                                            _observationUploadState.value = State.Empty()
                                        }
                                    }
                            }
                        } else {
                            _observationUploadState.value = State.Items(Pair(id, 0))
                            // Reset state after it has been posted once
                            _observationUploadState.value = State.Empty()
                        }
                        lastUpdated = Date(0)
                    }
                }
        }
    }

    fun editObservation(id: Int, jsonObject: JSONObject, imageFiles: List<File>?) {
        val token = token
        val user = user.value

        if (token != null && user != null) {
            _observationUploadState.value = State.Loading()

            val usersArray = JSONArray()
            listOf(user).forEach {
                usersArray.put(
                    JSONObject()
                        .put("_id", it.id)
                        .put("Initialer", it.initials)
                        .put("facebook", it.facebookID)
                        .put("name", it.name)
                )
            }
            jsonObject.put("users", usersArray)

            DataService.getInstance(MyApplication.applicationContext)
                .editObservation(TAG, id, token, jsonObject) {
                    it.onError {
                        _observationUploadState.value = State.Error(it)

                        // Reset state after it has been posted once
                        _observationUploadState.value = State.Empty()
                    }

                    it.onSuccess { id ->
                        if (imageFiles != null && imageFiles.isNotEmpty()) {
                            GlobalScope.launch {
                                DataService.getInstance(MyApplication.applicationContext)
                                    .uploadImages(TAG, id, imageFiles, token) {
                                        it.onSuccess {
                                            _observationUploadState.value =
                                                State.Items(Pair(id, it))
                                            // Reset state after it has been posted once
                                            _observationUploadState.value = State.Empty()
                                        }
                                    }
                            }
                        } else {
                            _observationUploadState.value = State.Items(Pair(id, 0))
                            // Reset state after it has been posted once
                            _observationUploadState.value = State.Empty()
                        }
                        lastUpdated = Date(0)
                    }
                }
    }
    }


    fun postOffensiveContentComment(observationID: Int, comment: String?) {
        val json = JSONObject()
        json.put("message", comment ?: "")
        token?.let {
            DataService.getInstance(MyApplication.applicationContext).postOffensiveComment(TAG, observationID, json, it)
        }
    }

    fun logout() {
        SharedPreferences.removeToken()
        GlobalScope.launch {
            RoomService.clearUser()
        }

        token = null
        _user.value = null
        _loggedInState.value = State.Items(false)

        notificationsCount = 0
        observationsCount = 0
        _notificationsState.value = State.Empty()
        _observationsState.value = State.Empty()
    }

}

//class SessionViewModel(application: Application) : AndroidViewModel(application) {
//
//    companion object {
//        val TAG = "SessionViewModel"
//    }
//
//    private var token: String?
//    private var notificationsCount = 0
//    private var observationsCount = 0
//
//    private val _loggedInState by lazy { MutableLiveData<State<Boolean>>() }
//    private val _user by lazy { MutableLiveData<User?>() }
//
//    private var lastUpdated = Date(System.currentTimeMillis())
//    private val _notificationsState by lazy { MutableLiveData<State<Pair<List<Notification>, Int>>>() }
//    private val _observationsState by lazy { MutableLiveData<State<Pair<List<Observation>, Int>>>() }
//
//    private val _commentUploadState by lazy { MutableLiveData<State<Comment>>() }
//    private val _observationUploadState by lazy { MutableLiveData<State<Pair<Int, Int>>>() }
//
//    val isLoggedIn: Boolean
//        get() {
//            return if (loggedInState.value is State.Items) (loggedInState.value as State.Items).items else false
//        }
//
//    val user: LiveData<User?> get() = _user
//    val loggedInState: LiveData<State<Boolean>> get() = _loggedInState
//
//    val notificationsState: LiveData<State<Pair<List<Notification>, Int>>> get() = _notificationsState
//    val observationsState: LiveData<State<Pair<List<Observation>, Int>>> get() = _observationsState
//
//    val commentUploadState: LiveData<State<Comment>> get() = _commentUploadState
//    val observationUploadState: LiveData<State<Pair<Int, Int>>> get() = _observationUploadState
//
//    init {
//        this.token = SharedPreferences.getToken()
//        evaluateLoginState(this.token)
//    }
//
//    private fun evaluateLoginState(token: String?) {
//        if (token != null) {
//            getUser(token)
//        } else {
//            _loggedInState.value = State.Items(false)
//            _user.value = null
//        }
//    }
//
//    private fun getUser(token: String) {
//        viewModelScope.launch {
//            val result = RoomService.getUser()
//
//            result.onError {
//
//                DataService.getInstance(MyApplication.applicationContext).getUser(TAG, token) {
//                    it.onSuccess {
//                        viewModelScope.launch {
//                            RoomService.saveUser(it)
//                        }
//
//                        _user.value = it
//                        _loggedInState.value = State.Items(true)
//                        getNotifications(token)
//                        getObservations(it)
//                    }
//
//                    it.onError {
//                        _loggedInState.value = State.Items(false)
//                        _user.value = null
//                    }
//                }
//
//            }
//
//            result.onSuccess {
//                _user.value = it
//                _loggedInState.value = State.Items(true)
//                getNotifications(token)
//                getObservations(it)
//            }
//        }
//    }
//
//    private fun getNotifications(token: String) {
//        _notificationsState.value = State.Loading()
//
//        DataService.getInstance(MyApplication.applicationContext).getUserNotificationCount(TAG, token) {
//            it.onSuccess {
//                notificationsCount = it
//                lastUpdated = Date(System.currentTimeMillis())
//                DataService.getInstance(MyApplication.applicationContext)
//                    .getNotifications(TAG, token, if (it >= 8) 8 else it, 0) {
//                        it.onSuccess {
//                            _notificationsState.value = State.Items(Pair(it, notificationsCount))
//                        }
//
//                        it.onError {
//                            _notificationsState.value = State.Error(it)
//                        }
//                    }
//            }
//
//            it.onError {
//                _notificationsState.value = State.Error(it)
//            }
//        }
//    }
//
//    private fun getObservations(user: User) {
//        _observationsState.value = State.Loading()
//
//        DataService.getInstance(MyApplication.applicationContext).getObservationCountForUser(TAG, user.id) {
//            it.onSuccess {
//                lastUpdated = Date(System.currentTimeMillis())
//                observationsCount = it
//                DataService.getInstance(MyApplication.applicationContext)
//                    .getObservationsForUser(TAG, user.id, 0, 16) {
//                        it.onSuccess {
//                            _observationsState.value = State.Items(Pair(it, observationsCount))
//                        }
//
//                        it.onError {
//                            _observationsState.value = State.Error(it)
//                        }
//                    }
//            }
//
//            it.onError {
//                _observationsState.value = State.Error(it)
//            }
//        }
//    }
//
//    fun login(initials: String, password: String) {
//        DataService.getInstance(MyApplication.applicationContext).login(initials, password) {
//            it.onError {
//                _loggedInState.value = State.Error(it)
//            }
//
//            it.onSuccess {
//                token = it
//                SharedPreferences.saveToken(it)
//                getUser(it)
//            }
//        }
//    }
//
//
//    fun getAdditionalNotifications(offset: Int) {
//        token?.let {
//            _notificationsState.value = State.Loading()
//
//            DataService.getInstance(MyApplication.applicationContext).getNotifications(
//                TAG,
//                it,
//                if (offset + 8 <= notificationsCount) offset + 8 else notificationsCount,
//                0
//            ) {
//
//                it.onSuccess {
//                    _notificationsState.value = State.Items(Pair(it, notificationsCount))
//                }
//
//                it.onError {
//                    _notificationsState.value = State.Error(it)
//                }
//            }
//        }
//    }
//
//    fun getAdditionalObservations(offset: Int) {
//        user.value?.let {
//            _observationsState.value = State.Loading()
//
//            DataService.getInstance(MyApplication.applicationContext)
//                .getObservationsForUser(TAG, it.id, 0, offset + 16) {
//                    it.onSuccess {
//                        _observationsState.value = State.Items(Pair(it, observationsCount))
//                    }
//
//                    it.onError {
//                        _observationsState.value = State.Error(it)
//                    }
//                }
//        }
//    }
//
//    fun reloadData(isNeededNow: Boolean) {
//        if (isNeededNow) {
//            DataService.getInstance(MyApplication.applicationContext).clearRequestsWithTag(TAG)
//
//            token?.let { getNotifications(it) }
//            user.value?.let { getObservations(it) }
//        } else {
//            val diff = Date(System.currentTimeMillis()).time - (lastUpdated.time)
//            val hours = TimeUnit.MILLISECONDS.toHours(diff)
//            Log.d(TAG, "Data is $hours old")
//
//            if (hours > 0) {
//                DataService.getInstance(MyApplication.applicationContext).clearRequestsWithTag(TAG)
//
//                token?.let { getNotifications(it) }
//                user.value?.let { getObservations(it) }
//            }
//        }
//    }
//
//    fun uploadComment(observationID: Int, comment: String) {
//        token?.let {
//            _commentUploadState.value = State.Loading()
//
//            DataService.getInstance(MyApplication.applicationContext).postComment(observationID, comment, it) {
//                it.onError {
//                    _commentUploadState.value = State.Error(it)
//                }
//
//                it.onSuccess {
//                    _commentUploadState.value = State.Items(it)
//                }
//
//                _commentUploadState.value = State.Empty()
//            }
//        }
//    }
//
//    fun markNotificationAsRead(notification: Notification) {
//        token?.let {
//            viewModelScope.launch {
//                DataService.getInstance(MyApplication.applicationContext)
//                    .markNotificationAsRead(TAG, notification.observationID, it)
//                notificationsState.value?.let {
//                    (it as? State.Items)?.items?.let {
//                        val notifications = it.first.toMutableList()
//                        notifications.removeAll { it.observationID == notification.observationID }
//                        notificationsCount -= it.first.count() - notifications.count()
//                        _notificationsState.value =
//                            State.Items(Pair(notifications, notificationsCount))
//                    }
//                }
//            }
//        }
//    }
//
//    fun uploadObservation(jsonObject: JSONObject, imageFiles: List<File>?) {
//        val token = token
//        val user = user.value
//
//        if (token != null && user != null) {
//            _observationUploadState.value = State.Loading()
//
//            val usersArray = JSONArray()
//            listOf(user).forEach {
//                usersArray.put(
//                    JSONObject()
//                        .put("_id", it.id)
//                        .put("Initialer", it.initials)
//                        .put("facebook", it.facebookID)
//                        .put("name", it.name)
//                )
//            }
//            jsonObject.put("users", usersArray)
//
//            DataService.getInstance(MyApplication.applicationContext)
//                .uploadObservation(TAG, token, jsonObject) {
//                    it.onError {
//                        _observationUploadState.value = State.Error(it)
//
//                        // Reset state after it has been posted once
//                        _observationUploadState.value = State.Empty()
//                    }
//
//                    it.onSuccess { id ->
//                        if (imageFiles != null && imageFiles.isNotEmpty()) {
//                            viewModelScope.launch {
//                                DataService.getInstance(MyApplication.applicationContext)
//                                    .uploadImages(TAG, id, imageFiles, token) {
//                                        it.onSuccess {
//                                            _observationUploadState.value =
//                                                State.Items(Pair(id, it))
//                                            // Reset state after it has been posted once
//                                            _observationUploadState.value = State.Empty()
//                                        }
//                                    }
//                            }
//                        } else {
//                            _observationUploadState.value = State.Items(Pair(id, 0))
//                            // Reset state after it has been posted once
//                            _observationUploadState.value = State.Empty()
//                        }
//                        lastUpdated = Date(0)
//                    }
//                }
//        }
//    }
//
//    fun editObservation(id: Int) {
//        val token = token
//
//    }
//
//    fun postOffensiveContentComment(observationID: Int, comment: String?) {
//        val json = JSONObject()
//        json.put("message", comment ?: "")
//        token?.let {
//            DataService.getInstance(MyApplication.applicationContext).postOffensiveComment(TAG, observationID, json, it)
//        }
//    }
//
//    fun logout() {
//       SharedPreferences.removeToken()
//        viewModelScope.launch {
//            RoomService.clearUser()
//        }
//
//        token = null
//        _user.value = null
//        _loggedInState.value = State.Items(false)
//
//        notificationsCount = 0
//        observationsCount = 0
//        _notificationsState.value = State.Empty()
//        _observationsState.value = State.Empty()
//    }
//
//    override fun onCleared() {
//        Log.d(TAG, "On cleared")
//        super.onCleared()
//    }
//
//}