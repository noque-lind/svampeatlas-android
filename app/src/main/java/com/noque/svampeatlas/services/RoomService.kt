package com.noque.svampeatlas.services

import android.content.Context
import com.noque.svampeatlas.models.*

class RoomService(context: Context) {

    sealed class Error(title: String, message: String) : AppError(title, message) {
        class NoData() : Error("", "")
    }

    companion object {
        @Volatile
        private var INSTANCE: RoomService? = null
        val TAG = "RoomService"

        fun getInstance(context: Context): RoomService {
            return INSTANCE ?: synchronized(this) {
                return RoomService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var database: Database

    init {
        database = Database.buildDatabase(context)
    }


    suspend fun saveUser(user: User) {
        database.UserDao().insert(user)
    }

    suspend fun clearUser() {
            database.UserDao().clear()
    }

    suspend fun getUser(): Result<User, Error> {
        val users = database.UserDao().getUsers()
        return if (users.firstOrNull() != null) Result.Success(users.first()) else Result.Error(
            Error.NoData()
        )
    }

    suspend fun saveSubstrate(substrate: Substrate) {
        database.SubstratesDao().save(substrate)
    }

    suspend fun getSubstrateWithID(id: Int): Result<Substrate, Error> {
        val substrate =  database.SubstratesDao().getSubstrateWithID(id)
        return if (substrate != null) Result.Success(substrate) else Result.Error(Error.NoData())
    }

    suspend fun saveVegetationType(vegetationType: VegetationType) {
        database.VegetationTypeDao().save(vegetationType)
    }

    suspend fun getVegetationTypeWithID(id: Int): Result<VegetationType, Error> {
        val vegetationType = database.VegetationTypeDao().getVegetationTypeWithID(id)
        return if (vegetationType != null) Result.Success(vegetationType) else Result.Error(Error.NoData())
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
        return if (hosts.isNotEmpty())  Result.Success(hosts) else Result.Error(Error.NoData())
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
            return Result.Error(Error.NoData())
        } else {
            return Result.Success(mushrooms)
        }
    }

    suspend fun getMushroomWithID(id: Int): Result<Mushroom, Error> {
        val mushroom = database.mushroomsDao().getMushroom(id)

        if (mushroom == null) {
            return Result.Error(Error.NoData())
        } else {
            return Result.Success(mushroom)
        }
    }
}