package com.fam4k007.videoplayer.sniffer

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 视频嗅探管理器（单例）
 * 负责管理WebView中检测到的视频URL
 */
object VideoSnifferManager {
    private const val TAG = "VideoSnifferManager"
    private const val MAX_VIDEOS = 50  // 最多保存50个视频
    
    // 使用StateFlow管理检测到的视频列表（线程安全）
    private val _detectedVideos = MutableStateFlow<List<DetectedVideo>>(emptyList())
    val detectedVideos: StateFlow<List<DetectedVideo>> = _detectedVideos.asStateFlow()
    
    // 用于去重的URL集合
    private val urlSet = ConcurrentHashMap.newKeySet<String>()
    
    /**
     * 添加检测到的视频
     */
    fun addVideo(video: DetectedVideo) {
        // URL去重
        if (urlSet.contains(video.url)) {
            Log.d(TAG, "Video already exists: ${video.url}")
            return
        }
        
        urlSet.add(video.url)
        
        val currentList = _detectedVideos.value.toMutableList()
        currentList.add(0, video)  // 添加到列表开头
        
        // 限制列表大小
        if (currentList.size > MAX_VIDEOS) {
            val removed = currentList.removeAt(currentList.size - 1)
            urlSet.remove(removed.url)
        }
        
        _detectedVideos.value = currentList
        
        Log.d(TAG, "Added video: ${video.getDisplayText()}, total: ${currentList.size}")
    }
    
    /**
     * 处理WebView拦截的请求
     * @return 是否为视频URL
     */
    fun processRequest(
        url: String,
        headers: Map<String, String>,
        pageUrl: String,
        pageTitle: String
    ): Boolean {
        if (UrlDetector.isVideo(url, headers)) {
            val video = DetectedVideo(
                url = url,
                title = pageTitle,
                pageUrl = pageUrl,
                headers = headers
            )
            addVideo(video)
            return true
        }
        return false
    }
    
    /**
     * 清空检测到的视频
     */
    fun clear() {
        _detectedVideos.value = emptyList()
        urlSet.clear()
        Log.d(TAG, "Cleared all detected videos")
    }
    
    /**
     * 开始新页面的检测（清空之前的结果）
     */
    fun startNewPage() {
        clear()
        Log.d(TAG, "Started detecting new page")
    }
    
    /**
     * 获取最新检测到的视频
     */
    fun getLatestVideo(): DetectedVideo? {
        return _detectedVideos.value.firstOrNull()
    }
    
    /**
     * 获取检测到的视频数量
     */
    fun getVideoCount(): Int {
        return _detectedVideos.value.size
    }
}
