package com.noque.svampeatlas.repositories

import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.extensions.getBitmap
import com.noque.svampeatlas.extensions.rotate
import com.noque.svampeatlas.extensions.toAppError
import com.noque.svampeatlas.extensions.toJPEG
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.DispatchGroup
import com.noque.svampeatlas.utilities.MultipartFormImage
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.volleyRequests.AppEmptyRequest
import com.noque.svampeatlas.utilities.volleyRequests.AppJSONObjectRequest
import com.noque.svampeatlas.utilities.volleyRequests.AppMultipartPost
import com.noque.svampeatlas.utilities.volleyRequests.AppRequest
import com.noque.svampeatlas.view_models.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ObservationsRepository(private val requestQueue: RequestQueue) {

    suspend fun editObservation(id: Int, token: String, jsonObject: JSONObject, imageFiles: List<File>?): Result<Pair<Int, Int>, DataService.Error> {
        return when (val result = putObservation(id, jsonObject, token)) {
            is Result.Success -> Result.Success(Pair(id, postImagesToObservation(id, imageFiles, token)))
            is Result.Error -> Result.Error(result.error)
        }
    }

    suspend fun uploadObservation(tag: String, token: String, jsonObject: JSONObject, imageFiles: List<File>?): Result<Pair<Int, Int>, DataService.Error> {
       return when (val result = postObservation(jsonObject, token)) {
           is Result.Success -> Result.Success(Pair(result.value,postImagesToObservation(result.value, imageFiles, token)))
           is Result.Error -> Result.Error(result.error)
       }
    }

    suspend fun deleteObservation(id: Int, token: String): Result<Void?, DataService.Error> {
        return delete(id, token)
    }

    private suspend fun postImagesToObservation(id: Int, imageFiles: List<File>?, token: String): Int {
        if (imageFiles == null || imageFiles.isEmpty()) return 0
        var completedUploads = 0
        imageFiles.forEach {
            uploadImage(id, it, token)
            completedUploads += 1
        }
        return completedUploads
    }

    private suspend fun uploadImage(
        observationID: Int,
        image: File,
        token: String
    ): Result<Void?, AppError> {
        return when (val bitmapResult = image.getBitmap()) {
            is Result.Error -> {
                Result.Error(bitmapResult.error)
            }
            is Result.Success -> {
                val byteArray = bitmapResult.value.rotate(image).toJPEG(0.6)
                return suspendCoroutine { cont ->
                    val multipartFormImage = MultipartFormImage(byteArray, "file")
                    val request = AppMultipartPost(API(APIType.Post.Image(observationID)),
                        token,
                        multipartFormImage,
                        Response.Listener {
                            cont.resume(Result.Success(null))
                        },
                        Response.ErrorListener {
                            cont.resume((Result.Error(it.toAppError())))
                        }
                    )

                    request.retryPolicy = DefaultRetryPolicy(
                        20000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )
                    requestQueue.add(request)
                }
            }
        }
    }

    private suspend fun putObservation(id: Int, json: JSONObject, token: String): Result<Void?, DataService.Error> = suspendCoroutine { cont ->
        val request = AppJSONObjectRequest(
            API(APIType.Put.Observation(id)),
            token,
            json,
            Response.Listener {
                cont.resume(Result.Success(null))
            },
            Response.ErrorListener {
                cont.resume(Result.Error(it.toAppError()))
            })

        requestQueue.add(request)
    }

    private suspend fun postObservation(json: JSONObject, token: String): Result<Int, DataService.Error> = suspendCoroutine { cont ->
        val request = AppJSONObjectRequest(
            API(APIType.Post.Observation()),
            token,
            json,
            Response.Listener {
                val id = it.getInt("_id")
                cont.resume(Result.Success(id))
            },
            Response.ErrorListener {
                cont.resume(Result.Error(it.toAppError()))
            })

        requestQueue.add(request)
    }

    private suspend fun delete(id: Int, token: String): Result<Void?, DataService.Error> = suspendCoroutine { cont ->
        val request = AppEmptyRequest(
            API(APIType.Delete.Observation(id)),
            token,
            Response.Listener {
                cont.resume(Result.Success(null))
            },
            Response.ErrorListener {
                cont.resume(Result.Error(it.toAppError()))
            })

        requestQueue.add(request)
    }
}