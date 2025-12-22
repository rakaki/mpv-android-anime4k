package com.fam4k007.videoplayer.player

import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.manager.VideoThumbnailManager
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * 进度条缩略图预览助手
 * 处理拖动进度条时显示缩略图预览
 * 使用代理模式，包装原有的监听器
 */
class SeekBarThumbnailHelper(
    context: Context,
    private val seekBar: SeekBar,
    private val containerView: ViewGroup,
    private val thumbnailManager: VideoThumbnailManager,
    private val originalListener: SeekBar.OnSeekBarChangeListener? = null
) {
    
    private val contextRef = WeakReference(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var popupWindow: PopupWindow? = null
    private var thumbnailImageView: ImageView? = null
    private var thumbnailTimeText: TextView? = null
    
    private var isShowing = false
    private var videoDuration: Double = 0.0
    private var lastDisplayedSecond: Long = -1  // 记录上次显示的秒数
    
    private var extractJob: Job? = null
    
    companion object {
        private const val POPUP_WIDTH = 640  // 更大尺寸
        private const val POPUP_HEIGHT = 410 // 包含文字高度
        private const val TAG = "SeekBarThumbnail"
    }
    
    init {
        setupSeekBarListener()
    }
    
    /**
     * 设置进度条监听（使用代理模式包装原监听器）
     */
    private fun setupSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 先调用原监听器
                originalListener?.onProgressChanged(seekBar, progress, fromUser)
                
                // 然后处理缩略图显示
                if (fromUser && thumbnailManager.isThumbnailSupported()) {
                    // progress 现在就是秒数
                    val currentSecond = progress.toLong()
                    
                    // 只有当秒数变化时才更新缩略图
                    if (currentSecond != lastDisplayedSecond) {
                        lastDisplayedSecond = currentSecond
                        showThumbnail(currentSecond)
                    }
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 先调用原监听器
                originalListener?.onStartTrackingTouch(seekBar)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 先调用原监听器
                originalListener?.onStopTrackingTouch(seekBar)
                // 然后隐藏缩略图
                hideThumbnail()
            }
        })
    }
    
    /**
     * 显示缩略图
     * @param positionSec 视频位置（秒）
     */
    private fun showThumbnail(positionSec: Long) {
        if (!isShowing) {
            createPopupWindow()
            isShowing = true
        }
        
        // 更新时间文字
        thumbnailTimeText?.text = formatTime(positionSec.toDouble())
        
        // 加载缩略图
        loadThumbnail(positionSec)
    }
    
    /**
     * 创建弹窗，固定在屏幕中央
     */
    private fun createPopupWindow() {
        val context = contextRef.get() ?: return
        
        val contentView = LayoutInflater.from(context).inflate(
            R.layout.popup_seekbar_thumbnail,
            null
        )
        
        thumbnailImageView = contentView.findViewById(R.id.ivThumbnail)
        thumbnailTimeText = contentView.findViewById(R.id.tvThumbnailTime)
        
        // 计算居中位置
        val screenWidth = containerView.width
        val screenHeight = containerView.height
        val x = (screenWidth - POPUP_WIDTH) / 2
        val y = (screenHeight - POPUP_HEIGHT) / 2
        
        popupWindow = PopupWindow(
            contentView,
            POPUP_WIDTH,
            POPUP_HEIGHT,
            false
        ).apply {
            elevation = 8f
            setBackgroundDrawable(null)
        }
        
        popupWindow?.showAtLocation(containerView, Gravity.NO_GRAVITY, x, y)
    }
    
    /**
     * 加载缩略图
     * @param positionSec 视频位置（秒）
     */
    private fun loadThumbnail(positionSec: Long) {
        extractJob?.cancel()
        
        // 先尝试从缓存获取
        val cached = thumbnailManager.getThumbnailAt(positionSec)
        if (cached != null) {
            thumbnailImageView?.setImageBitmap(cached)
            return
        }
        
        // 缓存未命中，异步提取
        extractJob = scope.launch {
            val bitmap = thumbnailManager.extractThumbnailRealtime(positionSec)
            bitmap?.let {
                thumbnailImageView?.setImageBitmap(it)
            }
        }
    }
    
    /**
     * 隐藏缩略图
     */
    private fun hideThumbnail() {
        popupWindow?.dismiss()
        popupWindow = null
        thumbnailImageView = null
        thumbnailTimeText = null
        isShowing = false
        lastDisplayedSecond = -1  // 重置秒数记录
        
        extractJob?.cancel()
        extractJob = null
    }
    
    /**
     * 更新视频时长
     */
    fun updateDuration(duration: Double) {
        videoDuration = duration
    }
    
    /**
     * 格式化时间
     */
    private fun formatTime(seconds: Double): String {
        val totalSec = seconds.toInt()
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val secs = totalSec % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        hideThumbnail()
        scope.cancel()
    }
}
