package com.noque.svampeatlas.repositories

import androidx.collection.LruCache
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.extensions.toAppError
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Prediction
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RecognitionService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.MyApplication.Companion.applicationContext
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.volleyRequests.AppRequest
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MushroomRepository(private val requestQueue: RequestQueue) {

    private val cache = LruCache<Int, Mushroom>(100)

    suspend fun fetchMushrooms(predictionResults: RecognitionService.GetResultsRequestResult): List<Prediction> {
        val predictions = mutableListOf<Prediction>()
        for (index in predictionResults.taxonIds.indices) {
            getMushroom(predictionResults.taxonIds[index]).onSuccess {
                predictions.add(Prediction(it,predictionResults.conf[index]))
            }
        }
        return predictions
    }

    suspend fun getMushroom(id: Int, ignoreLocal: Boolean = false): Result<Mushroom, DataService.Error> {
        fetchFromCache(id)?.let {
            return Result.Success(it)
        }
        if (!ignoreLocal) {
            fetchFromRepository(id)?.let {
                return Result.Success(it)
            }
        }

        return fetch(id)
    }

    private fun fetchFromCache(id: Int): Mushroom? {
        cache.get(id)?.let {
            return if (it.acceptedTaxon != null && it.acceptedTaxon.id != id) fetchFromCache(it.acceptedTaxon.id) else it
        }
        return null
    }

    private suspend fun fetchFromRepository(id: Int): Mushroom? {
        RoomService.mushrooms.getMushroomWithID(id).onSuccess {
            return if (it.acceptedTaxon != null && it.acceptedTaxon.id != id) fetchFromRepository(it.acceptedTaxon.id) else it
        }
    return null
    }

    private suspend fun fetch(id: Int): Result<Mushroom, DataService.Error> {
        return when (val result = fetchMushroom(id)) {
            is Result.Error -> result
            is Result.Success -> {
                if (result.value.acceptedTaxon != null && result.value.acceptedTaxon.id != id) fetchMushroom(result.value.acceptedTaxon.id) else result
            }
        }
    }

    private suspend fun fetchMushroom(id: Int): Result<Mushroom, DataService.Error> = suspendCoroutine { cont ->
        val request = AppRequest<List<Mushroom>>(object : TypeToken<List<Mushroom>>() {}.type,
            API(APIType.Request.Mushroom(id)),
            null,
            null,
            {
                if (it.firstOrNull() != null) {
                    val mushroom = it.first()
                    cache.put(id, mushroom)
                    cont.resume(Result.Success(mushroom))
                } else {
                    cont.resume(Result.Error<Mushroom, DataService.Error>(DataService.Error.NotFound(applicationContext)))
                }
            },
            {
                cont.resume(Result.Error(it.toAppError()))
            })

        requestQueue.add(request)
    }



}