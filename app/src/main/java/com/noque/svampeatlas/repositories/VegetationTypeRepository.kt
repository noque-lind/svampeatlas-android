package com.noque.svampeatlas.repositories

import com.android.volley.RequestQueue
import com.android.volley.Response
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.extensions.toAppError
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.models.SubstrateGroup
import com.noque.svampeatlas.models.VegetationType
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.volleyRequests.AppRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VegetationTypeRepository(private val requestQueue: RequestQueue) {

    suspend fun getVegetationTypes(tag: String, freshDownload: Boolean = false): Result<List<VegetationType>, DataService.Error> {
        if (!freshDownload) RoomService.vegetationTypes.getAll().onSuccess {
            return Result.Success(it)
        }
        return fetchVegetationTypes(tag).apply {
            onSuccess {
                RoomService.vegetationTypes.save(it)
            }
        }
    }

    private suspend fun fetchVegetationTypes(tag: String): Result<List<VegetationType>, DataService.Error> = suspendCoroutine { cont ->
        val api = API(APIType.Request.VegetationType())
        val request = AppRequest<List<VegetationType>>(
            object : TypeToken<List<VegetationType>>() {}.type,
            api,
            null,
            null,
            {
                cont.resume(Result.Success(it.sortedBy { it.id }))
            },

            {
                cont.resume(Result.Error(it.toAppError()))
            }
        )

        request.tag = tag
        requestQueue.add(request)
    }
}