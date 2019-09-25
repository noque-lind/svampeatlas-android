package com.noque.svampeatlas.daos

import androidx.room.*
import com.noque.svampeatlas.models.Substrate

@Dao
interface SubstratesDao {
    @Query("SELECT * FROM substrates")
    suspend fun getSubstrates(): Array<Substrate>

    @Query("SELECT * FROM substrates WHERE id = :id LIMIT 1")
    suspend fun getSubstrateWithID(id: Int): Substrate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vararg substrate: Substrate)

}