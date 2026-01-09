package com.fam4k007.videoplayer.danmaku

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fam4k007.videoplayer.dandanplay.DanDanApiManager
import com.fam4k007.videoplayer.dandanplay.DanDanConverter
import com.fam4k007.videoplayer.utils.DialogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 弹幕管理器
 * 负责弹幕的加载、控制、样式管理
 */
class DanmakuManager(
    private val context: Context,
    private val danmakuView: DanmakuPlayerView
) {
    companion object {
        private const val TAG = "DanmakuManager"
    }

    private var isInitialized = false
    
    // 记录当前加载的弹幕文件路径（参考 DanDanPlay 的 mAddedTrack）
    private var currentDanmakuPath: String? = null

    /**
     * 初始化弹幕配置
     */
    fun initialize() {
        if (isInitialized) {
            Log.w(TAG, "DanmakuManager already initialized")
            return
        }

        try {
            DanmakuConfig.init(context)
            isInitialized = true
            
            // 重新应用所有样式设置（因为 DanmakuPlayerView 在 DanmakuConfig.init() 之前就被创建了）
            danmakuView.updateDanmakuSize()
            danmakuView.updateDanmakuSpeed()
            danmakuView.updateDanmakuAlpha()
            danmakuView.updateDanmakuStroke()
            danmakuView.updateScrollDanmakuState()
            danmakuView.updateTopDanmakuState()
            danmakuView.updateBottomDanmakuState()
            danmakuView.updateMaxLine()
            danmakuView.updateMaxScreenNum()
            
            Log.d(TAG, "DanmakuManager initialized successfully with saved settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DanmakuManager", e)
        }
    }

    /**
     * 加载弹幕文件
     * 自动根据视频文件路径查找同名的 .xml 弹幕文件
     * 如果未找到，尝试使用弹弹play API 自动下载
     */
    fun loadDanmakuForVideo(videoUri: String, videoPath: String, autoShow: Boolean = true): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "DanmakuManager not initialized")
            return false
        }

        if (!DanmakuConfig.isEnabled) {
            Log.d(TAG, "Danmaku is disabled")
            return false
        }

        // 查找同名弹幕文件
        val danmakuFile = findDanmakuFile(videoPath)
        if (danmakuFile != null) {
            // 加载本地弹幕
            val loaded = danmakuView.loadDanmaku(danmakuFile.absolutePath)
            if (loaded) {
                currentDanmakuPath = danmakuFile.absolutePath
                danmakuView.setTrackSelected(true)
                if (autoShow) {
                    Log.d(TAG, "Danmaku loaded from local file: ${danmakuFile.absolutePath}")
                }
            }
            return loaded
        } else {
            // 本地未找到，尝试自动匹配并下载
            Log.d(TAG, "No local danmaku found, trying DanDanPlay match...")
            autoMatchAndDownloadDanmaku(videoPath, autoShow)
            return false // 异步操作，先返回 false
        }
    }

    private fun autoMatchAndDownloadDanmaku(videoPath: String, autoShow: Boolean) {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                // 1. 获取视频信息
                val videoFile = File(videoPath)
                if (!videoFile.exists()) return@launch
                
                // 这里暂时简单估算时长，或者需要从 VideoPlayerActivity 传入
                // 实际应该使用 MediaMetadataRetriever 获取准确时长
                // 为演示目的，假设调用方之后会完善
                val durationMs = 0L // TODO: 需要传入真实时长以提高匹配准确率

                // 2. 匹配视频
                val matchResults = DanDanApiManager.matchVideo(
                    fileName = videoFile.name,
                    filePath = videoPath,
                    durationMs = durationMs
                )

                if (matchResults.isNotEmpty()) {
                    // 默认取第一个匹配结果
                    val bestMatch = matchResults[0]
                    Log.d(TAG, "DanDanPlay match success: ${bestMatch.animeTitle} - ${bestMatch.episodeTitle}")

                    // 3. 下载弹幕
                    val comments = DanDanApiManager.getComments(bestMatch.episodeId)
                    if (comments.isNotEmpty()) {
                        // 4. 保存为 XML
                        val danmakuDir = File(context.filesDir, "danmaku_cache")
                        if (!danmakuDir.exists()) danmakuDir.mkdirs()
                        
                        val xmlFile = File(danmakuDir, "${videoFile.nameWithoutExtension}.xml")
                        withContext(Dispatchers.IO) {
                            DanDanConverter.saveToXmlFile(comments, xmlFile)
                        }

                        // 5. 加载弹幕
                        if (xmlFile.exists()) {
                            val loaded = danmakuView.loadDanmaku(xmlFile.absolutePath)
                            if (loaded) {
                                currentDanmakuPath = xmlFile.absolutePath
                                danmakuView.setTrackSelected(true)
                                Log.d(TAG, "Danmaku loaded from DanDanPlay: ${xmlFile.absolutePath}")
                                // TODO: 可以显示 Toast 提示用户已自动下载弹幕
                            }
                        }
                    } else {
                        Log.d(TAG, "No comments found for episode: ${bestMatch.episodeId}")
                    }
                } else {
                    Log.d(TAG, "DanDanPlay match failed for: ${videoFile.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto download danmaku failed", e)
            }
        }
    }

    /**
     * 加载指定的弹幕文件（用户手动选择或历史恢复）
     * 参考 DanDanPlay 的 addTrack 方法
     */
    fun loadDanmakuFile(danmakuPath: String, autoShow: Boolean = true): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "DanmakuManager not initialized")
            return false
        }

        val loaded = danmakuView.loadDanmaku(danmakuPath)
        
        // 如果加载成功，记录路径并设置轨道选中状态（参考 DanDanPlay 的 addTrack → selectTrack 流程）
        if (loaded) {
            currentDanmakuPath = danmakuPath
            // 参考 DanDanPlay: addTrack 成功后自动调用 selectTrack
            danmakuView.setTrackSelected(true)
            Log.d(TAG, "Danmaku loaded and track selected: $danmakuPath, autoShow=$autoShow")
        }
        
        return loaded
    }

    /**
     * 导入弹幕文件（从 URI 复制到内部存储）
     * @param context Android Context
     * @param uri 弹幕文件的 URI
     * @return 复制后的弹幕文件路径，失败返回 null
     */
    fun importDanmakuFile(context: Context, uri: Uri): String? {
        return try {
            val danmakuDir = File(context.filesDir, "danmaku")
            if (!danmakuDir.exists()) {
                danmakuDir.mkdirs()
            }
            
            // 获取文件名
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            } ?: "danmaku_${System.currentTimeMillis()}.xml"
            
            val danmakuFile = File(danmakuDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                danmakuFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            if (danmakuFile.exists()) {
                Log.d(TAG, "Danmaku file imported: ${danmakuFile.absolutePath}")
                // 自动加载导入的弹幕
                val loaded = loadDanmakuFile(danmakuFile.absolutePath, autoShow = true)
                if (loaded) {
                    danmakuFile.absolutePath
                } else {
                    null
                }
            } else {
                Log.e(TAG, "Failed to import danmaku file")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import danmaku file", e)
            null
        }
    }

    /**
     * 查找弹幕文件
     * 规则：视频文件同目录下，同名的 .xml 文件
     */
    private fun findDanmakuFile(videoPath: String): File? {
        try {
            val videoFile = File(videoPath)
            val videoDir = videoFile.parentFile ?: return null
            val videoNameWithoutExt = videoFile.nameWithoutExtension

            // 查找同名 .xml 文件
            val danmakuFile = File(videoDir, "$videoNameWithoutExt.xml")
            if (danmakuFile.exists() && danmakuFile.isFile) {
                Log.d(TAG, "Found danmaku file: ${danmakuFile.absolutePath}")
                return danmakuFile
            }

            // 也可以查找其他可能的命名
            val alternativeNames = listOf(
                "${videoNameWithoutExt}.danmaku.xml",
                "${videoNameWithoutExt}_danmaku.xml",
                "danmaku.xml"
            )

            for (name in alternativeNames) {
                val file = File(videoDir, name)
                if (file.exists() && file.isFile) {
                    Log.d(TAG, "Found alternative danmaku file: ${file.absolutePath}")
                    return file
                }
            }

            Log.d(TAG, "No danmaku file found in: ${videoDir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error finding danmaku file", e)
        }

        return null
    }

    /**
     * 开始播放弹幕
     */
    fun start() {
        danmakuView.startDanmaku()
    }

    /**
     * 暂停弹幕
     */
    fun pause() {
        danmakuView.pauseDanmaku()
    }

    /**
     * 恢复弹幕
     */
    fun resume() {
        danmakuView.resumeDanmaku()
    }

    /**
     * 同步进度
     */
    fun seekTo(timeMs: Long) {
        danmakuView.seekDanmaku(timeMs)
    }

    /**
     * 切换弹幕显示/隐藏
     */
    fun toggleVisibility() {
        danmakuView.toggleDanmakuVisibility()
        Log.d(TAG, "Danmaku visibility toggled to: ${danmakuView.isShown}")
    }

    /**
     * 设置弹幕轨道选中状态（参考 DanDanPlay 的 setTrackSelected）
     * @param selected true=选中轨道并显示弹幕，false=取消选中并隐藏弹幕
     */
    fun setTrackSelected(selected: Boolean) {
        danmakuView.setTrackSelected(selected)
        Log.d(TAG, "Danmaku track selected: $selected")
    }

    /**
     * 设置弹幕显示状态
     */
    fun setVisibility(visible: Boolean) {
        if (visible) {
            danmakuView.show()
        } else {
            danmakuView.hide()
        }
        Log.d(TAG, "Danmaku visibility set to: $visible")
    }

    /**
     * 获取弹幕当前显示状态
     */
    fun isVisible(): Boolean {
        return danmakuView.isShown
    }

    /**
     * 获取当前加载的弹幕文件路径（参考 DanDanPlay 的 getAddedTrack）
     */
    fun getCurrentDanmakuPath(): String? {
        return currentDanmakuPath
    }

    /**
     * 释放资源
     */
    fun release() {
        currentDanmakuPath = null
        danmakuView.releaseDanmaku()
    }

    /**
     * 设置播放倍速
     */
    fun setSpeed(speed: Float) {
        danmakuView.setPlaybackSpeed(speed)
    }

    // ==================== 样式设置方法 ====================

    fun updateSize() {
        danmakuView.updateDanmakuSize()
    }

    fun updateSpeed() {
        danmakuView.updateDanmakuSpeed()
    }

    fun updateAlpha() {
        danmakuView.updateDanmakuAlpha()
    }

    fun updateStroke() {
        danmakuView.updateDanmakuStroke()
    }

    fun updateScrollDanmaku() {
        danmakuView.updateScrollDanmakuState()
    }

    fun updateTopDanmaku() {
        danmakuView.updateTopDanmakuState()
    }

    fun updateBottomDanmaku() {
        danmakuView.updateBottomDanmakuState()
    }

    fun updateMaxLine() {
        danmakuView.updateMaxLine()
    }

    fun updateMaxScreenNum() {
        danmakuView.updateMaxScreenNum()
    }

    fun updateOffsetTime() {
        danmakuView.updateOffsetTime()
    }

    /**
     * 应用所有样式设置
     */
    fun applyAllSettings() {
        updateSize()
        updateSpeed()
        updateAlpha()
        updateStroke()
        updateScrollDanmaku()
        updateTopDanmaku()
        updateBottomDanmaku()
        updateMaxLine()
        updateMaxScreenNum()
    }
}
