package com.noque.svampeatlas.daos

import androidx.room.*
import com.noque.svampeatlas.models.User

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    suspend fun getUsers(): Array<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Query("DELETE FROM user")
    suspend fun clear()
}