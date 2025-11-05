package com.fam4k007.videoplayer.danmaku

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.widget.Toast
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDisplayer.DANMAKU_STYLE_STROKEN
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.ui.widget.DanmakuView
import java.io.File
import kotlin.math.max

/**
 * 弹幕视图 - 参考 DanDanPlay 实现
 */
class DanmakuPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DanmakuView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "DanmakuPlayerView"
        private const val DANMU_MAX_TEXT_SIZE = 2f
        private const val DANMU_MAX_TEXT_ALPHA = 1f
        private const val DANMU_MAX_TEXT_SPEED = 2.5f
        private const val DANMU_MAX_TEXT_STROKE = 20f
    }

    private val danmakuContext = DanmakuContext.create()
    private val danmakuLoader = BiliDanmakuLoader.instance()

    // 当前加载的弹幕文件路径
    private var currentDanmakuPath: String? = null

    init {
        // 显示 FPS（调试用）
        showFPS(DanmakuConfig.isDebug)

        initDanmakuContext()

        setCallback(object : DrawHandler.Callback {
            override fun drawingFinished() {
            }

            override fun danmakuShown(danmaku: BaseDanmaku?) {
            }

            override fun prepared() {
                // 弹幕准备完成
                Log.d(TAG, "Danmaku prepared")
            }

            override fun updateTimer(timer: DanmakuTimer?) {
                // 定时器更新回调
            }
        })
    }

    /**
     * 初始化弹幕上下文
     */
    private fun initDanmakuContext() {
        // 设置禁止重叠 - 与 DanDanPlay 完全一致
        val overlappingPair: MutableMap<Int, Boolean> = HashMap()
        overlappingPair[BaseDanmaku.TYPE_SCROLL_LR] = true
        overlappingPair[BaseDanmaku.TYPE_SCROLL_RL] = true
        overlappingPair[BaseDanmaku.TYPE_FIX_TOP] = true
        overlappingPair[BaseDanmaku.TYPE_FIX_BOTTOM] = true

        // 弹幕更新方式：0=Choreographer（高刷适配）, 1=new Thread, 2=DrawHandler（稳定）
        val danmuUpdateMethod: Byte = if (DanmakuConfig.updateInChoreographer) 0 else 2

        danmakuContext.apply {
            // 合并重复弹幕
            isDuplicateMergingEnabled = true
            // 弹幕view开启绘制缓存
            enableDanmakuDrawingCache(true)
            // 设置禁止重叠
            preventOverlapping(overlappingPair)
            // 设置更新方式
            updateMethod = danmuUpdateMethod
        }

        // 应用所有样式设置
        updateDanmakuSize()
        updateDanmakuSpeed()
        updateDanmakuAlpha()
        updateDanmakuStroke()
        updateScrollDanmakuState()
        updateTopDanmakuState()
        updateBottomDanmakuState()
        updateMaxLine()
        updateMaxScreenNum()
    }

    /**
     * 加载弹幕文件
     */
    fun loadDanmaku(filePath: String): Boolean {
        try {
            Log.d(TAG, "Loading danmaku file: $filePath")
            
            val danmuFile = File(filePath)
            if (!danmuFile.exists()) {
                Log.e(TAG, "Danmaku file not exists: $filePath")
                Toast.makeText(context, "弹幕文件不存在", Toast.LENGTH_SHORT).show()
                return false
            }

            // 释放之前的弹幕
            releaseDanmaku()

            // 加载弹幕文件
            danmakuLoader.load(filePath)
            val dataSource = danmakuLoader.dataSource
            if (dataSource == null) {
                Log.e(TAG, "Failed to load danmaku data source")
                Toast.makeText(context, "弹幕加载失败", Toast.LENGTH_SHORT).show()
                return false
            }

            currentDanmakuPath = filePath

            // 创建解析器并准备
            val danmuParser = BiliDanmakuParser().apply {
                load(dataSource)
            }
            prepare(danmuParser, danmakuContext)

            Log.d(TAG, "Danmaku file loaded successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading danmaku", e)
            Toast.makeText(context, "弹幕加载异常: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /**
     * 开始播放弹幕
     */
    fun startDanmaku() {
        if (isPrepared) {
            start()
            Log.d(TAG, "Danmaku started")
        }
    }

    /**
     * 暂停弹幕
     */
    fun pauseDanmaku() {
        if (isPrepared) {
            pause()
            Log.d(TAG, "Danmaku paused")
        }
    }

    /**
     * 恢复弹幕
     */
    fun resumeDanmaku() {
        if (isPrepared) {
            resume()
            Log.d(TAG, "Danmaku resumed")
        }
    }

    /**
     * 同步弹幕进度
     */
    fun seekDanmaku(timeMs: Long) {
        if (isPrepared) {
            val adjustedTime = timeMs + DanmakuConfig.offsetTime
            seekTo(adjustedTime)
            Log.d(TAG, "Danmaku seeked to: $adjustedTime ms")
        }
    }

    /**
     * 释放弹幕资源
     */
    fun releaseDanmaku() {
        currentDanmakuPath = null
        hide()
        clear()
        clearDanmakusOnScreen()
        release()
        Log.d(TAG, "Danmaku released")
    }

    /**
     * 显示/隐藏弹幕
     */
    fun toggleDanmakuVisibility() {
        if (isShown) {
            hide()
            Log.d(TAG, "Danmaku hidden")
        } else {
            show()
            Log.d(TAG, "Danmaku shown")
        }
    }

    /**
     * 获取当前弹幕文件路径
     */
    fun getCurrentDanmakuPath(): String? = currentDanmakuPath

    // ==================== 样式更新方法 ====================

    fun updateDanmakuSize() {
        val progress = DanmakuConfig.size / 100f
        val size = progress * DANMU_MAX_TEXT_SIZE
        danmakuContext.setScaleTextSize(size)
        Log.d(TAG, "Danmaku size updated: $size")
    }

    fun updateDanmakuSpeed() {
        val progress = DanmakuConfig.speed / 100f
        var speed = DANMU_MAX_TEXT_SPEED * (1 - progress)
        speed = max(0.1f, speed)
        danmakuContext.setScrollSpeedFactor(speed)
        Log.d(TAG, "Danmaku speed updated: $speed")
    }

    fun updateDanmakuAlpha() {
        val progress = DanmakuConfig.alpha / 100f
        val alpha = progress * DANMU_MAX_TEXT_ALPHA
        danmakuContext.setDanmakuTransparency(alpha)
        Log.d(TAG, "Danmaku alpha updated: $alpha")
    }

    fun updateDanmakuStroke() {
        val progress = DanmakuConfig.stroke / 100f
        val stroke = progress * DANMU_MAX_TEXT_STROKE
        danmakuContext.setDanmakuStyle(DANMAKU_STYLE_STROKEN, stroke)
        Log.d(TAG, "Danmaku stroke updated: $stroke")
    }

    fun updateScrollDanmakuState() {
        danmakuContext.r2LDanmakuVisibility = DanmakuConfig.showScrollDanmaku
        Log.d(TAG, "Scroll danmaku visibility: ${DanmakuConfig.showScrollDanmaku}")
    }

    fun updateTopDanmakuState() {
        danmakuContext.ftDanmakuVisibility = DanmakuConfig.showTopDanmaku
        Log.d(TAG, "Top danmaku visibility: ${DanmakuConfig.showTopDanmaku}")
    }

    fun updateBottomDanmakuState() {
        danmakuContext.fbDanmakuVisibility = DanmakuConfig.showBottomDanmaku
        Log.d(TAG, "Bottom danmaku visibility: ${DanmakuConfig.showBottomDanmaku}")
    }

    fun updateMaxLine() {
        val danmuMaxLineMap: MutableMap<Int, Int?> = mutableMapOf()

        val scrollLine = DanmakuConfig.maxScrollLine
        val topLine = DanmakuConfig.maxTopLine
        val bottomLine = DanmakuConfig.maxBottomLine
        
        danmuMaxLineMap[BaseDanmaku.TYPE_SCROLL_LR] = if (scrollLine > 0) scrollLine else null
        danmuMaxLineMap[BaseDanmaku.TYPE_SCROLL_RL] = if (scrollLine > 0) scrollLine else null
        danmuMaxLineMap[BaseDanmaku.TYPE_FIX_TOP] = if (topLine > 0) topLine else null
        danmuMaxLineMap[BaseDanmaku.TYPE_FIX_BOTTOM] = if (bottomLine > 0) bottomLine else null
        
        danmakuContext.setMaximumLines(danmuMaxLineMap)
        Log.d(TAG, "Max lines updated - scroll:$scrollLine, top:$topLine, bottom:$bottomLine")
    }

    fun updateMaxScreenNum() {
        val maxNum = DanmakuConfig.maxScreenNum
        danmakuContext.setMaximumVisibleSizeInScreen(maxNum)
        Log.d(TAG, "Max screen number updated: $maxNum")
    }

    fun updateOffsetTime() {
        if (isPrepared) {
            seekDanmaku(currentTime)
        }
    }

    /**
     * 设置播放倍速（用于倍速播放时同步弹幕）
     */
    fun setPlaybackSpeed(speed: Float) {
        if (speed >= 1f) {
            danmakuContext.setSpeed(speed)
            Log.d(TAG, "Playback speed set to: $speed")
        }
    }
}
