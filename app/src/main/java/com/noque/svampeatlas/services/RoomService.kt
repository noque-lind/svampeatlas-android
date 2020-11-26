package com.noque.svampeatlas.services

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.utilities.MyApplication

object RoomService {

    sealed class Error(title: String, message: String) : AppError(title, message, null) {
        enum class DataType {
            FAVORITES,
            USER,
            SUBSTRATE,
            VEGETATIONTYPE,
            MUSHROOM,
            HOST
        }

        class NoData(resources: Resources, dataType: DataType) : Error(
            resources.getString(R.string.error_database_noEntries_title),
            when (dataType) {
                DataType.FAVORITES -> resources.getString(R.string.error_database_noEntries_favoritedMushrooms_message)
                else -> resources.getString(R.string.error_database_noEntries_message)
            }
        )
    }

    private val database by lazy { Database.buildDatabase(MyApplication.applicationContext)  }
    private val resources by lazy {  MyApplication.applicationContext.resources }


    suspend fun saveUser(user: User) {
        database.UserDao().insert(user)
    }

    suspend fun clearUser() {
        database.UserDao().clear()
    }

    suspend fun getUser(): Result<User, Error> {
        val users = database.UserDao().getUsers()
        return if (users.firstOrNull() != null) Result.Success(users.first()) else Result.Error(
            Error.NoData(resources, Error.DataType.USER)
        )
    }

    suspend fun saveSubstrate(substrate: Substrate) {
        database.SubstratesDao().save(substrate)
    }

    suspend fun getSubstrateWithID(id: Int): Result<Substrate, Error> {
        val substrate = database.SubstratesDao().getSubstrateWithID(id)
        return if (substrate != null) Result.Success(substrate) else Result.Error(Error.NoData(resources, Error.DataType.SUBSTRATE))
    }

    fun getSubstrateWithIDNow(id: Int): Result<Substrate, Error> {
        val substrate = database.SubstratesDao().getSubstrateWithIDNow(id)
        return if (substrate != null) Result.Success(substrate) else Result.Error(Error.NoData(resources, Error.DataType.SUBSTRATE))
    }

    suspend fun saveVegetationType(vegetationType: VegetationType) {
        database.VegetationTypeDao().save(vegetationType)
    }

    suspend fun getVegetationTypeWithID(id: Int): Result<VegetationType, Error> {
        val vegetationType = database.VegetationTypeDao().getVegetationTypeWithID(id)
        return if (vegetationType != null) Result.Success(vegetationType) else Result.Error(Error.NoData(resources, Error.DataType.VEGETATIONTYPE))
    }

    fun getVegetationTypewithIDNow(id: Int): Result<VegetationType, Error> {
        val vegetationType = database.VegetationTypeDao().getVegetationTypeWithIDNow(id)
        return if (vegetationType != null) Result.Success(vegetationType) else Result.Error(Error.NoData(resources, Error.DataType.VEGETATIONTYPE))
    }

    suspend fun saveHosts(hosts: List<Host>) {
        database.HostsDao().save(*hosts.toTypedArray())
    }

    suspend fun getHostsWithIds(ids: List<Int>): Result<List<Host>, Error> {
        var hosts = mutableListOf<Host>()

        ids.forEach {
            database.HostsDao().getHostWithID(it)?.let {
                hosts.add(it)
            }
        }
        return if (hosts.isNotEmpty()) Result.Success(hosts) else Result.Error(Error.NoData(resources, Error.DataType.HOST))
    }

    suspend fun saveMushroom(mushroom: Mushroom) {
        database.mushroomsDao().saveFavorites(mushroom)

    }

    suspend fun deleteMushroom(mushroom: Mushroom) {
        database.mushroomsDao().deleteMushroom(mushroom)
    }

    suspend fun getFavoritedMushrooms(): Result<List<Mushroom>, Error> {
        val mushrooms = database.mushroomsDao().getFavorites().toList()
        if (mushrooms.isEmpty()) {
            return Result.Error(Error.NoData(resources, Error.DataType.FAVORITES))
        } else {
            return Result.Success(mushrooms)
        }
    }

    suspend fun getMushroomWithID(id: Int): Result<Mushroom, Error> {
        val mushroom = database.mushroomsDao().getMushroom(id)

        if (mushroom == null) {
            return Result.Error(Error.NoData(resources, Error.DataType.MUSHROOM))
        } else {
            return Result.Success(mushroom)
        }
    }


}