package com.fam4k007.videoplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 视频数据库
 */
@Database(entities = [VideoCacheEntity::class], version = 1, exportSchema = false)
abstract class VideoDatabase : RoomDatabase() {
    
    abstract fun videoCacheDao(): VideoCacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null
        
        fun getDatabase(context: Context): VideoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "video_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
