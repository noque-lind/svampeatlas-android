package com.noque.svampeatlas.services

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.noque.svampeatlas.R
import com.noque.svampeatlas.daos.*
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.utilities.MyApplication

object RoomService {

    sealed class Error(title: Int, message: Int) :
        AppError2(title, message, null) {
        enum class DataType {
            FAVORITES,
            USER,
            SUBSTRATE,
            VEGETATIONTYPE,
            MUSHROOM,
            HOST,
            NOTES
        }

        object DatabaseError: Error(R.string.error_database_saveError_title, R.string.error_database_saveError_message)

        class NoData(dataType: DataType) : Error(
            R.string.error_database_noEntries_title,
            when (dataType) {
                DataType.FAVORITES -> R.string.error_database_noEntries_favoritedMushrooms_message
                DataType.NOTES -> R.string.error_database_noNotes_message
                else -> R.string.error_database_noEntries_message
            }
        )
    }

    private val database by lazy { Database.buildDatabase(MyApplication.applicationContext)  }
     val users by lazy { UsersDaoInterface(database.UserDao()) }
     val substrates by lazy { SubstratesDaoInterface(database.SubstratesDao()) }
     val vegetationTypes by lazy { VegetationTypesDaoInterface(database.VegetationTypeDao()) }
     val hosts by lazy { HostsDaoInterface(database.HostsDao()) }
     val mushrooms by lazy { MushroomsDaoInterface(database.mushroomsDao()) }
    val notesDao by lazy { NotesDaoInterface(database.notesDao()) }
}