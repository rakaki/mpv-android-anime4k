package com.fam4k007.videoplayer.manager

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 视频缩略图管理器 - 重构优化版
 * 核心优化：
 * 1. 单例 MediaMetadataRetriever，避免重复创建
 * 2. 智能预加载：拖动时预加载周围10秒
 * 3. 防抖机制：快速拖动时避免过度提取
 * 4. 高效缓存：LruCache + 按需提取
 */
class VideoThumbnailManager(context: Context) {
    
    private val contextRef = WeakReference(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // LRU 缓存，最多300张缩略图（约占30MB内存）
    private val thumbnailCache = LruCache<Long, Bitmap>(300)
    
    // 单例 MediaMetadataRetriever，避免重复创建
    private var retriever: MediaMetadataRetriever? = null
    private val retrieverLock = Any()
    
    // 当前视频信息
    private var currentVideoUri: Uri? = null
    private var videoDuration: Long = 0L
    private var isLocal: Boolean = false
    private var isInitialized = AtomicBoolean(false)
    
    // 缩略图参数
    private val thumbnailWidth = 480
    private val thumbnailHeight = 270
    
    // 预加载控制
    private var preloadJob: Job? = null
    private val preloadRange = 10L  // 预加载前后10秒
    
    companion object {
        private const val TAG = "VideoThumbnailManager"
    }
    
    /**
     * 初始化视频
     */
    fun initializeVideo(uri: Uri, duration: Long, isWebDav: Boolean = false) {
        // 如果是同一视频，直接返回
        if (isInitialized.get() && currentVideoUri == uri) {
            return
        }
        
        isLocal = !isWebDav && (uri.scheme == "file" || uri.scheme == "content")
        
        if (!isLocal) {
            Logger.d(TAG, "非本地视频，跳过缩略图")
            return
        }
        
        // 释放旧资源
        releaseRetriever()
        thumbnailCache.evictAll()
        
        currentVideoUri = uri
        videoDuration = duration
        isInitialized.set(true)
        
        // 初始化 MediaMetadataRetriever
        scope.launch {
            initializeRetriever(uri)
        }
        
        Logger.d(TAG, "初始化完成: ${duration / 1000}秒")
    }
    
    /**
     * 初始化 MediaMetadataRetriever（在后台线程）
     */
    private suspend fun initializeRetriever(uri: Uri) = withContext(Dispatchers.IO) {
        val context = contextRef.get() ?: return@withContext
        
        synchronized(retrieverLock) {
            try {
                retriever?.release()
                retriever = MediaMetadataRetriever().apply {
                    setDataSource(context, uri)
                }
                Logger.d(TAG, "MediaMetadataRetriever 初始化成功")
            } catch (e: Exception) {
                Logger.e(TAG, "初始化失败: ${e.message}", e)
                retriever = null
            }
        }
    }
    
    /**
     * 获取缩略图（带缓存）
     */
    fun getThumbnailAt(positionSec: Long): Bitmap? {
        return thumbnailCache.get(positionSec)
    }
    
    /**
     * 实时提取缩略图（主方法）- 关键帧优先策略
     */
    suspend fun extractThumbnailRealtime(positionSec: Long): Bitmap? = withContext(Dispatchers.IO) {
        // 先检查缓存
        thumbnailCache.get(positionSec)?.let { return@withContext it }
        
        // 提取缩略图（关键帧优先，速度更快）
        val bitmap = extractFrameFast(positionSec)
        
        // 缓存结果
        bitmap?.let { thumbnailCache.put(positionSec, it) }
        
        bitmap
    }
    
    /**
     * 快速提取视频帧（关键帧优先）
     */
    private fun extractFrameFast(positionSec: Long): Bitmap? {
        synchronized(retrieverLock) {
            val ret = retriever ?: return null
            
            return try {
                val timeUs = positionSec * 1_000_000
                
                // 策略1：优先使用关键帧（SYNC），速度最快（50-100ms）
                val syncFrame = ret.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (syncFrame != null) {
                    return scaleBitmap(syncFrame)
                }
                
                // 策略2：使用精确帧（CLOSEST），较慢但更准确（200-500ms）
                val exactFrame = ret.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (exactFrame != null) {
                    return scaleBitmap(exactFrame)
                }
                
                null
            } catch (e: Exception) {
                Logger.e(TAG, "提取帧失败 @${positionSec}s: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 智能预加载：拖动时预加载周围的帧（更激进的策略）
     */
    fun preloadAroundPosition(centerSec: Long) {
        preloadJob?.cancel()
        preloadJob = scope.launch {
            val startSec = maxOf(0, centerSec - preloadRange)
            val endSec = minOf(videoDuration / 1000, centerSec + preloadRange)
            
            // 优先加载中心附近的帧（每秒一帧）
            val positions = mutableListOf<Long>()
            for (offset in 0..preloadRange) {
                if (centerSec + offset <= endSec) positions.add(centerSec + offset)
                if (centerSec - offset >= startSec && offset > 0) positions.add(centerSec - offset)
            }
            
            // 批量提取（使用关键帧，速度快）
            positions.forEach { pos ->
                if (!isActive) return@forEach
                if (thumbnailCache.get(pos) == null) {
                    extractFrameFast(pos)?.let { thumbnailCache.put(pos, it) }
                }
                delay(3)  // 减少延迟，加快预加载
            }
        }
    }
    
    /**
     * 缩放 Bitmap
     */
    private fun scaleBitmap(source: Bitmap): Bitmap {
        val sw = source.width
        val sh = source.height
        
        if (sw <= thumbnailWidth && sh <= thumbnailHeight) {
            return source
        }
        
        val scale = minOf(
            thumbnailWidth.toFloat() / sw,
            thumbnailHeight.toFloat() / sh
        )
        
        val targetW = (sw * scale).toInt()
        val targetH = (sh * scale).toInt()
        
        val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)
        if (scaled != source) source.recycle()
        
        return scaled
    }
    
    /**
     * 释放 MediaMetadataRetriever
     */
    private fun releaseRetriever() {
        synchronized(retrieverLock) {
            retriever?.let {
                try {
                    it.release()
                } catch (e: Exception) {
                    Logger.e(TAG, "释放 retriever 失败: ${e.message}")
                }
            }
            retriever = null
        }
    }
    
    /**
     * 清理资源
     */
    fun release() {
        preloadJob?.cancel()
        scope.cancel()
        releaseRetriever()
        thumbnailCache.evictAll()
        currentVideoUri = null
        videoDuration = 0L
        isInitialized.set(false)
    }
    
    /**
     * 检查是否支持缩略图
     */
    fun isThumbnailSupported(): Boolean {
        return isLocal && currentVideoUri != null && videoDuration > 0
    }
}
