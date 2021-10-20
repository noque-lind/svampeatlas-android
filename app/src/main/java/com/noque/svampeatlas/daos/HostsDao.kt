package com.noque.svampeatlas.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Mushroom

@Dao
interface HostsDao {
    @Query("SELECT * FROM hosts")
    suspend fun getHosts(): Array<Host>

    @Query("SELECT * FROM hosts WHERE id = :id")
    fun getHostWithID(id : Int): Host?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun save(vararg hosts: Host)

}