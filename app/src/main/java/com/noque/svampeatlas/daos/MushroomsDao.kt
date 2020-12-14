package com.noque.svampeatlas.daos

import androidx.room.*
import com.noque.svampeatlas.models.Mushroom



@Dao
interface MushroomsDao {

    @Query("SELECT * FROM mushrooms")
    suspend fun getFavorites(): Array<Mushroom>

    @Query("SELECT * FROM mushrooms WHERE id = :id LIMIT 1")
    suspend fun getMushroom(id: Int): Mushroom?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavorites(vararg mushroom: Mushroom)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun saveMushrooms(vararg mushrooms: Mushroom)

    @Delete
    suspend fun deleteMushroom(mushroom: Mushroom)
}