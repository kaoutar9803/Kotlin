package fr.nextu.kouache.kotlinapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [Movie::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDAO


    companion object {

        fun getInstance(applicationContext: Context): AppDatabase {
            return Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "cour_android2.db"
            ).build()
        }
    }
}