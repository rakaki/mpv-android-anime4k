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
    
    /**
     * 分页查询指定文件夹的视频（支持Paging3）
     */
    @Query("SELECT * FROM video_cache WHERE folderPath = :folderPath ORDER BY name LIMIT :limit OFFSET :offset")
    fun getVideosByFolderPaged(
        folderPath: String,
        limit: Int,
        offset: Int
    ): List<VideoCacheEntity>
    
    /**
     * 获取指定文件夹视频总数（Paging3需要）
     */
    @Query("SELECT COUNT(*) FROM video_cache WHERE folderPath = :folderPath")
    fun getVideoCountByFolder(folderPath: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVideos(videos: List<VideoCacheEntity>)
    
    @Query("DELETE FROM video_cache WHERE lastScanned < :timestamp")
    fun deleteOldEntries(timestamp: Long): Int
    
    @Query("DELETE FROM video_cache")
    fun clearAll(): Int
    
    @Query("SELECT COUNT(*) FROM video_cache")
    fun getVideoCount(): Int
}
