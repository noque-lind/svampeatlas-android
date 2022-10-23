package com.noque.svampeatlas.view_models

import android.app.backup.SharedPreferencesBackupHelper
import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.logrocket.core.SDK
import com.noque.svampeatlas.R
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
import java.sql.Date
import java.util.concurrent.TimeUnit
import kotlin.coroutines.suspendCoroutine

object Session {

    sealed class Error(title: String, message: String, recoveryAction: RecoveryAction?) :
        AppError(title, message, recoveryAction) {
        class NewObservationError(resources: Resources, message: String): Error(resources.getString(R.string.newObservationError_missingInformation), message, null)
        class IsNotLoggedinError(resources: Resources): Error(resources.getString(R.string.dataServiceError_loginError_title), resources.getString(R.string.dataServiceError_loginError_message), null)
    }

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
        this.token = SharedPreferences.token
        evaluateLoginState(this.token)
    }

    private fun evaluateLoginState(token: String?) {
        if (token != null) {
            getUser(token)
        } else {
            setLoggedInState(null, null)
        }
    }

    private fun getUser(token: String) {
        GlobalScope.launch {
            val result = RoomService.users.getUser()

            result.onError {
                    DataService.getInstance(MyApplication.applicationContext).getUser(TAG, token) {
                        it.onSuccess {
                            GlobalScope.launch {
                                RoomService.users.saveUser(it)
                            }
                            setLoggedInState(token, it)
                            getNotifications(token)
                            getObservations(it)
                        }

                        it.onError {
                           setLoggedInState(null, null)
                        }
                    }
                }
            result.onSuccess {
               setLoggedInState(token, it)
                getNotifications(token)
                getObservations(it)
                downloadUser(token)
            }
        }
    }

    fun downloadUser(token: String) {
        DataService.getInstance(MyApplication.applicationContext).getUser(TAG, token) {
            it.onSuccess {
                GlobalScope.launch {
                    RoomService.users.saveUser(it)
                }
            }
        }
    }


    private fun setLoggedInState(token: String?, user: User?) {
        SharedPreferences.token = token
        this.token = token
        this._user.postValue(user)

        if (token == null || user == null) {
            SDK.identify("Anonymous")
            if (_loggedInState.value?.item != false) _loggedInState.postValue(State.Items(false))
        } else {
            SDK.identify(user.initials)
            if (_loggedInState.value?.item != true) _loggedInState.postValue(State.Items(true))
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
                SharedPreferences.token = it
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

    suspend fun deleteObservation(id: Int): Result<Void?, AppError> {
        val token = token
        val user = user.value
        if (token == null || user == null) return Result.Error(Error.IsNotLoggedinError(MyApplication.resources))
        return DataService.getInstance(MyApplication.applicationContext).observationsRepository.deleteObservation(id, token)
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

    suspend fun editObservation(id: Int, userObservation: UserObservation): Result<Pair<Int, Int>, AppError> {
        val token = token
        val user = user.value
        val imageFiles = userObservation.getImagesForUpload()
        if (token == null || user == null) return Result.Error(Error.IsNotLoggedinError(MyApplication.resources))
        val json = userObservation.asJSON(false)
            ?: return Result.Error(Error.NewObservationError(MyApplication.resources, "JSON was empty"))
        return DataService.getInstance(MyApplication.applicationContext).observationsRepository.editObservation(id, token, json, imageFiles).also {
            lastUpdated = Date(0)
            userObservation.deleteAllImages()
        }
    }

    suspend fun uploadObservation(userObservation: UserObservation): Result<Pair<Int, Int>, AppError> {
        val token = token
        val user = user.value
        if (token == null || user == null) return Result.Error(Error.IsNotLoggedinError(MyApplication.resources))
        val json = userObservation.asJSON(true)
            ?: return Result.Error(Error.NewObservationError(MyApplication.resources, "JSON object was nil"))
        val imageFiles = userObservation.getImagesForUpload()
        json.optJSONObject("determination")?.put("user_id", user.id)
        json.put("users", JSONArray().also { usersArray ->
            listOf(user).forEach {
                usersArray.put(
                    JSONObject()
                        .put("_id", it.id)
                        .put("Initialer", it.initials)
                        .put("email", it.email)
                        .put("facebook", it.facebookID ?: "")
                        .put("name", it.name)
                )
            }
        })

        if (json.optJSONArray("users")?.length() == 0) return Result.Error(Error.NewObservationError(MyApplication.resources, "Users array was empty, please contact app@noque.dk"))

        return DataService.getInstance(MyApplication.applicationContext).observationsRepository.uploadObservation(
            TAG,
            token,
            json,
            imageFiles).also {
            lastUpdated = Date(0)
            userObservation.deleteAllImages()
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
        SharedPreferences.token = null
        GlobalScope.launch {
            RoomService.users.clearUser()
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