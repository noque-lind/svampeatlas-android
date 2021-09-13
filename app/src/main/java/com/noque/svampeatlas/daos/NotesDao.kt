package com.noque.svampeatlas.daos

import androidx.room.*
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.NewObservation

@Dao
interface NotesDao {
    @Query("SELECT * FROM notes")
    suspend fun getAll(): Array<NewObservation>

    @Query("SELECT * FROM notes WHERE creationDate = :id LIMIT 1")
    suspend fun get(id: Long): NewObservation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vararg notes: NewObservation)

    @Delete
    suspend fun delete(note: NewObservation)
}