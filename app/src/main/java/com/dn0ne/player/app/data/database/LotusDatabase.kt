package com.dn0ne.player.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(UriConverter::class)
abstract class LotusDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        private const val DATABASE_NAME = "lotus_database"

        @Volatile
        private var INSTANCE: LotusDatabase? = null

        fun getDatabase(context: Context): LotusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LotusDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}