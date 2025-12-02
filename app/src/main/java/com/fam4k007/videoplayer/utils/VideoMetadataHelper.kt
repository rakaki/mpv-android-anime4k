package com.fam4k007.videoplayer.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

/**
 * 视频元数据获取工具类
 */
object VideoMetadataHelper {

    private const val TAG = "VideoMetadataHelper"

    data class VideoMetadata(
        val width: Int,
        val height: Int,
        val videoCodec: String,
        val audioCodec: String,
        val bitrate: String,
        val frameRate: String,
        val duration: Long
    ) {
        fun getResolution(): String = "${width}x${height}"
        fun getFormattedBitrate(): String = bitrate
        fun getFormattedFrameRate(): String = frameRate
    }

    fun getVideoMetadata(context: Context, uri: Uri): VideoMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            
            // 视频编码
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "未知"
            val videoCodec = when {
                mimeType.contains("avc", ignoreCase = true) || mimeType.contains("h264", ignoreCase = true) -> "H.264/AVC"
                mimeType.contains("hevc", ignoreCase = true) || mimeType.contains("h265", ignoreCase = true) -> "H.265/HEVC"
                mimeType.contains("vp9", ignoreCase = true) -> "VP9"
                mimeType.contains("av1", ignoreCase = true) -> "AV1"
                else -> mimeType.substringAfter("video/").uppercase()
            }
            
            // 音频编码（简化版）
            val audioCodec = "AAC"  // 大多数视频使用AAC
            
            // 比特率
            val bitrateValue = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            val bitrate = if (bitrateValue > 0) {
                String.format("%.1f Mbps", bitrateValue / 1_000_000.0)
            } else {
                "未知"
            }
            
            // 帧率 - 尝试多种方法获取
            var frameRateValue: Float? = null
            
            // 方法1: 使用 METADATA_KEY_CAPTURE_FRAMERATE (Android 6.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val captureFrameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                frameRateValue = captureFrameRate?.toFloatOrNull()
                Log.d(TAG, "CAPTURE_FRAMERATE: $captureFrameRate")
            }
            
            // 方法2: 如果方法1失败，尝试从视频帧数和时长计算 (Android 9.0+)
            if ((frameRateValue == null || frameRateValue <= 0) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val frameCountStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                val frameCount = frameCountStr?.toLongOrNull()
                Log.d(TAG, "VIDEO_FRAME_COUNT: $frameCountStr, duration: $duration")
                
                if (frameCount != null && frameCount > 0 && duration > 0) {
                    frameRateValue = (frameCount * 1000.0f) / duration.toFloat()
                    Log.d(TAG, "Calculated frame rate from count: $frameRateValue")
                }
            }
            
            // 方法3: 如果还是失败，尝试估算常见帧率
            if (frameRateValue == null || frameRateValue <= 0) {
                // 对于无法获取帧率的视频，根据常见视频标准估算
                val estimatedFps = when {
                    height >= 2160 -> 60.0f  // 4K通常是60fps
                    height >= 1080 && bitrateValue > 8_000_000 -> 60.0f  // 高码率1080p可能是60fps
                    height >= 720 -> 30.0f   // 720p通常是30fps
                    else -> 24.0f            // 默认24fps（电影标准）
                }
                Log.d(TAG, "Estimated frame rate based on resolution ($width x $height): $estimatedFps")
                frameRateValue = estimatedFps
            }
            
            val frameRate = if (frameRateValue != null && frameRateValue > 0) {
                String.format("%.2f fps", frameRateValue)
            } else {
                "未知"
            }
            
            VideoMetadata(
                width = width,
                height = height,
                videoCodec = videoCodec,
                audioCodec = audioCodec,
                bitrate = bitrate,
                frameRate = frameRate,
                duration = duration
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract video metadata", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release retriever", e)
            }
        }
    }
}
