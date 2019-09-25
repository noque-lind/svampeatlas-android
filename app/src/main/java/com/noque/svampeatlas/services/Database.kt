package com.noque.svampeatlas.services

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.noque.svampeatlas.daos.*
import com.noque.svampeatlas.models.*

@Database(entities = [User::class, Substrate::class, VegetationType::class, Host::class, Mushroom::class],
    version = 11)

@TypeConverters(ImagesTypeConverters::class, RedListDataTypeConverters::class)

abstract class Database: RoomDatabase() {
    abstract fun UserDao(): UserDao
    abstract fun SubstratesDao(): SubstratesDao
    abstract fun VegetationTypeDao(): VegetationTypeDao
    abstract fun HostsDao(): HostsDao
    abstract fun mushroomsDao(): MushroomsDao


    companion object {
        fun buildDatabase(context: Context) = Room.databaseBuilder(context,
            com.noque.svampeatlas.services.Database::class.java, "fungal-database.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}