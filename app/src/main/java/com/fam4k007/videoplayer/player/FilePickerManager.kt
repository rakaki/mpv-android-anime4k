package com.fam4k007.videoplayer.player

import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.fam4k007.videoplayer.PlaybackHistoryManager
import com.fam4k007.videoplayer.danmaku.DanmakuManager
import com.fam4k007.videoplayer.manager.PreferencesManager
import com.fam4k007.videoplayer.manager.SubtitleManager
import com.fam4k007.videoplayer.utils.DialogUtils
import java.lang.ref.WeakReference

/**
 * 文件选择器管理器
 * 负责管理字幕和弹幕文件的导入
 */
class FilePickerManager(
    private val activityRef: WeakReference<AppCompatActivity>,
    private val subtitleManager: SubtitleManager,
    private val danmakuManager: DanmakuManager,
    private val historyManager: PlaybackHistoryManager,
    private val playbackEngineRef: WeakReference<PlaybackEngine>,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "FilePickerManager"
    }

    private var subtitlePickerLauncher: ActivityResultLauncher<Array<String>>? = null
    private var danmakuPickerLauncher: ActivityResultLauncher<Array<String>>? = null
    
    private var wasPlayingBeforeSubtitlePicker = false
    private var wasPlayingBeforeDanmakuPicker = false
    
    private var currentVideoUri: Uri? = null

    /**
     * 初始化文件选择器
     */
    fun initialize() {
        val activity = activityRef.get() ?: return
        
        // 字幕文件选择器
        subtitlePickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            handleSubtitleSelected(uri)
        }
        
        // 弹幕文件选择器
        danmakuPickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            handleDanmakuSelected(uri)
        }
        
        Log.d(TAG, "File pickers initialized")
    }

    /**
     * 设置当前播放的视频 URI（用于历史记录更新）
     */
    fun setCurrentVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }

    /**
     * 导入字幕文件
     */
    fun importSubtitle(isPlaying: Boolean) {
        wasPlayingBeforeSubtitlePicker = isPlaying
        if (isPlaying) {
            playbackEngineRef.get()?.pause()
        }
        
        subtitlePickerLauncher?.launch(arrayOf("*/*"))
    }

    /**
     * 导入弹幕文件
     */
    fun importDanmaku(isPlaying: Boolean) {
        wasPlayingBeforeDanmakuPicker = isPlaying
        if (isPlaying) {
            playbackEngineRef.get()?.pause()
        }
        
        danmakuPickerLauncher?.launch(arrayOf("text/xml", "application/xml", "*/*"))
    }

    /**
     * 处理选中的字幕文件
     */
    private fun handleSubtitleSelected(uri: Uri?) {
        val activity = activityRef.get() ?: return
        
        if (uri != null) {
            Log.d(TAG, "Subtitle file selected: $uri")
            val success = subtitleManager.addExternalSubtitle(activity, uri)
            if (success) {
                DialogUtils.showToastShort(activity, "字幕导入成功")
                
                // 保存外挂字幕路径
                currentVideoUri?.let { videoUri ->
                    val subtitlePath = subtitleManager.getLastAddedSubtitlePath()
                    if (subtitlePath != null) {
                        preferencesManager.setExternalSubtitle(videoUri.toString(), subtitlePath)
                        Log.d(TAG, "Saved external subtitle path: $subtitlePath")
                    }
                }
            } else {
                DialogUtils.showToastLong(activity, "字幕导入失败")
            }
        } else {
            Log.d(TAG, "Subtitle picker cancelled")
        }
        
        // 恢复播放状态
        if (wasPlayingBeforeSubtitlePicker) {
            playbackEngineRef.get()?.play()
        }
    }

    /**
     * 处理选中的弹幕文件
     */
    private fun handleDanmakuSelected(uri: Uri?) {
        val activity = activityRef.get() ?: return
        val playbackEngine = playbackEngineRef.get() ?: return
        
        if (uri != null) {
            Log.d(TAG, "Danmaku file selected: $uri")
            try {
                // 使用 DanmakuManager 导入弹幕文件
                val danmakuPath = danmakuManager.importDanmakuFile(activity, uri)
                if (danmakuPath != null) {
                    DialogUtils.showToastShort(activity, "弹幕导入成功")
                    
                    // 加载弹幕文件
                    val loaded = danmakuManager.loadDanmakuFile(danmakuPath, autoShow = true)
                    if (loaded) {
                        // 同步弹幕到当前播放位置
                        val currentPosition = (playbackEngine.currentPosition * 1000).toLong()
                        danmakuManager.seekTo(currentPosition)
                        
                        // 如果正在播放，启动弹幕
                        if (wasPlayingBeforeDanmakuPicker) {
                            danmakuManager.start()
                        }
                        
                        Log.d(TAG, "Danmaku loaded and synced to position: $currentPosition")
                    }
                    
                    // 更新历史记录中的弹幕信息
                    currentVideoUri?.let { videoUri ->
                        historyManager.updateDanmu(
                            uri = videoUri,
                            danmuPath = danmakuPath,
                            danmuVisible = true,
                            danmuOffsetTime = 0L
                        )
                        Log.d(TAG, "Danmu info updated in history")
                    }
                } else {
                    DialogUtils.showToastLong(activity, "弹幕导入失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import danmaku", e)
                DialogUtils.showToastLong(activity, "弹幕导入失败: ${e.message}")
            }
        } else {
            Log.d(TAG, "Danmaku picker cancelled")
        }
        
        // 恢复播放状态
        if (wasPlayingBeforeDanmakuPicker) {
            playbackEngineRef.get()?.play()
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        subtitlePickerLauncher = null
        danmakuPickerLauncher = null
        currentVideoUri = null
        Log.d(TAG, "FilePickerManager cleaned up")
    }
}
