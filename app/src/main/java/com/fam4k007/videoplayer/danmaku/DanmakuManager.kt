package com.fam4k007.videoplayer.danmaku

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fam4k007.videoplayer.utils.DialogUtils
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
        if (danmakuFile == null) {
            Log.d(TAG, "No danmaku file found for video: $videoPath")
            return false
        }

        // 加载弹幕
        val loaded = danmakuView.loadDanmaku(danmakuFile.absolutePath)
        if (loaded) {
            currentDanmakuPath = danmakuFile.absolutePath
            // 根据参数决定是否自动显示（参考 DanDanPlay 的 setTrackSelected）
            if (autoShow) {
                danmakuView.show()
                Log.d(TAG, "Danmaku loaded and shown: ${danmakuFile.absolutePath}")
            } else {
                Log.d(TAG, "Danmaku loaded (hidden): ${danmakuFile.absolutePath}")
            }
        }
        return loaded
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
        
        // 如果加载成功，记录路径并根据配置决定是否显示
        if (loaded) {
            currentDanmakuPath = danmakuPath
            // 使用保存的开关状态，而不是autoShow参数
            val shouldShow = if (autoShow) DanmakuConfig.isEnabled else false
            if (shouldShow) {
                danmakuView.show()
                Log.d(TAG, "Danmaku loaded and shown: $danmakuPath")
            } else {
                danmakuView.hide()
                Log.d(TAG, "Danmaku loaded (hidden): $danmakuPath")
            }
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
        if (!DanmakuConfig.isEnabled) return
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
        if (!DanmakuConfig.isEnabled) return
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
