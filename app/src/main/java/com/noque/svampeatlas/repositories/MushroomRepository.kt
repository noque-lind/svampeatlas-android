package com.noque.svampeatlas.repositories

import androidx.collection.LruCache
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.extensions.toAppError
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication.Companion.applicationContext
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.volleyRequests.AppRequest
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MushroomRepository(private val requestQueue: RequestQueue) {

    private val cache = LruCache<Int, Mushroom>(100)

    suspend fun getMushroom(id: Int): Result<Mushroom, DataService.Error> {
        cache.get(id)?.let {
            return Result.Success(it)
        }

        RoomService.mushrooms.getMushroomWithID(id).onSuccess {
            return Result.Success(it)
        }

        return fetchMushroom(id)
    }

    private suspend fun fetchMushroom(id: Int): Result<Mushroom, DataService.Error> = suspendCoroutine { cont ->
        val request = AppRequest<List<Mushroom>>(object : TypeToken<List<Mushroom>>() {}.type,
            API(APIType.Request.Mushroom(id)),
            null,
            null,
            Response.Listener {
                if (it.firstOrNull() != null) {
                    cache.put(id, it.first())
                    cont.resume(Result.Success(it.first()))
                } else {
                    cont.resume(Result.Error<Mushroom, DataService.Error>(DataService.Error.NotFound(applicationContext)))
                }
            },

            Response.ErrorListener {
                cont.resume(Result.Error<Mushroom, DataService.Error>(it.toAppError()))
            })

        requestQueue.add(request)
    }



}