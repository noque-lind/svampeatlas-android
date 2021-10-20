package com.noque.svampeatlas.daos

import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.models.Substrate
import com.noque.svampeatlas.services.RoomService

class SubstratesDaoInterface(private val dao: SubstratesDao) {
    suspend fun saveSubstrate(substrate: Substrate) {
        dao.save(substrate)
    }

    suspend fun save(substrates: List<Substrate>) {
        dao.save(*substrates.toTypedArray())
    }

    fun getSubstrateWithID(id: Int): Result<Substrate, RoomService.Error> {
        val substrate = dao.getSubstrateWithID(id)
        return if (substrate != null) Result.Success(substrate) else Result.Error(
            RoomService.Error.NoData(RoomService.Error.DataType.SUBSTRATE))
    }

    fun getSubstrateWithIDNow(id: Int): Result<Substrate, RoomService.Error> {
        val substrate = dao.getSubstrateWithIDNow(id)
        return if (substrate != null) Result.Success(substrate) else Result.Error(
            RoomService.Error.NoData(
                RoomService.Error.DataType.SUBSTRATE))
    }

    suspend fun getSubstrates(): Result<List<Substrate>, RoomService.Error> {
        dao.getSubstrates().also {
            return if (it.isEmpty()) {
                Result.Error(RoomService.Error.NoData(RoomService.Error.DataType.SUBSTRATE))
            } else {
                Result.Success(it.toList())
            }
        }
    }
}