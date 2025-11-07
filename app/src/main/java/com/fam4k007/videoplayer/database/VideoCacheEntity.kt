package com.fam4k007.videoplayer.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 视频文件缓存实体
 * 用于缓存扫描结果，避免每次都查询MediaStore
 */
@Entity(tableName = "video_cache")
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
