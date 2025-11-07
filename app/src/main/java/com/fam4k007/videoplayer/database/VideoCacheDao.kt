package com.fam4k007.videoplayer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 视频缓存 DAO
 */
@Dao
interface VideoCacheDao {
    
    @Query("SELECT * FROM video_cache ORDER BY folderName, name")
    fun getAllVideos(): List<VideoCacheEntity>
    
    @Query("SELECT * FROM video_cache WHERE folderPath = :folderPath ORDER BY name")
    fun getVideosByFolder(folderPath: String): List<VideoCacheEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVideos(videos: List<VideoCacheEntity>)
    
    @Query("DELETE FROM video_cache WHERE lastScanned < :timestamp")
    fun deleteOldEntries(timestamp: Long): Int
    
    @Query("DELETE FROM video_cache")
    fun clearAll(): Int
    
    @Query("SELECT COUNT(*) FROM video_cache")
    fun getVideoCount(): Int
}
