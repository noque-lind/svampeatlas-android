package com.noque.svampeatlas.daos

import android.util.Log
import com.noque.svampeatlas.extensions.AppLanguage
import com.noque.svampeatlas.extensions.appLanguage
import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.services.RoomService
import java.util.*

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

    suspend fun getMushroomsFromSearch(entry: String): Result<List<Mushroom>, RoomService.Error> {
        var genus = ""
        var fullSearchTerm = ""
        var taxonName = ""

        entry.split(" ").toTypedArray().forEach {
            if (it != "") {
                if (fullSearchTerm == "") {
                    fullSearchTerm = it
                    genus = it
                } else {
                    fullSearchTerm += " ${it}"

                    if (taxonName == "") {
                        taxonName = it
                    } else {
                        taxonName += " ${it}"
                    }
                }
            }
        }

        val transformedSearchTerm = "%$fullSearchTerm%"

       val result =  when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> dao.searchMushrooms_dk(transformedSearchTerm, taxonName = taxonName, genus = genus)
            AppLanguage.English -> dao.searchMushrooms_en(transformedSearchTerm, taxonName = taxonName, genus = genus)
            AppLanguage.Czech -> dao.searchMushrooms_cz(transformedSearchTerm, taxonName = taxonName, genus = genus)
        }
        return if (result.isNotEmpty()) {
            Result.Success(result.toList())
        } else {
            Result.Error(RoomService.Error.NoData(RoomService.Error.DataType.MUSHROOM))
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