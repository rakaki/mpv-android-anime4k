
package com.fam4k007.videoplayer.player

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import dev.jdtech.mpv.MPVLib
import java.lang.ref.WeakReference

/**
 * 播放引擎管理器
 * 负责 MPV 播放器的初始化、播放控制、属性管理
 * 使用 WeakReference 防止内存泄漏
 */
class PlaybackEngine(
    private val contextRef: WeakReference<android.content.Context>,
    private val eventCallback: PlaybackEventCallback
) : SurfaceHolder.Callback, MPVLib.EventObserver {

    companion object {
        private const val TAG = "PlaybackEngine"
    }

    interface PlaybackEventCallback {
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onProgressUpdate(position: Double, duration: Double)
        fun onFileLoaded()
        fun onEndOfFile()
        fun onError(message: String)
        fun onSurfaceReady()  // 新增：Surface准备完成回调
    }

    // 播放状态
    var isPlaying: Boolean = false
        private set
    var currentPosition: Double = 0.0
        private set
    var duration: Double = 0.0
        private set
    var currentSpeed: Double = 1.0
        private set
    var isHardwareDecoding: Boolean = true
        private set
    
    // 保存当前文件路径，用于Surface重建时重新加载
    private var currentFilePath: String? = null
    
    // 追踪已添加的外部字幕路径，在视频重新加载后重新添加
    private val externalSubtitlePaths = mutableListOf<String>()

    // 进度更新
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 500)
        }
    }

    private var isInitialized = false
    private var surfaceHolder: SurfaceHolder? = null

    /**
     * 初始化 MPV 播放器
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "PlaybackEngine already initialized")
            return true
        }

        return try {
            val context = contextRef.get() ?: return false

            MPVLib.create(context.applicationContext)
            MPVLib.addObserver(this)

            // 配置 MPV 选项
            MPVLib.setOptionString("vo", "gpu")
            MPVLib.setOptionString("hwdec", "auto")
            MPVLib.setOptionString("ao", "audiotrack,opensles")
            MPVLib.setOptionString("keep-open", "yes")
            MPVLib.setOptionString("gpu-context", "android")

            // 视频适应屏幕
            MPVLib.setOptionString("keepaspect", "yes")
            MPVLib.setOptionString("panscan", "0.0")
            MPVLib.setOptionString("video-aspect-override", "-1")

            // ========== 字幕配置 ==========
            // 参考 mpvKt 项目的字幕配置
            // 自动加载外部字幕文件
            MPVLib.setOptionString("sub-auto", "fuzzy")
            // fuzzy: 模糊匹配文件名（推荐）
            // exact: 只加载完全同名的字幕
            // all: 加载所有字幕文件
            // 字幕文件编码（重要：影响中文显示）
            MPVLib.setOptionString("sub-codepage", "auto")
            // auto: 自动检测编码（推荐，避免乱码）
            // utf8: 强制 UTF-8（如果确定字幕都是 UTF-8）
            // gb18030: 中国国标编码（兼容 GBK）
            // 首选字幕语言（内嵌字幕自动选择）
            MPVLib.setOptionString("slang", "zh,chi,zho,chs,cht,zh-CN,zh-TW,en,eng")
            // 优先级：简体中文 > 繁体中文 > 英文
            // 当视频包含多个字幕轨道时，按此顺序自动选择
            // 🔥 修复：libass 字体配置（解决 "can't find selected font provider" 错误）
            // Android 不支持 fontconfig，需要禁用 sub-font-provider
            MPVLib.setOptionString("sub-font-provider", "none")
            // none: 使用内嵌字体或 libass 默认字体（Android 唯一可用选项）
            // 指定后备字体（Android 系统字体路径）
            MPVLib.setOptionString("sub-fonts-dir", "/system/fonts")
            // Android 系统字体目录，包含 Roboto、Noto Sans CJK 等
            // 默认字体族（支持中文的字体）
            MPVLib.setOptionString("sub-font", "Noto Sans CJK SC")
            // Noto Sans CJK SC = 思源黑体简体中文，支持完整中文字符
            // 忽略字幕文件中指定的字体（强制使用默认字体）
            MPVLib.setOptionString("embeddedfonts", "no")
            // no: 忽略 ASS 字幕文件中的字体样式，统一使用 sub-font
            // 避免因找不到"楷体"、"黑体"等字体而无法显示
            // 字幕显示位置（使用黑边区域）
            MPVLib.setOptionString("sub-use-margins", "yes")
            // yes: 字幕显示在黑边中，不遮挡画面（推荐）
            // no: 字幕覆盖在视频画面上
            // ASS 字幕也使用边距
            MPVLib.setOptionString("sub-ass-force-margins", "yes")
            // yes: ASS 字幕也显示在黑边中
            // no: 保持 ASS 字幕的原始位置
            // 字幕渲染层级
            MPVLib.setOptionString("blend-subtitles", "video")
            // video: 字幕混合到视频层，确保显示在最前面
            // ========== 字幕外观样式 ==========
            // 参考 mpvKt 的默认样式配置
            MPVLib.setOptionString("sub-font-size", "55")  // 字体大小（默认 55）
            MPVLib.setOptionString("sub-border-size", "2.5")  // 边框大小（增强可读性）
            Log.d(TAG, "字幕配置已完成")
            MPVLib.init()
            // 观察属性变化（包括轨道列表变化）
            try {
                MPVLib.observeProperty("pause", 3) // MPV_FORMAT_FLAG = 3
                MPVLib.observeProperty("track-list", 0) // MPV_FORMAT_NONE = 0
                MPVLib.observeProperty("sid", 1) // MPV_FORMAT_STRING = 1
                MPVLib.observeProperty("aid", 1) // MPV_FORMAT_STRING = 1
                Log.d(TAG, "Property observers registered (包括 track-list)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register property observers: ${e.message}")
            }
            isInitialized = true
            Log.d(TAG, "PlaybackEngine initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "MPV initialization failed", e)
            eventCallback.onError("播放器初始化失败: ${e.message}")
            false
        }
    }

    fun attachSurface(surfaceView: SurfaceView) {
        surfaceView.holder.addCallback(this)
        this.surfaceHolder = surfaceView.holder
    }

    /**
     * 加载视频文件
     */
    fun loadVideo(videoUri: android.net.Uri, startPosition: Double = 0.0) {
        if (!isInitialized) {
            Log.e(TAG, "PlaybackEngine not initialized")
            return
        }

        try {
            Log.d(TAG, "Loading video: $videoUri")
            Log.d(TAG, "Start position: $startPosition seconds")
            
            // Surface应该已经准备好（通过onSurfaceReady回调机制）
            if (surfaceHolder != null && surfaceHolder?.surface?.isValid == true) {
                Log.d(TAG, "✓ Surface is valid and ready")
            } else {
                Log.e(TAG, "✗ Surface not ready! Video will fail to initialize")
                handler.post {
                    eventCallback.onError("显示Surface未准备好")
                }
                return
            }
            
            // 保存文件路径，用于Surface重建时重新加载
            currentFilePath = videoUri.toString()
            Log.d(TAG, "✓ 保存文件路径: $currentFilePath")
            
            MPVLib.command(arrayOf("loadfile", videoUri.toString()))
            
            // ⚠️ 重要：标记视频应该自动播放
            // 这样即使 Surface 重新创建，也能正确恢复播放
            wasPlayingBeforeSurfaceDestroyed = true
            
            // 确保视频加载后开始播放
            handler.postDelayed({
                try {
                    MPVLib.setPropertyBoolean("pause", false)
                    isPlaying = true
                    
                    // 字幕将在 FILE_LOADED 事件中自动启用，不在这里处理
                    Log.d(TAG, "Video auto-play started")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to start auto-play: ${e.message}")
                }
            }, 100)
            
            // 如果有起始位置,在文件加载后立即跳转
            if (startPosition > 0.1) {
                // 使用延迟确保文件已开始加载
                handler.postDelayed({
                    try {
                        seekTo(startPosition.toInt())
                        Log.d(TAG, "Restored position: $startPosition")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to restore position: ${e.message}")
                    }
                }, 200)  // 减少延迟到200ms
            }
            
            // 异步记录视频信息(不阻塞播放)
            handler.postDelayed({
                try {
                    val videoCodec = MPVLib.getPropertyString("video-codec")
                    val audioCodec = MPVLib.getPropertyString("audio-codec")
                    val videoFormat = MPVLib.getPropertyString("video-format")
                    val hwdec = MPVLib.getPropertyString("hwdec-current")
                    
                    Log.d(TAG, "Video codec: $videoCodec")
                    Log.d(TAG, "Audio codec: $audioCodec")
                    Log.d(TAG, "Video format: $videoFormat")
                    Log.d(TAG, "Hardware decoding: $hwdec")
                    
                    // 检查视频流是否存在
                    if (videoCodec == null || videoCodec == "null") {
                        Log.w(TAG, "⚠️ Video codec is null - this file may be audio-only or corrupted")
                        handler.post {
                            eventCallback.onError("视频流无效或文件损坏")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get video info: ${e.message}")
                }
            }, 800)

            // 开始进度更新
            handler.post(updateProgressRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load video", e)
            eventCallback.onError("加载视频失败: ${e.message}")
        }
    }

    /**
     * 播放/暂停切换
     */
    fun togglePlayPause() {
        try {
            isPlaying = !isPlaying
            MPVLib.setPropertyBoolean("pause", !isPlaying)
            eventCallback.onPlaybackStateChanged(isPlaying)
            Log.d(TAG, "Playback state toggled to: $isPlaying")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle playback state", e)
            isPlaying = !isPlaying // 恢复状态
            eventCallback.onError("播放状态切换失败: ${e.message}")
        }
    }

    /**
     * 设置播放状态
     */
    fun setPlaying(playing: Boolean) {
        try {
            if (isPlaying != playing) {
                isPlaying = playing
                MPVLib.setPropertyBoolean("pause", !isPlaying)
                eventCallback.onPlaybackStateChanged(isPlaying)
                Log.d(TAG, "Playback state set to: $isPlaying")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set playback state", e)
            // 不反转状态，保留当前状态
            eventCallback.onError("播放状态设置失败: ${e.message}")
        }
    }

    /**
     * 跳转到指定位置（绝对位置）
     * @param precise true=精确定位(慢), false=关键帧定位(快)
     */
    fun seekTo(position: Int, precise: Boolean = false) {
        try {
            // 位置校验：防止超越
            val safePosition = when {
                position < 0 -> {
                    Log.w(TAG, "Seek position is negative: $position, clamping to 0")
                    0
                }
                duration > 0 && position > duration.toInt() -> {
                    Log.w(TAG, "Seek position exceeds duration: $position > ${duration.toInt()}, clamping to duration")
                    duration.toInt()
                }
                else -> position
            }
            
            // 根据精确度选择不同的定位模式
            val seekMode = if (precise) "absolute" else "absolute+keyframes"
            MPVLib.command(arrayOf("seek", safePosition.toString(), seekMode))
            
            Log.d(TAG, "Seek to: $safePosition (mode: $seekMode, requested: $position)")
        } catch (e: Exception) {
            Log.e(TAG, "Seek failed", e)
            eventCallback.onError("快进失败: ${e.message}")
        }
    }

    /**
     * 相对跳转
     */
    fun seekBy(seconds: Int) {
        try {
            val currentPos = MPVLib.getPropertyDouble("time-pos")
            val targetPos = currentPos + seconds
            
            // 位置校验：防止超越
            val safeTargetPos = when {
                targetPos < 0 -> {
                    Log.w(TAG, "Seek target is negative: $targetPos, clamping to 0")
                    0
                }
                duration > 0 && targetPos > duration -> {
                    Log.w(TAG, "Seek target exceeds duration: $targetPos > $duration, clamping to duration")
                    duration.toInt()
                }
                else -> targetPos.toInt()
            }
            
            // 用绝对定位代替相对定位，看看是否能解决倍数问题
            MPVLib.command(arrayOf("seek", safeTargetPos.toString(), "absolute"))
            Log.d(TAG, "Seek by: $seconds, position before: $currentPos, target: $targetPos, safe target: $safeTargetPos")
            Log.d("SEEK_DEBUG", "PlaybackEngine.seekBy executed: offset=$seconds, posBefore=$currentPos, targetPos=$targetPos, safeTargetPos=$safeTargetPos")
            
            // 异步检查seek后的位置
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val newPos = MPVLib.getPropertyDouble("time-pos")
                    val actualDiff = newPos - currentPos
                    Log.d("SEEK_DEBUG", "PlaybackEngine.seekBy RESULT: requested=$seconds, actualDiff=$actualDiff, posAfter=$newPos")
                } catch (e: Exception) {
                    Log.e("SEEK_DEBUG", "Failed to check position after seek", e)
                }
            }, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Seek by failed", e)
            eventCallback.onError("快进失败: ${e.message}")
        }
    }

    /**
     * 设置播放速度
     */
    fun setSpeed(speed: Double) {
        currentSpeed = speed
        MPVLib.setPropertyDouble("speed", speed)
        Log.d(TAG, "Speed set to: $speed")
    }

    /**
     * 切换解码器
     */
    fun setHardwareDecoding(enabled: Boolean) {
        isHardwareDecoding = enabled
        val hwdec = if (enabled) "auto" else "no"
        MPVLib.setPropertyString("hwdec", hwdec)
        Log.d(TAG, "Hardware decoding: $enabled")
    }

    /**
     * 设置音轨
     */
    fun setAudioTrack(trackId: Int) {
        MPVLib.setPropertyInt("aid", trackId)
        Log.d(TAG, "Audio track set to: $trackId")
    }

    /**
     * 获取章节列表
     */
    fun getChapters(): List<Pair<String, Double>> {
        val chapters = mutableListOf<Pair<String, Double>>()
        try {
            val chapterCount = MPVLib.getPropertyInt("chapter-list/count") ?: 0
            for (i in 0 until chapterCount) {
                val title = MPVLib.getPropertyString("chapter-list/$i/title") ?: "章节 ${i + 1}"
                val time = MPVLib.getPropertyDouble("chapter-list/$i/time") ?: 0.0
                chapters.add(Pair(title, time))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get chapters", e)
        }
        return chapters
    }

    /**
     * 应用 Anime4K 着色器
     */
    fun applyShaders(shaderChain: String) {
        try {
            MPVLib.setOptionString("glsl-shaders", shaderChain)
            Log.d(TAG, "Applied shaders: $shaderChain")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply shaders", e)
            eventCallback.onError("应用着色器失败: ${e.message}")
        }
    }

    /**
     * 更新播放进度
     */
    private fun updateProgress() {
        try {
            currentPosition = MPVLib.getPropertyDouble("time-pos") ?: 0.0
            duration = MPVLib.getPropertyDouble("duration") ?: 0.0

            if (duration > 0) {
                eventCallback.onProgressUpdate(currentPosition, duration)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update progress", e)
        }
    }

    /**
     * 销毁播放引擎
     */
    fun destroy() {
        Log.d(TAG, "Destroying PlaybackEngine")
        
        // 立即停止进度更新，防止内存泄漏
        handler.removeCallbacks(updateProgressRunnable)
        
        try {
            MPVLib.removeObserver(this)
            MPVLib.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying MPV", e)
        }

        surfaceHolder = null
        isInitialized = false
        
        Log.d(TAG, "PlaybackEngine destroyed - all handlers removed")
    }

    // ========== SurfaceHolder.Callback ==========

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "========== Surface Created ==========")
        Log.d(TAG, "Surface: ${holder.surface}")
        Log.d(TAG, "Surface valid: ${holder.surface?.isValid}")
        Log.d(TAG, "wasPlayingBeforeSurfaceDestroyed: $wasPlayingBeforeSurfaceDestroyed")
        
        try {
            MPVLib.attachSurface(holder.surface)
            Log.d(TAG, "✓ Surface attached to MPV")
            
            // ⚠️ 关键修复：Surface重建后恢复视频渲染
            // 问题：从文件选择器、锁屏等返回时Surface会重建，需要正确恢复播放
            // 特别处理：如果有外挂字幕，需要更长的延迟等待libass初始化完成
            handler.postDelayed({
                try {
                    // 检查是否有字幕轨道
                    val trackCount = MPVLib.getPropertyInt("track-list/count") ?: 0
                    val currentSid = MPVLib.getPropertyInt("sid") ?: -1
                    val hasSubtitle = currentSid > 0
                    
                    // 如果有字幕，需要额外延迟等待字幕系统初始化
                    val subtitleDelay = if (hasSubtitle) 200L else 0L
                    Log.d(TAG, "检测到字幕: $hasSubtitle (轨道数: $trackCount, sid: $currentSid)")
                    if (subtitleDelay > 0) {
                        Log.d(TAG, "字幕已加载，等待额外 ${subtitleDelay}ms 让libass初始化...")
                        Thread.sleep(subtitleDelay)
                    }
                    
                    Log.d(TAG, "--- 开始恢复视频渲染 ---")
                    
                    // 检查是否有视频正在播放
                    val currentPos = MPVLib.getPropertyDouble("time-pos")
                    val duration = MPVLib.getPropertyDouble("duration")
                    val paused = MPVLib.getPropertyBoolean("pause")
                    val hwdec = MPVLib.getPropertyString("hwdec-current")
                    
                    Log.d(TAG, "MPV状态检查:")
                    Log.d(TAG, "  - time-pos: $currentPos")
                    Log.d(TAG, "  - duration: $duration")
                    Log.d(TAG, "  - paused: $paused")
                    Log.d(TAG, "  - hwdec: $hwdec")
                    
                    val hasVideo = currentPos != null && currentPos >= 0.0
                    
                    if (hasVideo) {
                        Log.d(TAG, "✓ 检测到视频正在播放 (position: $currentPos)")
                        
                        // 🔥 完全重新加载视频文件以重新绑定Surface
                        Log.d(TAG, "🔥 完全重新加载视频文件以重新绑定Surface")
                        
                        // 保存所有状态
                        val savedPos = currentPos
                        val savedPaused = paused
                        val savedPath = currentFilePath
                        
                        Log.d(TAG, "  保存状态: path=$savedPath, pos=$savedPos, paused=$savedPaused")
                        
                        if (savedPath != null) {
                            // 1. 停止当前播放
                            Log.d(TAG, "  1. 停止当前播放")
                            MPVLib.command(arrayOf("stop"))
                            Thread.sleep(100)
                            
                            // 2. 重新加载文件
                            Log.d(TAG, "  2. 重新加载文件: $savedPath")
                            MPVLib.command(arrayOf("loadfile", savedPath))
                            Thread.sleep(800)
                            
                            // 2.5. 重新添加外部字幕
                            if (externalSubtitlePaths.isNotEmpty()) {
                                Log.d(TAG, "  2.5. 重新添加 ${externalSubtitlePaths.size} 个外部字幕")
                                for ((index, subtitlePath) in externalSubtitlePaths.withIndex()) {
                                    try {
                                        Log.d(TAG, "     [$index] Re-adding: $subtitlePath")
                                        val flag = if (index == 0) "select" else "append"
                                        MPVLib.command(arrayOf("sub-add", subtitlePath, flag))
                                        Log.d(TAG, "     ✓ Re-added with flag: $flag")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "     ❌ Failed to re-add subtitle: $subtitlePath", e)
                                    }
                                }
                            }
                            Thread.sleep(300)
                            
                            // 3. Seek到保存位置
                            Log.d(TAG, "  3. Seek到保存位置: $savedPos")
                            MPVLib.command(arrayOf("seek", savedPos.toString(), "absolute", "exact"))
                            Thread.sleep(300)
                            
                            // 4. 启用字幕
                            if (hasSubtitle || externalSubtitlePaths.isNotEmpty()) {
                                Log.d(TAG, "  4. 启用字幕可见性")
                                MPVLib.setPropertyBoolean("sub-visibility", true)
                            }
                            
                            // 5. 恢复播放
                            if (wasPlayingBeforeSurfaceDestroyed) {
                                Log.d(TAG, "  5. 恢复播放状态")
                                MPVLib.setPropertyBoolean("pause", false)
                                isPlaying = true
                            } else {
                                Log.d(TAG, "  5. 保持暂停状态")
                                MPVLib.setPropertyBoolean("pause", true)
                                isPlaying = false
                            }
                            
                            // 🔧 黑屏恢复：强制渲染重新初始化
                            Log.d(TAG, "  6. 黑屏恢复：触发渲染管道重新初始化...")
                            Thread {
                                try {
                                    Thread.sleep(400)
                                    
                                    // 检查VO状态
                                    val voConfigured = MPVLib.getPropertyString("vo-configured")
                                    Log.d(TAG, "     VO状态: $voConfigured")
                                    
                                    if (voConfigured != "yes") {
                                        Log.d(TAG, "     ⚠ VO未配置，强制初始化")
                                        MPVLib.setPropertyString("vo", "gpu")
                                        Thread.sleep(100)
                                    }
                                    
                                    // 暂停/继续周期
                                    val currentPause = MPVLib.getPropertyBoolean("pause") ?: false
                                    Log.d(TAG, "     执行暂停/继续刷新周期...")
                                    
                                    if (!currentPause) {
                                        MPVLib.setPropertyBoolean("pause", true)
                                        Thread.sleep(150)
                                        MPVLib.setPropertyBoolean("pause", false)
                                        Log.d(TAG, "     ✓ 暂停→继续")
                                    } else {
                                        MPVLib.setPropertyBoolean("pause", false)
                                        Thread.sleep(150)
                                        MPVLib.setPropertyBoolean("pause", true)
                                        Log.d(TAG, "     ✓ 继续→暂停")
                                    }
                                    
                                    Thread.sleep(200)
                                    
                                    // 触发video-scale更新
                                    val currentScale = MPVLib.getPropertyDouble("video-scale") ?: 1.0
                                    MPVLib.setPropertyDouble("video-scale", currentScale)
                                    Log.d(TAG, "     ✓ 触发video-scale更新")
                                    
                                } catch (e: Exception) {
                                    Log.e(TAG, "     ❌ 黑屏恢复失败", e)
                                }
                            }.start()
                            
                        } else {
                            Log.e(TAG, "  ❌ 无法重新加载：文件路径为空")
                        }
                        
                        // 验证恢复结果
                        handler.postDelayed({
                            val newHwdec = MPVLib.getPropertyString("hwdec-current")
                            val newPaused = MPVLib.getPropertyBoolean("pause")
                            val newPos = MPVLib.getPropertyDouble("time-pos")
                            Log.d(TAG, "✅ 恢复完成 - hwdec: $newHwdec, paused: $newPaused, pos: $newPos")
                        }, 500)
                        
                    } else {
                        Log.d(TAG, "⚠ 未检测到视频，跳过帧恢复")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 恢复视频帧失败", e)
                    Log.e(TAG, "异常堆栈: ${e.stackTraceToString()}")
                    // 降级方案：尝试简单的暂停/继续来触发重绘
                    try {
                        Log.d(TAG, "尝试降级方案...")
                        if (wasPlayingBeforeSurfaceDestroyed) {
                            MPVLib.setPropertyBoolean("pause", false)
                            isPlaying = true
                        }
                    } catch (e2: Exception) {
                        Log.e(TAG, "❌ 降级方案也失败", e2)
                    }
                }
            }, 100)
            
            // 通知Activity Surface已准备完成
            handler.post {
                eventCallback.onSurfaceReady()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Surface attach 失败", e)
        }
        
        Log.d(TAG, "========================================")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "========== Surface Changed ==========")
        Log.d(TAG, "尺寸: ${width}x${height}, 格式: $format")
        try {
            MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
            Log.d(TAG, "✓ Surface尺寸已更新到MPV")
            
            // 额外诊断：检查VO状态
            handler.postDelayed({
                try {
                    val voDriver = MPVLib.getPropertyString("vo-configured")
                    val currentVo = MPVLib.getPropertyString("current-vo")
                    Log.d(TAG, "VO诊断: configured=$voDriver, current=$currentVo")
                } catch (e: Exception) {
                    Log.w(TAG, "无法获取VO状态", e)
                }
            }, 50)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 设置Surface尺寸失败", e)
        }
        Log.d(TAG, "========================================")
    }

    // 保存Surface销毁前的播放状态
    private var wasPlayingBeforeSurfaceDestroyed = false

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "========== Surface Destroyed ==========")
        Log.d(TAG, "当前播放状态: isPlaying=$isPlaying")
        try {
            // ⚠️ 关键：在Surface销毁前保存播放状态
            // 因为detachSurface()会导致MPV暂停，从而触发isPlaying变为false
            wasPlayingBeforeSurfaceDestroyed = isPlaying
            Log.d(TAG, "✓ 保存播放状态: wasPlayingBeforeSurfaceDestroyed=$wasPlayingBeforeSurfaceDestroyed")
            
            MPVLib.detachSurface()
            Log.d(TAG, "✓ Surface已从MPV分离")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Surface分离失败", e)
        }
        Log.d(TAG, "========================================")
    }

    // ========== MPVLib.EventObserver ==========

    override fun eventProperty(property: String) {
        Log.d(TAG, "Event property: $property")
        
        // 监听轨道列表变化（参考 mpvKt 实现）
        if (property == "track-list") {
            Log.d(TAG, "轨道列表已更新，刷新字幕和音轨信息")
            handler.post {
                // 通知UI更新轨道列表（如果需要）
                // 这里可以添加回调通知UI刷新
            }
        }
    }

    override fun eventProperty(property: String, value: Long) {
        Log.d(TAG, "Event property: $property = $value")
    }

    override fun eventProperty(property: String, value: Double) {
        Log.d(TAG, "Event property: $property = $value")
    }

    override fun eventProperty(property: String, value: Boolean) {
        Log.d(TAG, "Event property: $property = $value")

        // 监听暂停状态变化
        if (property == "pause") {
            isPlaying = !value
            handler.post {
                eventCallback.onPlaybackStateChanged(isPlaying)
            }
        }
    }

    override fun eventProperty(property: String, value: String) {
        Log.d(TAG, "Event property: $property = $value")
        
        // 监听字幕轨道变化（参考 mpvKt）
        when (property) {
            "sid" -> {
                val trackId = when (value) {
                    "auto" -> null
                    "no", "false" -> -1
                    else -> value.toIntOrNull()
                }
                if (trackId != null) {
                    Log.d(TAG, "当前字幕轨道已变更为: $trackId")
                }
            }
            "aid" -> {
                val trackId = when (value) {
                    "auto" -> null
                    "no", "false" -> -1
                    else -> value.toIntOrNull()
                }
                if (trackId != null) {
                    Log.d(TAG, "当前音轨已变更为: $trackId")
                }
            }
        }
    }

    override fun event(eventId: Int) {
        Log.d(TAG, "Event: $eventId")
        when (eventId) {
            6 -> { // MPV_EVENT_FILE_LOADED
                Log.d(TAG, "File loaded successfully")
                handler.post {
                    // 确保播放状态和MPV同步
                    try {
                        val isPaused = MPVLib.getPropertyBoolean("pause") ?: false
                        isPlaying = !isPaused
                        Log.d(TAG, "File loaded, playing state: $isPlaying")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get pause state: ${e.message}")
                        isPlaying = true  // 默认为播放状态
                    }
                    eventCallback.onFileLoaded()
                    
                    // 文件加载完成后，尝试启用字幕（此时轨道列表已就绪）
                    handler.postDelayed({
                        tryEnableFirstSubtitleTrack()
                    }, 300)  // 给一个小延迟确保轨道列表完全就绪
                }
            }
            7 -> { // MPV_EVENT_END_FILE
                Log.d(TAG, "End of file")
                handler.post {
                    isPlaying = false
                    eventCallback.onEndOfFile()
                }
            }
        }
    }

    /**
     * 获取音轨列表
     * @return List<Triple<轨道ID, 轨道名称, 是否当前轨道>>
     */
    fun getAudioTracks(): List<Triple<Int, String, Boolean>> {
        return try {
            val trackCount = MPVLib.getPropertyInt("track-list/count") ?: 0
            val currentTrackId = MPVLib.getPropertyInt("aid") ?: -1
            val tracks = mutableListOf<Triple<Int, String, Boolean>>()

            for (i in 0 until trackCount) {
                val type = MPVLib.getPropertyString("track-list/$i/type")
                if (type == "audio") {
                    val id = MPVLib.getPropertyInt("track-list/$i/id") ?: continue
                    val lang = MPVLib.getPropertyString("track-list/$i/lang") ?: "unknown"
                    val title = MPVLib.getPropertyString("track-list/$i/title") ?: ""
                    val name = if (title.isNotEmpty()) "$lang - $title" else lang
                    val isCurrent = (id == currentTrackId)
                    tracks.add(Triple(id, name, isCurrent))
                }
            }
            tracks
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get audio tracks", e)
            emptyList()
        }
    }

    /**
     * 选择音轨
     */
    fun selectAudioTrack(trackId: Int) {
        try {
            MPVLib.setPropertyInt("aid", trackId)
            Log.d(TAG, "Selected audio track: $trackId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to select audio track", e)
        }
    }

    /**
     * 获取字幕轨道列表
     * @return List<Triple<轨道ID, 轨道名称, 是否当前轨道>>
     */
    /**
     * 获取字幕轨道列表
     * 参考 mpvKt 实现
     * @return List<Triple<轨道ID, 轨道名称, 是否当前轨道>>
     */
    fun getSubtitleTracks(): List<Triple<Int, String, Boolean>> {
        return try {
            val trackCount = MPVLib.getPropertyInt("track-list/count") ?: 0
            // 获取当前字幕轨道（可能是字符串 "no" 或数字）
            val currentTrackIdStr = MPVLib.getPropertyString("sid") ?: "no"
            val currentTrackId = when (currentTrackIdStr) {
                "no", "false" -> -1
                "auto" -> null
                else -> currentTrackIdStr.toIntOrNull() ?: -1
            }
            
            Log.d(TAG, "===== getSubtitleTracks() called =====")
            Log.d(TAG, "track-list/count: $trackCount, current sid: $currentTrackIdStr (parsed as: $currentTrackId)")
            
            val tracks = mutableListOf<Triple<Int, String, Boolean>>()
            
            // 添加"关闭字幕"选项
            tracks.add(Triple(-1, "关闭字幕", currentTrackId == -1))

            for (i in 0 until trackCount) {
                val type = MPVLib.getPropertyString("track-list/$i/type")
                val rawId = MPVLib.getPropertyInt("track-list/$i/id")
                val lang = MPVLib.getPropertyString("track-list/$i/lang") ?: "unknown"
                val title = MPVLib.getPropertyString("track-list/$i/title") ?: ""
                
                Log.d(TAG, "Track[$i]: type=$type, rawId=$rawId, lang=$lang, title=$title")
                
                if (type == "sub") {
                    val id = rawId ?: continue
                    
                    // 构建显示名称（参考 mpvKt）
                    val name = if (title.isNotEmpty() && lang.isNotEmpty()) {
                        "#$id: $title ($lang)"
                    } else if (title.isNotEmpty()) {
                        "#$id: $title"
                    } else if (lang.isNotEmpty()) {
                        "#$id: $lang"
                    } else {
                        "#$id: 字幕轨道"
                    }
                    
                    val isCurrent = (id == currentTrackId)
                    tracks.add(Triple(id, name, isCurrent))
                    
                    Log.d(TAG, "  └─ Added to UI list: id=$id, name=$name, current=$isCurrent")
                }
            }
            
            Log.d(TAG, "===== Found ${tracks.size - 1} subtitle tracks, current=$currentTrackId =====")
            tracks
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get subtitle tracks", e)
            emptyList()
        }
    }

    /**
     * 选择字幕轨道
     * 参考 mpvKt 实现
     */
    fun selectSubtitleTrack(trackId: Int) {
        try {
            if (trackId == -1) {
                // 关闭字幕
                MPVLib.setPropertyString("sid", "no")
                // 也可以使用：MPVLib.setPropertyBoolean("sub-visibility", false)
                Log.d(TAG, "Subtitle disabled")
            } else {
                MPVLib.setPropertyInt("sid", trackId)
                // 确保字幕可见
                MPVLib.setPropertyBoolean("sub-visibility", true)
                Log.d(TAG, "Selected subtitle track: $trackId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to select subtitle track", e)
        }
    }

    /**
     * 注册外部字幕路径，以便在视频重新加载时能够重新添加
     * 当用户选择了外部字幕文件后调用此方法
     */
    fun registerExternalSubtitle(subtitlePath: String) {
        if (!externalSubtitlePaths.contains(subtitlePath)) {
            externalSubtitlePaths.add(subtitlePath)
            Log.d(TAG, "Registered external subtitle: $subtitlePath (total: ${externalSubtitlePaths.size})")
        }
    }

    /**
     * 清除外部字幕注册
     */
    fun clearExternalSubtitles() {
        externalSubtitlePaths.clear()
        Log.d(TAG, "Cleared all external subtitle registrations")
    }

    /**
     * 尝试启用第一个字幕轨道（异步，不抛出异常）
     */
    private fun tryEnableFirstSubtitleTrack() {
        try {
            val trackCount = MPVLib.getPropertyInt("track-list/count") ?: 0
            Log.d(TAG, "═══════════════════════════════════")
            Log.d(TAG, "尝试启用字幕轨道")
            Log.d(TAG, "总轨道数: $trackCount")
            
            var subtitleCount = 0
            for (i in 0 until trackCount) {
                val type = MPVLib.getPropertyString("track-list/$i/type")
                val id = MPVLib.getPropertyInt("track-list/$i/id")
                val lang = MPVLib.getPropertyString("track-list/$i/lang") ?: "unknown"
                val title = MPVLib.getPropertyString("track-list/$i/title") ?: ""
                
                Log.d(TAG, "轨道 $i: type=$type, id=$id, lang=$lang, title=$title")
                
                if (type == "sub") {
                    subtitleCount++
                    if (id != null && subtitleCount == 1) {
                        // 启用第一个字幕轨道
                        MPVLib.setPropertyInt("sid", id)
                        
                        // 确保字幕可见
                        MPVLib.setPropertyBoolean("sub-visibility", true)
                        
                        Log.d(TAG, "✓ 已启用字幕轨道: id=$id, lang=$lang, title=$title")
                        Log.d(TAG, "✓ 字幕可见性已设置为 true")
                        
                        // 检查设置是否生效
                        handler.postDelayed({
                            try {
                                val currentSid = MPVLib.getPropertyInt("sid")
                                val subVisible = MPVLib.getPropertyBoolean("sub-visibility")
                                Log.d(TAG, "验证字幕状态: sid=$currentSid, visible=$subVisible")
                            } catch (e: Exception) {
                                Log.w(TAG, "无法验证字幕状态", e)
                            }
                        }, 200)
                        
                        Log.d(TAG, "═══════════════════════════════════")
                        return
                    }
                }
            }
            
            if (subtitleCount == 0) {
                Log.d(TAG, "⚠ 视频中没有找到字幕轨道")
            } else {
                Log.d(TAG, "⚠ 找到 $subtitleCount 个字幕轨道但启用失败")
            }
            Log.d(TAG, "═══════════════════════════════════")
        } catch (e: Exception) {
            // 不抛出异常，只记录日志，避免影响视频播放
            Log.e(TAG, "启用字幕轨道失败 (非关键错误)", e)
            Log.d(TAG, "═══════════════════════════════════")
        }
    }

    /**
     * 设置字幕缩放比例（通用方法，支持所有字幕类型）
     * @param scale 缩放比例（默认1.0），范围 0.5-3.0
     */
    fun setSubtitleScale(scale: Double) {
        try {
            MPVLib.setPropertyDouble("sub-scale", scale)
            Log.d(TAG, "Subtitle scale set to: $scale")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set subtitle scale", e)
        }
    }

    /**
     * 获取当前字幕缩放比例
     */
    fun getSubtitleScale(): Double {
        return try {
            MPVLib.getPropertyDouble("sub-scale") ?: 1.0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get subtitle scale", e)
            1.0
        }
    }

    /**
     * 设置字幕垂直位置
     * @param position 0-100，0=顶部，100=底部（默认100）
     */
    fun setSubtitleVerticalPosition(position: Int) {
        try {
            MPVLib.setPropertyInt("sub-pos", position)
            Log.d(TAG, "Subtitle vertical position set to: $position")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set subtitle vertical position", e)
        }
    }

    /**
     * 获取字幕垂直位置
     */
    fun getSubtitleVerticalPosition(): Int {
        return try {
            MPVLib.getPropertyInt("sub-pos") ?: 100
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get subtitle vertical position", e)
            100
        }
    }

    /**
     * 设置字幕延迟
     * @param delay 延迟时间（秒），正数延迟，负数提前
     */
    fun setSubtitleDelay(delay: Double) {
        try {
            MPVLib.setPropertyDouble("sub-delay", delay)
            Log.d(TAG, "Subtitle delay set to: $delay seconds")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set subtitle delay", e)
        }
    }

    /**
     * 获取字幕延迟
     */
    fun getSubtitleDelay(): Double {
        return try {
            MPVLib.getPropertyDouble("sub-delay") ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get subtitle delay", e)
            0.0
        }
    }

    /**
     * 检查是否有章节
     */
    fun hasChapters(): Boolean {
        return try {
            val chapterCount = MPVLib.getPropertyInt("chapters") ?: 0
            chapterCount > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check chapters", e)
            false
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        try {
            MPVLib.setPropertyBoolean("pause", true)
            isPlaying = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause", e)
        }
    }
    
    /**
     * 开始播放
     */
    fun play() {
        try {
            MPVLib.setPropertyBoolean("pause", false)
            isPlaying = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play", e)
        }
    }
}
