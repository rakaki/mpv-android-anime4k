package com.fam4k007.videoplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * 视频缩略图缓存管理器
 * 提供磁盘缓存、线程池管理、内存优化
 */
class ThumbnailCacheManager private constructor(context: Context) {

    companion object {
        private const val TAG = "ThumbnailCacheManager"
        private const val CACHE_DIR_NAME = "video_thumbnails"
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
        private const val THUMBNAIL_WIDTH = 384
        private const val THUMBNAIL_HEIGHT = 216
        
        @Volatile
        private var instance: ThumbnailCacheManager? = null

        fun getInstance(context: Context): ThumbnailCacheManager {
            return instance ?: synchronized(this) {
                instance ?: ThumbnailCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val cacheDir: File = File(context.cacheDir, CACHE_DIR_NAME).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    // 使用固定线程池限制并发数量
    private val executorService = Executors.newFixedThreadPool(3)

    /**
     * 获取缩略图（带缓存）
     * 1. 先从磁盘缓存查找
     * 2. 如果没有，则提取并缓存
     */
    suspend fun getThumbnail(context: Context, uri: Uri, duration: Long): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = generateCacheKey(uri)
                val cacheFile = File(cacheDir, cacheKey)

                // 1. 尝试从缓存读取
                if (cacheFile.exists()) {
                    Log.d(TAG, "Loading thumbnail from cache: $cacheKey")
                    val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    if (bitmap != null) {
                        return@withContext bitmap
                    } else {
                        // 缓存文件损坏，删除
                        cacheFile.delete()
                    }
                }

                // 2. 提取缩略图
                Log.d(TAG, "Extracting thumbnail for: $uri")
                val bitmap = extractThumbnail(context, uri, duration)

                // 3. 保存到缓存
                if (bitmap != null) {
                    saveThumbnailToCache(bitmap, cacheFile)
                    checkCacheSize()
                }

                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get thumbnail", e)
                null
            }
        }
    }

    /**
     * 提取视频缩略图
     */
    private fun extractThumbnail(context: Context, uri: Uri, duration: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            // 计算中间帧位置（微秒）
            val frameTimeMicros = if (duration > 0) {
                (duration * 1000L) / 2
            } else {
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val videoDuration = durationStr?.toLongOrNull() ?: 0L
                if (videoDuration > 0) {
                    (videoDuration * 1000L) / 2
                } else {
                    5000000L // 默认5秒
                }
            }

            // 提取指定位置的帧
            val bitmap = retriever.getFrameAtTime(
                frameTimeMicros,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )

            // 缩放到目标尺寸
            return if (bitmap != null) {
                Bitmap.createScaledBitmap(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract thumbnail", e)
            return null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release retriever", e)
            }
        }
    }

    /**
     * 保存缩略图到缓存
     */
    private fun saveThumbnailToCache(bitmap: Bitmap, cacheFile: File) {
        try {
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            Log.d(TAG, "Thumbnail saved to cache: ${cacheFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save thumbnail to cache", e)
        }
    }

    /**
     * 生成缓存键（基于URI的MD5）
     */
    private fun generateCacheKey(uri: Uri): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(uri.toString().toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // 降级方案：使用URI的哈希码
            uri.toString().hashCode().toString()
        }
    }

    /**
     * 检查缓存大小，超过限制则清理
     */
    private fun checkCacheSize() {
        try {
            val files = cacheDir.listFiles() ?: return
            var totalSize = files.sumOf { it.length() }

            if (totalSize > MAX_CACHE_SIZE) {
                Log.d(TAG, "Cache size exceeded, cleaning up...")
                
                // 按最后修改时间排序，删除最旧的文件
                val sortedFiles = files.sortedBy { it.lastModified() }
                
                for (file in sortedFiles) {
                    if (totalSize <= MAX_CACHE_SIZE * 0.8) { // 清理到80%
                        break
                    }
                    val fileSize = file.length()
                    if (file.delete()) {
                        totalSize -= fileSize
                        Log.d(TAG, "Deleted cache file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check cache size", e)
        }
    }

    /**
     * 清空所有缓存
     */
    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }

    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        return try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 清理资源
     */
    fun shutdown() {
        executorService.shutdown()
    }
}
