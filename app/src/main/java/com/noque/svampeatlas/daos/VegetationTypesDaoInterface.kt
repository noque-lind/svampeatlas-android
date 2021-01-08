package com.noque.svampeatlas.daos

import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.models.VegetationType
import com.noque.svampeatlas.services.RoomService

class VegetationTypesDaoInterface(private val dao: VegetationTypeDao) {
    suspend fun save(vegetationTypes: List<VegetationType>) {
        dao.save(*vegetationTypes.toTypedArray())
    }

    suspend fun saveVegetationType(vegetationType: VegetationType) {
        dao.save(vegetationType)
    }

    suspend fun getAll(): Result<List<VegetationType>, RoomService.Error> {
        dao.getVegetationTypes().also {
            return if (it.isEmpty()) {
                Result.Error(RoomService.Error.NoData(RoomService.Error.DataType.VEGETATIONTYPE))
            } else {
                Result.Success(it.toList())
            }
        }
    }

    suspend fun getVegetationTypeWithID(id: Int): Result<VegetationType, RoomService.Error> {
        val vegetationType = dao.getVegetationTypeWithID(id)
        return if (vegetationType != null) Result.Success(vegetationType) else Result.Error(
            RoomService.Error.NoData(RoomService.Error.DataType.VEGETATIONTYPE))
    }

    fun getVegetationTypewithIDNow(id: Int): Result<VegetationType, RoomService.Error> {
        val vegetationType = dao.getVegetationTypeWithIDNow(id)
        return if (vegetationType != null) Result.Success(vegetationType) else Result.Error(
            RoomService.Error.NoData(RoomService.Error.DataType.VEGETATIONTYPE))
    }
}