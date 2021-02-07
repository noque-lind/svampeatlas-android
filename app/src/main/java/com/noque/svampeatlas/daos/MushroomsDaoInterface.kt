package com.noque.svampeatlas.daos

import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.services.RoomService

class MushroomsDaoInterface(private val dao: MushroomsDao) {
    suspend fun saveMushroom(mushroom: Mushroom) {
        dao.saveFavorites(mushroom)
    }

    suspend fun save(mushrooms: List<Mushroom>) {
        dao.saveMushrooms(*mushrooms.toTypedArray())
    }

    suspend fun deleteMushroom(mushroom: Mushroom) {
        dao.deleteMushroom(mushroom)
    }

    suspend fun getFavoritedMushrooms(): Result<List<Mushroom>, RoomService.Error> {
        val mushrooms = dao.getFavorites().toList()
        if (mushrooms.isEmpty()) {
            return Result.Error(RoomService.Error.NoData(RoomService.Error.DataType.FAVORITES))
        } else {
            return Result.Success(mushrooms)
        }
    }

    suspend fun getMushroomWithID(id: Int): Result<Mushroom, RoomService.Error> {
        val mushroom = dao.getMushroom(id)

        if (mushroom == null) {
            return Result.Error(RoomService.Error.NoData(RoomService.Error.DataType.MUSHROOM))
        } else {
            return Result.Success(mushroom)
        }
    }

}