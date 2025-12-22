package com.fam4k007.videoplayer.manager

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * 视频缩略图管理器 - 哔哩哔哩级别优化
 * 特性：分批优先级生成、多线程并行、关键帧加速、智能缓存
 */
class VideoThumbnailManager(context: Context) {
    
    private val contextRef = WeakReference(context)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // LRU 缓存，500张缩略图
    private val thumbnailCache = LruCache<Long, Bitmap>(500)
    
    // 当前视频信息
    private var currentVideoUri: Uri? = null
    private var videoDuration: Long = 0L
    private var isLocal: Boolean = false
    private var isInitialized: Boolean = false
    private var isGenerating: Boolean = false
    private var currentPlayPosition: Long = 0
    
    // 缩略图参数 - 使用较小分辨率提高速度
    private val thumbnailWidth = 320
    private val thumbnailHeight = 180
    
    // 提取任务队列
    private val extractQueue = mutableSetOf<Long>()
    
    companion object {
        private const val TAG = "VideoThumbnailManager"
    }
    
    /**
     * 初始化视频，分批优先级生成缩略图
     */
    fun initializeVideo(uri: Uri, duration: Long, isWebDav: Boolean = false) {
        if (isInitialized && currentVideoUri == uri) {
            Log.d(TAG, "缩略图已初始化，跳过")
            return
        }
        
        isLocal = !isWebDav && (uri.scheme == "file" || uri.scheme == "content")
        
        if (!isLocal) {
            Log.d(TAG, "非本地视频，跳过缩略图生成")
            return
        }
        
        currentVideoUri = uri
        videoDuration = duration
        isInitialized = true
        isGenerating = false
        currentPlayPosition = 0
        
        thumbnailCache.evictAll()
        extractQueue.clear()
        
        val durationSec = duration / 1000
        Log.d(TAG, "初始化: ${durationSec}秒, 每2秒一帧")
        
        // 立即开始分批生成
        scope.launch {
            generateThumbnailsInBatches()
        }
    }
    
    /**
     * 分批优先级生成缩略图（类似哔哩哔哩）
     */
    private suspend fun generateThumbnailsInBatches() {
        if (isGenerating) return
        isGenerating = true
        
        val durationSec = videoDuration / 1000
        
        // 第1批：当前位置前后60秒（120帧）
        Log.d(TAG, "[第1批] 生成当前位置前后60秒...")
        generateRange(maxOf(0, currentPlayPosition - 60), minOf(durationSec, currentPlayPosition + 60))
        
        // 第2批：扩展到前后3分钟（360帧）
        Log.d(TAG, "[第2批] 扩展到前后3分钟...")
        generateRange(maxOf(0, currentPlayPosition - 180), minOf(durationSec, currentPlayPosition + 180))
        
        // 第3批：生成全视频
        Log.d(TAG, "[第3批] 生成全视频...")
        generateRange(0, durationSec)
        
        Log.d(TAG, "所有缩略图生成完成，共 ${thumbnailCache.size()} 帧")
        isGenerating = false
    }
    
    /**
     * 生成指定范围的缩略图（并行加速）
     */
    private suspend fun generateRange(startSec: Long, endSec: Long) = withContext(Dispatchers.IO) {
        val uri = currentVideoUri ?: return@withContext
        val context = contextRef.get() ?: return@withContext
        
        // 分成多个块并行处理
        val chunkSize = 30  // 每块30秒
        val chunks = mutableListOf<Pair<Long, Long>>()
        var current = startSec
        while (current < endSec) {
            val end = minOf(current + chunkSize, endSec)
            chunks.add(current to end)
            current = end
        }
        
        // 并行处理每个块（最多3个线程）
        chunks.chunked(3).forEach { batch ->
            batch.map { (start, end) ->
                async {
                    generateChunk(start, end, context, uri)
                }
            }.awaitAll()
        }
    }
    
    /**
     * 生成一个块的缩略图
     */
    private suspend fun generateChunk(startSec: Long, endSec: Long, context: Context, uri: Uri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            
            for (sec in startSec until endSec step 2) {
                if (!scope.isActive || !isInitialized) break
                
                // 跳过已缓存的
                if (thumbnailCache.get(sec) != null) continue
                
                try {
                    val timeUs = sec * 1000 * 1000
                    
                    // 使用 OPTION_CLOSEST_SYNC 获取关键帧，速度更快
                    val frame = retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    
                    frame?.let {
                        val scaledBitmap = scaleBitmapKeepRatio(it, thumbnailWidth, thumbnailHeight)
                        if (it != scaledBitmap) {
                            it.recycle()
                        }
                        thumbnailCache.put(sec, scaledBitmap)
                    }
                    
                } catch (e: Exception) {
                    // 关键帧失败，尝试普通帧
                    try {
                        val frame = retriever.getFrameAtTime(
                            sec * 1000 * 1000,
                            MediaMetadataRetriever.OPTION_CLOSEST
                        )
                        frame?.let {
                            val scaledBitmap = scaleBitmapKeepRatio(it, thumbnailWidth, thumbnailHeight)
                            if (it != scaledBitmap) {
                                it.recycle()
                            }
                            thumbnailCache.put(sec, scaledBitmap)
                        }
                    } catch (e2: Exception) {
                        // 忽略失败的帧
                    }
                }
            }
            
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // 忽略
            }
        }
    }
    
    /**
     * 更新当前播放位置（用于智能预测）
     */
    fun updatePlayPosition(positionSec: Long) {
        currentPlayPosition = positionSec
    }
    
    /**
     * 获取指定位置的缩略图（智能查找最接近的）
     */
    fun getThumbnailAt(positionSec: Long): Bitmap? {
        if (!isLocal || videoDuration <= 0) return null
        
        // 1. 精确匹配
        thumbnailCache.get(positionSec)?.let { return it }
        
        // 2. 找最近的2秒倍数位置
        val nearestKey = (positionSec / 2) * 2
        thumbnailCache.get(nearestKey)?.let { return it }
        
        // 3. 查找前后的帧
        thumbnailCache.get(nearestKey - 2)?.let { return it }
        thumbnailCache.get(nearestKey + 2)?.let { return it }
        
        return null
    }
    
    /**
     * 按比例缩放 Bitmap，保持宽高比
     */
    private fun scaleBitmapKeepRatio(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height
        
        val ratioWidth = maxWidth.toFloat() / sourceWidth
        val ratioHeight = maxHeight.toFloat() / sourceHeight
        val ratio = minOf(ratioWidth, ratioHeight)
        
        val targetWidth = (sourceWidth * ratio).toInt()
        val targetHeight = (sourceHeight * ratio).toInt()
        
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
    
    /**
     * 实时提取缩略图（用于补充）
     */
    suspend fun extractThumbnailRealtime(positionSec: Long): Bitmap? = withContext(Dispatchers.IO) {
        if (!isLocal) return@withContext null
        
        val uri = currentVideoUri ?: return@withContext null
        val context = contextRef.get() ?: return@withContext null
        
        // 再次检查缓存
        getThumbnailAt(positionSec)?.let { return@withContext it }
        
        // 避免重复提取
        synchronized(extractQueue) {
            if (positionSec in extractQueue) return@withContext null
            extractQueue.add(positionSec)
        }
        
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            
            val frame = retriever.getFrameAtTime(
                positionSec * 1000 * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST
            )
            
            frame?.let {
                val scaledBitmap = scaleBitmapKeepRatio(it, thumbnailWidth, thumbnailHeight)
                if (it != scaledBitmap) it.recycle()
                
                thumbnailCache.put(positionSec, scaledBitmap)
                return@withContext scaledBitmap
            }
            
        } finally {
            retriever.release()
            synchronized(extractQueue) {
                extractQueue.remove(positionSec)
            }
        }
        
        null
    }
    
    /**
     * 清理资源
     */
    fun release() {
        scope.cancel()
        thumbnailCache.evictAll()
        extractQueue.clear()
        currentVideoUri = null
        videoDuration = 0L
        isInitialized = false
        isGenerating = false
        currentPlayPosition = 0
    }
    
    /**
     * 检查是否支持缩略图预览
     */
    fun isThumbnailSupported(): Boolean {
        return isLocal && currentVideoUri != null && videoDuration > 0
    }
}
