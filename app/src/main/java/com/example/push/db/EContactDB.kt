package com.example.push.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [EContact::class], version = 1)
@TypeConverters(BitmapConverter::class)
abstract class EContactDB : RoomDatabase() {

    abstract fun econtactDAO() : EContactDAO

    companion object {

        @Volatile
        private var INSTANCE : EContactDB? = null

        private const val DB_NAME = "econtactsdb.db"
        private val LOCK = Any()

        operator fun invoke(context: Context) = INSTANCE ?: synchronized(LOCK) {
            INSTANCE ?: buildDB(context).also {
                INSTANCE = it
            }
        }

        private fun buildDB(context: Context) = Room.databaseBuilder(context.applicationContext, EContactDB::class.java, DB_NAME).fallbackToDestructiveMigration().build()


        fun getInstance(context : Context) : EContactDB {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context, EContactDB::class.java, "econtactdb.db").build()
            }
            return INSTANCE!!
         }

        fun instanceIsStarted() : Boolean {
            return (INSTANCE != null)
        }

        fun destroyInstance() : Boolean {
            INSTANCE = null
            return (INSTANCE == null)
        }

    }

}