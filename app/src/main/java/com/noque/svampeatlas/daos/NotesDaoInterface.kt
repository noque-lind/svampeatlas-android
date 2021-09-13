package com.noque.svampeatlas.daos

import android.database.sqlite.SQLiteException
import com.noque.svampeatlas.models.NewObservation
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.services.RoomService

class NotesDaoInterface(private val dao: NotesDao) {
    suspend fun getAll(): Result<List<NewObservation>, RoomService.Error> {
        dao.getAll().toList().also {
            return if (it.isEmpty()) {
                Result.Error(RoomService.Error.NoData(RoomService.Error.DataType.NOTES))
            } else {
                Result.Success(it)
            }
        }
    }

    suspend fun getById(id: Long): Result<NewObservation, RoomService.Error> {
        dao.get(id).also {
            return if (it == null) {
                Result.Error(RoomService.Error.NoData(RoomService.Error.DataType.NOTES))
            } else {
                Result.Success(it)
            }
        }
    }

            suspend fun save(newObservation: NewObservation): Result<Void?, RoomService.Error>  {
        return try {
            dao.save(newObservation)
            Result.Success(null)
        } catch(error: SQLiteException) {
            Result.Error(RoomService.Error.DatabaseError)
        }
    }

    suspend fun delete(newObservation: NewObservation): Result<Void?, RoomService.Error> {
       return try {
           dao.delete(newObservation)
           Result.Success(null)
       } catch (error: SQLiteException) {
           Result.Error(RoomService.Error.DatabaseError)
       }
    }


}