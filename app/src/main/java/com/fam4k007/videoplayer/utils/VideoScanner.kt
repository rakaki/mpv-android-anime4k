package com.fam4k007.videoplayer.utils

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VideoItem(val name: String, val path: String, val folder: String)

object VideoScanner {
    
    /**
     * 异步获取所有视频（带分页支持）
     * @param context Context
     * @param page 页码（从0开始）
     * @param pageSize 每页数量
     * @return 视频列表
     */
    suspend fun getAllVideos(
        context: Context,
        page: Int = 0,
        pageSize: Int = 50
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )
        
        // 计算分页参数
        val offset = page * pageSize
        val limit = "$offset,$pageSize"
        
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC LIMIT $limit"
        
        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )
            
            cursor?.use {
                val nameIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx) ?: continue
                    val path = it.getString(pathIdx) ?: continue
                    
                    // 检查文件路径是否在包含 .nomedia 的文件夹中
                    if (NoMediaChecker.fileInNoMediaFolder(path)) {
                        android.util.Log.d("VideoScanner", "跳过 .nomedia 文件夹中的视频: $path")
                        continue
                    }
                    
                    val folder = path.substringBeforeLast('/')
                    videos.add(VideoItem(name, path, folder))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoScanner", "Error scanning videos", e)
        }
        
        videos
    }
    
    /**
     * 获取视频总数
     */
    suspend fun getVideoCount(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Video.Media._ID),
                null,
                null,
                null
            )
            cursor?.use {
                return@withContext it.count
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoScanner", "Error counting videos", e)
        }
        0
    }
    
    /**
     * 同步方法（兼容旧代码，但不推荐使用）
     * @deprecated 请使用 suspend fun getAllVideos()
     */
    @Deprecated("Use suspend getAllVideos() instead", ReplaceWith("getAllVideos(context, 0, Int.MAX_VALUE)"))
    fun getAllVideosSync(context: Context): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val pathIdx = it.getColumnIndex(MediaStore.Video.Media.DATA)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx)
                val path = it.getString(pathIdx)
                
                // 检查文件路径是否在包含 .nomedia 的文件夹中
                if (NoMediaChecker.fileInNoMediaFolder(path)) {
                    android.util.Log.d("VideoScanner", "跳过 .nomedia 文件夹中的视频: $path")
                    continue
                }
                
                val folder = path.substringBeforeLast('/')
                videos.add(VideoItem(name, path, folder))
            }
        }
        return videos
    }
}
