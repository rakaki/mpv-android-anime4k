package com.fam4k007.videoplayer.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 视频文件缓存实体
 * 用于缓存扫描结果，避免每次都查询MediaStore
 * 
 * 索引优化说明：
 * - folderName: 用于 getAllVideos() 的排序优化
 * - folderPath, name: 复合索引，优化 getVideosByFolder() 的查询和排序
 * - lastScanned: 用于 deleteOldEntries() 的条件过滤
 */
@Entity(
    tableName = "video_cache",
    indices = [
        Index(value = ["folderName"]),
        Index(value = ["folderPath", "name"]),
        Index(value = ["lastScanned"])
    ]
)
data class VideoCacheEntity(
    @PrimaryKey
    val uri: String,
    val name: String,
    val path: String,
    val folderPath: String,
    val folderName: String,
    val size: Long,
    val duration: Long,
    val dateModified: Long,
    val dateAdded: Long,
    val lastScanned: Long = System.currentTimeMillis()
)
