package com.example.flightmobileapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Url::class], version = 2, exportSchema = false)
abstract class UrlDataBase : RoomDatabase() {
    abstract val urlDataBaseDao: UrlDataBaseDao

    companion object {
        private fun newDataBase(context: Context): UrlDataBase {
            return Room.databaseBuilder(
                context.applicationContext,
                UrlDataBase::class.java, "url_table"
            ).fallbackToDestructiveMigration().allowMainThreadQueries().build()
        }

        @Volatile
        private var INSTANCE: UrlDataBase? = null
        fun getInstance(context: Context): UrlDataBase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = newDataBase(context)
                }
                INSTANCE = instance
                return instance
            }
        }
    }
}