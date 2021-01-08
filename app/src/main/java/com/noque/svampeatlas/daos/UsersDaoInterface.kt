package com.noque.svampeatlas.daos

import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.models.User
import com.noque.svampeatlas.services.RoomService

class UsersDaoInterface(private val dao: UserDao) {
    suspend fun saveUser(user: User) {
        dao.insert(user)
    }

    suspend fun clearUser() {
        dao.clear()
    }

    suspend fun getUser(): Result<User, RoomService.Error> {
        val users = dao.getUsers()
        return if (users.firstOrNull() != null) Result.Success(users.first()) else Result.Error(
            RoomService.Error.NoData(RoomService.Error.DataType.USER)
        )
    }
}