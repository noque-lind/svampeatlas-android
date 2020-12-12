package com.noque.svampeatlas.services

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.noque.svampeatlas.daos.*
import com.noque.svampeatlas.models.*

val MIGRATION_12_13 = object: Migration(12,13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE user "
                + " ADD COLUMN roles TEXT")
        database.execSQL("DROP TABLE hosts")
        database.execSQL("CREATE TABLE IF NOT EXISTS `hosts` (`id` INTEGER NOT NULL, `dkName` TEXT, `latinName` TEXT NOT NULL, `probability` INTEGER, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_13_14 = object: Migration(13,14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE substrates ADD COLUMN groupCzName TEXT")
        database.execSQL("ALTER TABLE substrates ADD COLUMN czName TEXT")
        database.execSQL("ALTER TABLE vegetationType ADD COLUMN czName TEXT")
        database.execSQL("ALTER TABLE mushrooms ADD COLUMN vernacularNameCz TEXT")
    }
}

@Database(entities = [User::class, Substrate::class, VegetationType::class, Host::class, Mushroom::class],
    version = 15)

@TypeConverters(ImagesTypeConverters::class, RedListDataTypeConverters::class, UserRolesTypeConverters::class)


abstract class Database: RoomDatabase() {
    abstract fun UserDao(): UserDao
    abstract fun SubstratesDao(): SubstratesDao
    abstract fun VegetationTypeDao(): VegetationTypeDao
    abstract fun HostsDao(): HostsDao
    abstract fun mushroomsDao(): MushroomsDao


    companion object {
        fun buildDatabase(context: Context) = Room.databaseBuilder(context,
            com.noque.svampeatlas.services.Database::class.java, "fungal-database.db")
            .addMigrations(MIGRATION_12_13)
            .addMigrations(MIGRATION_13_14)
            .fallbackToDestructiveMigration()
            .build()
    }
}