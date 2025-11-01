package com.fam4k007.videoplayer.player

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import `is`.xyz.mpv.MPVLib
import java.lang.ref.WeakReference

/**
 * 手势处理管理器
 * 负责处理屏幕手势（左右滑动调节音量/亮度）
 */
class GestureHandler(
    private val contextRef: WeakReference<Context>,
    private val windowRef: WeakReference<android.view.Window>,
    private val callback: GestureCallback
) {

    companion object {
        private const val TAG = "GestureHandler"
        
        // 参考 mpvKt 的灵敏度设置
        private const val BRIGHTNESS_SENSITIVITY = 0.001f  // 亮度灵敏度
        private const val VOLUME_SENSITIVITY = 0.03f       // 音量灵敏度
        private const val MPV_VOLUME_SENSITIVITY = 0.02f   // MPV音量增强灵敏度
        private const val SEEK_SENSITIVITY = 0.15f         // 进度灵敏度
        
        // 音量增强配置
        private const val VOLUME_BOOST_CAP = 200  // MPV音量上限(200%)
        
        // 偏好设置键名
        private const val PREF_NAME = "player_preferences"
        private const val KEY_PRECISE_SEEKING = "precise_seeking"
    }

    interface GestureCallback {
        fun onGestureStart()
        fun onGestureEnd()
        fun onLongPressRelease()  // 长按松手时调用
        fun onSingleTap()
        fun onDoubleTap()
        fun onLongPress()
        fun onSeekGesture(seekSeconds: Int)  // 改为传递秒数
    }

    // 音量和亮度 - 使用原始值而不是0-10刻度
    private var audioManager: AudioManager? = null
    private var maxVolume = 0
    private var maxBrightness = 255
    
    // 当前实际值（直接使用系统值）
    private var currentVolume = 0          // 系统音量值（0-maxVolume）
    private var currentBrightness = 0.5f   // 亮度值（0.0-1.0）
    
    // MPV内部音量控制 (100-200%)
    private var currentMPVVolume = 100
    
    // 精确进度控制设置
    private var usePreciseSeeking = false
    
    // 保存进入播放器时的原始系统设置(退出时恢复)
    private var originalSystemVolume = -1
    private var originalSystemBrightness = -1f
    
    // 手势起始值（每次手势开始时记录）
    private var gestureStartVolume = 0
    private var gestureStartMPVVolume = 100
    private var gestureStartBrightness = 0.5f
    private var gestureStartY = 0f
    
    // 手势状态
    var isAdjusting = false
        private set
    
    // 长按状态
    private var isLongPressing = false

    // 指示器隐藏Handler
    private val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var brightnessHideRunnable: Runnable? = null
    private var volumeHideRunnable: Runnable? = null

    // 指示器视图
    private var brightnessIndicator: LinearLayout? = null
    private var volumeIndicator: LinearLayout? = null
    private var brightnessBar: View? = null
    private var volumeBar: View? = null
    private var brightnessText: TextView? = null
    private var volumeText: TextView? = null

    // 手势检测器
    val gestureDetector: GestureDetector by lazy {
        GestureDetector(contextRef.get(), GestureListener())
    }

    /**
     * 初始化
     */
    fun initialize() {
        val context = contextRef.get() ?: return
        val window = windowRef.get() ?: return
        
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        
        // 保存并获取当前系统音量
        currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        originalSystemVolume = currentVolume
        
        // 初始化MPV音量
        try {
            currentMPVVolume = MPVLib.getPropertyInt("volume") ?: 100
        } catch (e: Exception) {
            currentMPVVolume = 100
        }
        
        // 保存并获取当前系统亮度
        originalSystemBrightness = window.attributes.screenBrightness
        
        if (originalSystemBrightness < 0) {
            try {
                val systemBrightness = Settings.System.getInt(
                    context.contentResolver, 
                    Settings.System.SCREEN_BRIGHTNESS
                ).toFloat()
                currentBrightness = systemBrightness / maxBrightness
                originalSystemBrightness = currentBrightness
            } catch (e: Exception) {
                currentBrightness = 0.5f
                originalSystemBrightness = 0.5f
            }
        } else {
            currentBrightness = originalSystemBrightness
        }
        
        // 读取精确进度控制设置
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        usePreciseSeeking = prefs.getBoolean(KEY_PRECISE_SEEKING, false)
        
        Log.d(TAG, "Initialized - Volume: $currentVolume/$maxVolume, MPV Volume: $currentMPVVolume%, Brightness: $currentBrightness")
    }

    /**
     * 绑定指示器视图
     */
    fun bindIndicatorViews(
        brightnessIndicator: LinearLayout,
        volumeIndicator: LinearLayout,
        brightnessBar: View,
        volumeBar: View,
        brightnessText: TextView,
        volumeText: TextView
    ) {
        this.brightnessIndicator = brightnessIndicator
        this.volumeIndicator = volumeIndicator
        this.brightnessBar = brightnessBar
        this.volumeBar = volumeBar
        this.brightnessText = brightnessText
        this.volumeText = volumeText
    }

    /**
     * 处理触摸事件
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        val result = gestureDetector.onTouchEvent(event)
        
        // 触摸结束时重置并隐藏指示器
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            // 如果是长按松手，调用释放回调
            if (isLongPressing) {
                callback.onLongPressRelease()
                isLongPressing = false
            }
            
            // 重置手势起始值
            gestureStartY = 0f
            gestureStartVolume = currentVolume
            gestureStartMPVVolume = currentMPVVolume
            gestureStartBrightness = currentBrightness
            
            // 如果是长按倍速，触摸结束时恢复正常速度
            callback.onGestureEnd()
            
            // 延迟隐藏指示器
            hideIndicatorDelayed(brightnessIndicator, 1500)
            hideIndicatorDelayed(volumeIndicator, 1500)
            
            // 延迟重置手势标志
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                isAdjusting = false
            }, 100)
        }
        
        return result
    }

    /**
     * 计算新亮度值（纯计算，无副作用）
     * @param startY 手势开始Y坐标
     * @param currentY 当前Y坐标
     * @param baseValue 基础亮度值
     * @return 计算后的亮度值（0.01-1.0）
     */
    private fun calculateBrightness(startY: Float, currentY: Float, baseValue: Float): Float {
        val newBrightness = baseValue + ((startY - currentY) * BRIGHTNESS_SENSITIVITY)
        return newBrightness.coerceIn(0.01f, 1.0f)
    }

    /**
     * 调整亮度 - 使用 mpvKt 的算法
     * @param startY 手势开始的Y坐标
     * @param currentY 当前Y坐标
     */
    private fun adjustBrightness(startY: Float, currentY: Float) {
        if (gestureStartY == 0f) {
            gestureStartY = startY
            gestureStartBrightness = currentBrightness
        }
        
        // 计算新亮度值
        currentBrightness = calculateBrightness(gestureStartY, currentY, gestureStartBrightness)
        
        // 应用亮度
        try {
            windowRef.get()?.let { window ->
                val layoutParams = window.attributes
                layoutParams.screenBrightness = currentBrightness
                window.attributes = layoutParams
            }
            Log.d(TAG, "Brightness: ${(currentBrightness * 100).toInt()}%")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness", e)
        }
        
        showBrightnessIndicator()
    }

    /**
     * 计算系统音量（纯计算，无副作用）
     * @param startY 手势开始Y坐标
     * @param currentY 当前Y坐标
     * @param baseValue 基础音量值
     * @return 计算后的音量值（0-maxVolume）
     */
    private fun calculateSystemVolume(startY: Float, currentY: Float, baseValue: Int): Int {
        val newVolume = baseValue + ((startY - currentY) * VOLUME_SENSITIVITY).toInt()
        return newVolume.coerceIn(0, maxVolume)
    }

    /**
     * 计算MPV音量增强值（纯计算，无副作用）
     * @param startY 手势开始Y坐标
     * @param currentY 当前Y坐标
     * @param baseValue 基础MPV音量（100-200）
     * @return 计算后的MPV音量值（100-200）
     */
    private fun calculateMPVVolume(startY: Float, currentY: Float, baseValue: Int): Int {
        val newVolume = baseValue + ((startY - currentY) * MPV_VOLUME_SENSITIVITY).toInt()
        return newVolume.coerceIn(100, VOLUME_BOOST_CAP)
    }

    /**
     * 调整音量 - 使用 mpvKt 的算法（双层控制）
     * @param startY 手势开始的Y坐标
     * @param currentY 当前Y坐标
     */
    private fun adjustVolume(startY: Float, currentY: Float) {
        val dragAmount = gestureStartY - currentY
        
        // 判断是否需要使用MPV音量增强
        val isIncreasingBoost = currentVolume == maxVolume && currentMPVVolume - 100 < 100 && dragAmount > 0
        val isDecreasingBoost = currentVolume == maxVolume && currentMPVVolume > 100 && dragAmount < 0
        
        if (isIncreasingBoost || isDecreasingBoost) {
            // MPV音量增强模式（100-200%）
            if (gestureStartY == 0f) {
                gestureStartY = startY
                gestureStartMPVVolume = currentMPVVolume
            }
            
            // 计算新MPV音量
            currentMPVVolume = calculateMPVVolume(gestureStartY, currentY, gestureStartMPVVolume)
            
            // 应用MPV音量
            try {
                MPVLib.setPropertyInt("volume", currentMPVVolume)
                Log.d(TAG, "MPV Volume Boost: $currentMPVVolume%")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set MPV volume", e)
            }
        } else {
            // 系统音量模式（0-maxVolume）
            if (gestureStartY == 0f) {
                gestureStartY = startY
                gestureStartVolume = currentVolume
            }
            
            // 计算新系统音量
            currentVolume = calculateSystemVolume(gestureStartY, currentY, gestureStartVolume)
            
            // 应用系统音量
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                Log.d(TAG, "System Volume: $currentVolume/$maxVolume")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set system volume", e)
            }
            
            // 如果音量达到最大且MPV增强不是100%，重置MPV音量
            if (currentVolume == maxVolume && currentMPVVolume != 100) {
                currentMPVVolume = 100
                try {
                    MPVLib.setPropertyInt("volume", 100)
                    Log.d(TAG, "Reset MPV volume to 100%")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reset MPV volume", e)
                }
            }
        }
        
        showVolumeIndicator()
    }

    /**
     * 显示亮度指示器
     */
    private fun showBrightnessIndicator() {
        brightnessIndicator?.visibility = View.VISIBLE
        
        val context = contextRef.get() ?: return
        val maxHeight = 120 * context.resources.displayMetrics.density
        
        // 直接使用百分比计算高度
        val barHeight = (maxHeight * currentBrightness).toInt()
        
        brightnessBar?.layoutParams?.height = barHeight
        brightnessBar?.requestLayout()
        brightnessText?.text = "${(currentBrightness * 100).toInt()}"
    }

    /**
     * 显示音量指示器
     */
    private fun showVolumeIndicator() {
        volumeIndicator?.visibility = View.VISIBLE
        
        val context = contextRef.get() ?: return
        val maxHeight = 120 * context.resources.displayMetrics.density
        
        if (currentMPVVolume > 100) {
            // MPV音量增强模式: 显示100-200%
            val percentage = ((currentMPVVolume - 100) / 100f).coerceIn(0f, 1f)
            val barHeight = (maxHeight * percentage).toInt()
            volumeBar?.layoutParams?.height = barHeight
            volumeBar?.requestLayout()
            volumeText?.text = "${currentMPVVolume}%"
        } else {
            // 系统音量模式: 显示百分比
            val percentage = currentVolume.toFloat() / maxVolume
            val barHeight = (maxHeight * percentage).toInt()
            volumeBar?.layoutParams?.height = barHeight
            volumeBar?.requestLayout()
            volumeText?.text = "${(percentage * 100).toInt()}"
        }
    }

    private fun hideIndicatorDelayed(indicator: LinearLayout?, delayMillis: Long) {
        hideHandler.postDelayed({
            indicator?.visibility = View.GONE
        }, delayMillis)
    }

    /**
     * 计算Seek值（纯计算，无副作用）
     * 固定步进：每100px滑动 = 5秒
     * @param deltaX 水平滑动距离
     * @return seek秒数
     */
    private fun calculateSeek(deltaX: Float): Int {
        return (deltaX / 100f * 5.0).toInt()
    }

    /**
     * 手势类型枚举
     */
    private enum class GestureType {
        NONE,
        BRIGHTNESS,  // 左侧上下滑动
        VOLUME,      // 右侧上下滑动
        SEEK         // 中间左右滑动
    }

    /**
     * 手势监听器
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private var gestureType = GestureType.NONE
        private var initialX = 0f
        private var initialY = 0f
        
        // 缓存屏幕尺寸，避免每次 onScroll 都重新获取
        private var cachedScreenWidth = 0
        private var cachedScreenHeight = 0

        override fun onDown(e: MotionEvent): Boolean {
            initialX = e.x
            initialY = e.y
            gestureType = GestureType.NONE
            
            // 首次或屏幕尺寸变化时更新缓存（避免每次 onScroll 都获取）
            val context = contextRef.get()
            if (context != null && (cachedScreenWidth == 0 || cachedScreenHeight == 0)) {
                cachedScreenWidth = context.resources.displayMetrics.widthPixels
                cachedScreenHeight = context.resources.displayMetrics.heightPixels
                Log.d(TAG, "Screen size cached: ${cachedScreenWidth}x${cachedScreenHeight}")
            }
            
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isAdjusting) {
                callback.onSingleTap()
            }
            return true
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // 双击全屏响应 - 暂停/播放
            callback.onDoubleTap()
            return true
        }
        
        override fun onLongPress(e: MotionEvent) {
            // 长按全屏响应 - 触发倍速播放
            isLongPressing = true
            callback.onLongPress()
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (e1 == null) return false
            
            // 使用缓存的屏幕尺寸，避免每次都调用 context.resources.displayMetrics
            val screenWidth = cachedScreenWidth
            val screenHeight = cachedScreenHeight
            
            // 计算滑动距离
            val deltaX = e2.x - initialX
            val deltaY = e2.y - initialY
            
            // 首次判断手势类型（根据滑动方向和位置）
            if (gestureType == GestureType.NONE) {
                val absX = kotlin.math.abs(deltaX)
                val absY = kotlin.math.abs(deltaY)
                
                // 判断是横向还是纵向滑动（降低阈值，减少死区）
                if (absX > 20 || absY > 20) {  // 从30px降低到20px
                    if (absX > absY * 1.2f) {  // 从1.5降低到1.2，减少斜向滑动死区
                        // 横向滑动 - 简化进度条区域检测
                        // 只保护屏幕底部15%区域（进度条通常在底部）
                        if (initialY > screenHeight * 0.85f) {
                            // 在底部区域，可能是点击进度条，不响应横向手势
                            return false
                        }
                        gestureType = GestureType.SEEK
                    } else if (absY > absX * 1.2f) {  // 从1.5降低到1.2
                        // 纵向滑动 - 调整音量/亮度（参考 mpvKt：屏幕左半边=亮度，右半边=音量）
                        if (initialX < screenWidth / 2) {
                            gestureType = GestureType.BRIGHTNESS
                        } else {
                            gestureType = GestureType.VOLUME
                        }
                    }
                }
            }
            
            // 根据手势类型执行相应操作（使用新算法）
            when (gestureType) {
                GestureType.BRIGHTNESS -> {
                    isAdjusting = true
                    callback.onGestureStart()
                    adjustBrightness(initialY, e2.y)
                }
                GestureType.VOLUME -> {
                    isAdjusting = true
                    callback.onGestureStart()
                    adjustVolume(initialY, e2.y)
                }
                GestureType.SEEK -> {
                    isAdjusting = true
                    callback.onGestureStart()
                    // 使用计算方法获取seek值
                    val seekStep = calculateSeek(deltaX)
                    callback.onSeekGesture(seekStep)
                }
                GestureType.NONE -> {
                    // 还未确定手势类型
                }
            }
            
            return true
        }
    }

    /**
     * 获取精确进度控制设置
     */
    fun isPreciseSeekingEnabled(): Boolean = usePreciseSeeking
    
    /**
     * 设置精确进度控制
     */
    fun setPreciseSeeking(enabled: Boolean) {
        usePreciseSeeking = enabled
        val context = contextRef.get() ?: return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PRECISE_SEEKING, enabled).apply()
        Log.d(TAG, "Precise seeking ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 恢复原始系统设置(退出播放器时调用)
     */
    fun restoreOriginalSettings() {
        try {
            // 恢复原始音量
            if (originalSystemVolume >= 0) {
                try {
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalSystemVolume, 0)
                    Log.d(TAG, "Restored original volume: $originalSystemVolume")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore original volume", e)
                }
            }
            
            // 恢复原始亮度
            if (originalSystemBrightness >= 0) {
                try {
                    windowRef.get()?.let { window ->
                        val layoutParams = window.attributes
                        layoutParams.screenBrightness = originalSystemBrightness
                        window.attributes = layoutParams
                        Log.d(TAG, "Restored original brightness: $originalSystemBrightness")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore original brightness", e)
                }
            }
            
            // 恢复MPV音量到100%
            try {
                MPVLib.setPropertyInt("volume", 100)
                Log.d(TAG, "Restored MPV volume to 100%")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore MPV volume", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during restoreOriginalSettings", e)
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // 移除所有 pending callbacks，防止内存泄漏
        brightnessHideRunnable?.let { hideHandler.removeCallbacks(it) }
        volumeHideRunnable?.let { hideHandler.removeCallbacks(it) }
        
        // 清空引用
        audioManager = null
        brightnessIndicator = null
        volumeIndicator = null
        brightnessBar = null
        volumeBar = null
        brightnessText = null
        volumeText = null
        
        Log.d(TAG, "Cleanup completed - all handlers cleared")
    }
}
