package com.fam4k007.videoplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 视频缩略图提取工具
 * 参考 DanDanPlay 在历史记录中显示视频当前播放位置的缩略图
 */
object VideoThumbnailHelper {
    
    private const val TAG = "VideoThumbnailHelper"
    private const val THUMBNAIL_WIDTH = 320
    private const val THUMBNAIL_HEIGHT = 180
    private const val THUMBNAIL_QUALITY = 85
    
    /**
     * 从视频指定位置提取缩略图并保存到本地
     * @param context 上下文
     * @param videoUri 视频URI
     * @param positionMs 提取位置（毫秒）
     * @return 缩略图文件路径，失败返回null
     */
    suspend fun extractAndSaveThumbnail(
        context: Context,
        videoUri: Uri,
        positionMs: Long
    ): String? = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        var outputStream: FileOutputStream? = null
        
        try {
            // 1. 创建缩略图目录
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            if (!thumbnailDir.exists()) {
                thumbnailDir.mkdirs()
            }
            
            // 2. 生成缩略图文件名（基于视频URI的哈希）
            val fileName = "thumb_${videoUri.toString().hashCode().toString().replace("-", "N")}.jpg"
            val thumbnailFile = File(thumbnailDir, fileName)
            
            // 3. 提取视频帧
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            
            // 确保提取位置不为负数
            val extractPosition = if (positionMs < 0) 0L else positionMs
            
            // 提取指定位置的视频帧（微秒）
            val bitmap = retriever.getFrameAtTime(
                extractPosition * 1000, // 转换为微秒
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            
            if (bitmap == null) {
                Log.w(TAG, "Failed to extract frame at position $extractPosition ms")
                return@withContext null
            }
            
            // 4. 缩放图片（保持宽高比）
            val scaledBitmap = scaleBitmap(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            
            // 5. 保存为JPEG文件
            outputStream = FileOutputStream(thumbnailFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
            outputStream.flush()
            
            // 6. 释放资源
            bitmap.recycle()
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            
            Log.d(TAG, "Thumbnail saved: ${thumbnailFile.absolutePath}")
            return@withContext thumbnailFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract thumbnail", e)
            return@withContext null
        } finally {
            try {
                retriever?.release()
                outputStream?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release resources", e)
            }
        }
    }
    
    /**
     * 缩放图片（保持宽高比）
     */
    private fun scaleBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height
        
        // 计算缩放比例
        val scale = minOf(
            maxWidth.toFloat() / sourceWidth,
            maxHeight.toFloat() / sourceHeight
        )
        
        // 如果原始尺寸已经小于目标尺寸，直接返回
        if (scale >= 1.0f) {
            return source
        }
        
        val scaledWidth = (sourceWidth * scale).toInt()
        val scaledHeight = (sourceHeight * scale).toInt()
        
        return Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
    }
    
    /**
     * 清理旧的缩略图（可选）
     */
    fun cleanOldThumbnails(context: Context, maxAge: Long = 7 * 24 * 60 * 60 * 1000L) {
        try {
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            if (!thumbnailDir.exists()) return
            
            val currentTime = System.currentTimeMillis()
            thumbnailDir.listFiles()?.forEach { file ->
                if (currentTime - file.lastModified() > maxAge) {
                    file.delete()
                    Log.d(TAG, "Deleted old thumbnail: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old thumbnails", e)
        }
    }
}
