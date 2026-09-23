package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AudiobookDao
import com.example.data.dao.BookDao
import com.example.data.dao.BookmarkDao
import com.example.data.model.AudioBookmarkEntity
import com.example.data.model.AudiobookEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity

@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        AudiobookEntity::class,
        AudioBookmarkEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ReadoverDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun audiobookDao(): AudiobookDao

    companion object {
        @Volatile
        private var INSTANCE: ReadoverDatabase? = null

        fun getDatabase(context: Context): ReadoverDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReadoverDatabase::class.java,
                    "readover_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
