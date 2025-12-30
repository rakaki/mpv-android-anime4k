package com.fam4k007.videoplayer.utils

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
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
            
            // 帧率 - 使用多种方法获取，优先使用准确的方法
            val frameRate = getFrameRate(context, uri, retriever, duration)
            
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
    
    /**
     * 获取视频帧率 - 使用多种方法，优先使用最准确的
     */
    private fun getFrameRate(
        context: Context,
        uri: Uri,
        retriever: MediaMetadataRetriever,
        duration: Long
    ): String {
        var frameRateValue: Double? = null
        var isMkvOrWebm = false
        
        // 方法1: 使用 MediaExtractor 和 MediaFormat 获取 (最准确)
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            
            // 查找视频轨道
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                
                // 检测是否为 MKV/WebM 格式
                if (mime.contains("matroska") || mime.contains("webm")) {
                    isMkvOrWebm = true
                }
                
                if (mime.startsWith("video/")) {
                    Log.d(TAG, "Video track found - MIME: $mime")
                    
                    // 尝试获取帧率
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRateValue = format.getInteger(MediaFormat.KEY_FRAME_RATE).toDouble()
                        Log.d(TAG, "MediaFormat KEY_FRAME_RATE: $frameRateValue")
                    }
                    
                    // 对于 MKV/WebM，尝试从其他键获取
                    if ((frameRateValue == null || frameRateValue <= 0) && isMkvOrWebm) {
                        // 尝试读取 max-fps (某些 MKV 文件会有这个)
                        try {
                            if (format.containsKey("max-fps")) {
                                frameRateValue = format.getInteger("max-fps").toDouble()
                                Log.d(TAG, "MKV max-fps: $frameRateValue")
                            }
                        } catch (e: Exception) {
                            // 忽略
                        }
                    }
                    break
                }
            }
            extractor.release()
        } catch (e: Exception) {
            Log.w(TAG, "MediaExtractor failed: ${e.message}")
        }
        
        // 方法2: 使用 METADATA_KEY_CAPTURE_FRAMERATE (Android 6.0+)
        if ((frameRateValue == null || frameRateValue <= 0) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val captureFrameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            if (captureFrameRate != null) {
                frameRateValue = captureFrameRate.toDoubleOrNull()
                Log.d(TAG, "CAPTURE_FRAMERATE: $captureFrameRate")
            }
        }
        
        // 方法3: 从视频帧数和时长计算 (Android 9.0+)
        if ((frameRateValue == null || frameRateValue <= 0) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val frameCountStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
            val frameCount = frameCountStr?.toLongOrNull()
            
            if (frameCount != null && frameCount > 0 && duration > 0) {
                frameRateValue = (frameCount * 1000.0) / duration.toDouble()
                Log.d(TAG, "Calculated from frame count: $frameRateValue (frames=$frameCount, duration=$duration)")
            }
        }
        
        // 方法4: 对于 MKV 等特殊格式，尝试采样法估算帧率
        if ((frameRateValue == null || frameRateValue <= 0) && isMkvOrWebm && duration > 1000) {
            frameRateValue = estimateFrameRateBySampling(context, uri, duration)
            if (frameRateValue != null) {
                Log.d(TAG, "Estimated by sampling: $frameRateValue")
            }
        }
        
        // 格式化输出
        return when {
            frameRateValue == null || frameRateValue <= 0 -> "未知"
            frameRateValue.isNaN() || frameRateValue.isInfinite() -> "未知"
            else -> {
                // 识别常见帧率并优化显示
                val roundedFps = when {
                    // 23.976 (23.98) - 电影标准
                    frameRateValue in 23.8..24.1 -> "24 fps"
                    // 25 - PAL标准
                    frameRateValue in 24.8..25.2 -> "25 fps"
                    // 29.97 - NTSC标准
                    frameRateValue in 29.5..30.2 -> "30 fps"
                    // 50 - PAL高帧率
                    frameRateValue in 49.5..50.5 -> "50 fps"
                    // 59.94/60 - 高帧率
                    frameRateValue in 59.5..60.5 -> "60 fps"
                    // 120/144 - 超高帧率
                    frameRateValue in 119.5..120.5 -> "120 fps"
                    frameRateValue in 143.5..144.5 -> "144 fps"
                    // 其他帧率显示一位小数
                    frameRateValue < 100 -> String.format("%.1f fps", frameRateValue)
                    // 超过100的显示整数
                    else -> String.format("%.0f fps", frameRateValue)
                }
                roundedFps
            }
        }
    }
    
    /**
     * 通过采样关键帧估算帧率 (用于 MKV 等难以获取帧率的格式)
     */
    private fun estimateFrameRateBySampling(context: Context, uri: Uri, duration: Long): Double? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            
            // 查找视频轨道
            var videoTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    break
                }
            }
            
            if (videoTrackIndex < 0) {
                return null
            }
            
            extractor.selectTrack(videoTrackIndex)
            
            // 采样前10秒的关键帧时间戳
            val sampleTimes = mutableListOf<Long>()
            val sampleLimit = 100 // 最多采样100帧
            val timeLimit = 10_000_000L // 10秒 (微秒)
            
            while (sampleTimes.size < sampleLimit) {
                val sampleTime = extractor.sampleTime
                if (sampleTime < 0 || sampleTime > timeLimit) break
                
                sampleTimes.add(sampleTime)
                extractor.advance()
            }
            
            // 计算平均帧间隔
            if (sampleTimes.size >= 10) {
                val intervals = mutableListOf<Long>()
                for (i in 1 until sampleTimes.size) {
                    intervals.add(sampleTimes[i] - sampleTimes[i - 1])
                }
                
                // 去除异常值（过大或过小的间隔）
                intervals.sort()
                val validIntervals = intervals.subList(
                    intervals.size / 10,  // 去掉最小的10%
                    intervals.size * 9 / 10  // 去掉最大的10%
                )
                
                if (validIntervals.isNotEmpty()) {
                    val avgInterval = validIntervals.average()
                    if (avgInterval > 0) {
                        // 从微秒转换到fps
                        val fps = 1_000_000.0 / avgInterval
                        if (fps > 5 && fps < 300) { // 合理范围检查
                            return fps
                        }
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "Frame sampling failed: ${e.message}")
            null
        } finally {
            try {
                extractor.release()
            } catch (e: Exception) {
                // 忽略
            }
        }
    }
}