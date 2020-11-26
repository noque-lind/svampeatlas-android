package com.noque.svampeatlas.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.noque.svampeatlas.models.VegetationType

@Dao
interface VegetationTypeDao {
    @Query("SELECT * FROM vegetationType")
    suspend fun getVegetationTypes(): Array<VegetationType>

    @Query("SELECT * FROM vegetationType WHERE id = :id LIMIT 1")
    suspend fun getVegetationTypeWithID(id: Int): VegetationType?

    @Query("SELECT * FROM vegetationType WHERE id = :id LIMIT 1")
    fun getVegetationTypeWithIDNow(id: Int): VegetationType?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vararg vegetationType: VegetationType)

}