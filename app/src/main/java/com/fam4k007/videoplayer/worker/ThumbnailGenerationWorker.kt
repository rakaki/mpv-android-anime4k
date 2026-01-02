package com.fam4k007.videoplayer.worker

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 缩略图生成后台任务Worker
 * 使用WorkManager在设备空闲且电量充足时生成缩略图，不阻塞主线程
 */
class ThumbnailGenerationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ThumbnailWorker"
        
        // Input参数
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_DURATION_MS = "duration_ms"
        const val KEY_PRIORITY = "priority" // high/medium/low
        
        // Output参数
        const val KEY_GENERATED_COUNT = "generated_count"
        const val KEY_TOTAL_SIZE_KB = "total_size_kb"
        
        private const val THUMBNAIL_WIDTH = 320
        private const val THUMBNAIL_HEIGHT = 180
        private const val THUMBNAIL_QUALITY = 85
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val videoUriString = inputData.getString(KEY_VIDEO_URI) ?: return@withContext Result.failure()
            val durationMs = inputData.getLong(KEY_DURATION_MS, 0L)
            val priority = inputData.getString(KEY_PRIORITY) ?: "medium"
            
            if (durationMs <= 0) {
                Logger.w(TAG, "Invalid duration: $durationMs")
                return@withContext Result.failure()
            }
            
            val videoUri = Uri.parse(videoUriString)
            Logger.d(TAG, "Starting thumbnail generation for: $videoUriString, duration: ${durationMs}ms, priority: $priority")
            
            // 根据优先级决定生成范围
            val durationSec = durationMs / 1000
            val interval = when (priority) {
                "high" -> 2L    // 高优先级：每2秒一帧
                "medium" -> 5L  // 中优先级：每5秒一帧
                else -> 10L     // 低优先级：每10秒一帧
            }
            
            val positions = mutableListOf<Long>()
            var pos = 0L
            while (pos < durationSec) {
                positions.add(pos)
                pos += interval
            }
            
            Logger.d(TAG, "Will generate ${positions.size} thumbnails with ${interval}s interval")
            
            // 生成缩略图
            val result = generateThumbnails(videoUri, positions)
            
            Logger.d(TAG, "Thumbnail generation completed: ${result.first} thumbnails, ${result.second}KB")
            
            // 返回结果
            val outputData = workDataOf(
                KEY_GENERATED_COUNT to result.first,
                KEY_TOTAL_SIZE_KB to result.second
            )
            
            Result.success(outputData)
            
        } catch (e: Exception) {
            Logger.e(TAG, "Thumbnail generation failed: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * 批量生成缩略图
     * @return Pair(生成数量, 总大小KB)
     */
    private suspend fun generateThumbnails(videoUri: Uri, positions: List<Long>): Pair<Int, Long> {
        var generatedCount = 0
        var totalSizeKB = 0L
        
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(applicationContext, videoUri)
            
            val cacheDir = File(applicationContext.cacheDir, "thumbnails_bg").apply {
                if (!exists()) mkdirs()
            }
            
            for (positionSec in positions) {
                // 检查任务是否被取消
                if (isStopped) {
                    Logger.d(TAG, "Work stopped, breaking thumbnail generation")
                    break
                }
                
                try {
                    // 提取视频帧
                    val frame = retriever.getFrameAtTime(
                        positionSec * 1000 * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    ) ?: continue
                    
                    // 缩放
                    val scaledBitmap = scaleBitmap(frame, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    if (frame != scaledBitmap) {
                        frame.recycle()
                    }
                    
                    // 保存到磁盘
                    val fileName = "thumb_${videoUri.toString().hashCode()}_${positionSec}.jpg"
                    val file = File(cacheDir, fileName)
                    
                    FileOutputStream(file).use { output ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, output)
                    }
                    
                    scaledBitmap.recycle()
                    
                    generatedCount++
                    totalSizeKB += file.length() / 1024
                    
                    // 每生成10张报告一次进度
                    if (generatedCount % 10 == 0) {
                        Logger.d(TAG, "Progress: $generatedCount/${positions.size} thumbnails generated")
                        setProgress(workDataOf(KEY_GENERATED_COUNT to generatedCount))
                    }
                    
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to generate thumbnail at ${positionSec}s: ${e.message}")
                    // 继续处理下一张
                }
            }
            
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // 忽略
            }
        }
        
        return Pair(generatedCount, totalSizeKB)
    }

    /**
     * 缩放Bitmap保持宽高比
     */
    private fun scaleBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height
        
        val scale = minOf(
            maxWidth.toFloat() / sourceWidth,
            maxHeight.toFloat() / sourceHeight
        )
        
        if (scale >= 1.0f) {
            return source
        }
        
        val scaledWidth = (sourceWidth * scale).toInt()
        val scaledHeight = (sourceHeight * scale).toInt()
        
        return Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
    }
}
