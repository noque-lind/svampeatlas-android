package com.noque.svampeatlas.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.noque.svampeatlas.models.Host

@Dao
interface HostsDao {
    @Query("SELECT * FROM hosts")
    suspend fun getHosts(): Array<Host>

    @Query("SELECT * FROM hosts WHERE id = :id")
    suspend fun getHostWithID(id : Int): Host?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vararg hosts: Host)

}