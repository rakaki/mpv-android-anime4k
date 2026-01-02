package com.fam4k007.videoplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 视频数据库
 * 
 * Schema 导出配置：
 * - exportSchema = true：导出数据库结构到 app/schemas/ 目录
 * - 每次修改数据库结构时，Room 会自动生成对应版本的 JSON schema 文件
 * - 这些文件应该提交到版本控制，方便追踪数据库变更历史
 */
@Database(
    entities = [VideoCacheEntity::class, PlaybackHistoryEntity::class],
    version = 3,
    exportSchema = true
)
abstract class VideoDatabase : RoomDatabase() {
    
    abstract fun videoCacheDao(): VideoCacheDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null
        
        /**
         * 数据库版本1到2的迁移
         * 添加播放历史记录表
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建播放历史记录表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playback_history (
                        uri TEXT PRIMARY KEY NOT NULL,
                        fileName TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        lastPlayed INTEGER NOT NULL,
                        folderName TEXT NOT NULL,
                        danmuPath TEXT,
                        danmuVisible INTEGER NOT NULL DEFAULT 1,
                        danmuOffsetTime INTEGER NOT NULL DEFAULT 0,
                        thumbnailPath TEXT
                    )
                """.trimIndent())
                
                // 创建索引以优化查询性能
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_lastPlayed ON playback_history(lastPlayed DESC)")
            }
        }
        
        /**
         * 数据库版本2到3的迁移
         * 为video_cache表添加索引以优化查询性能
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为 folderName 添加索引（优化 getAllVideos 排序）
                database.execSQL("CREATE INDEX IF NOT EXISTS index_video_cache_folderName ON video_cache(folderName)")
                
                // 为 folderPath 和 name 添加复合索引（优化 getVideosByFolder 查询和排序）
                database.execSQL("CREATE INDEX IF NOT EXISTS index_video_cache_folderPath_name ON video_cache(folderPath, name)")
                
                // 为 lastScanned 添加索引（优化 deleteOldEntries 条件过滤）
                database.execSQL("CREATE INDEX IF NOT EXISTS index_video_cache_lastScanned ON video_cache(lastScanned)")
            }
        }
        
        fun getDatabase(context: Context): VideoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "video_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
