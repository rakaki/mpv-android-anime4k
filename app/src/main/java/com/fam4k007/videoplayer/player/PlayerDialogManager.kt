package com.fam4k007.videoplayer.player

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.Anime4KManager
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.adapter.Anime4KModeAdapter
import com.fam4k007.videoplayer.danmaku.DanmakuConfig
import com.fam4k007.videoplayer.danmaku.DanmakuManager
import com.fam4k007.videoplayer.manager.PreferencesManager
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.ThemeManager
import `is`.xyz.mpv.MPVLib
import java.lang.ref.WeakReference

/**
 * 播放器对话框管理器
 * 负责所有对话框的显示和交互
 */
class PlayerDialogManager(
    private val activityRef: WeakReference<AppCompatActivity>,
    private val playbackEngine: PlaybackEngine,
    private val danmakuManager: DanmakuManager,
    private val anime4KManager: Anime4KManager,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "PlayerDialogManager"
    }

    private val context: Context?
        get() = activityRef.get()

    private var anime4KDialog: Dialog? = null
    
    // 追踪所有活动的Dialog，防止内存泄漏
    private val activeDialogs = mutableListOf<Dialog>()

    // 回调接口
    interface DialogCallback {
        fun onSpeedChanged(speed: Double)
        fun onAnime4KChanged(enabled: Boolean, mode: Anime4KManager.Mode, quality: Anime4KManager.Quality)
    }

    private var dialogCallback: DialogCallback? = null

    fun setCallback(callback: DialogCallback) {
        this.dialogCallback = callback
    }

    /**
     * 显示音频轨道选择对话框
     */
    fun showAudioTrackDialog() {
        val activity = activityRef.get() ?: return

        try {
            val audioTracks = playbackEngine.getAudioTracks()

            if (audioTracks.isEmpty()) {
                DialogUtils.showToastShort(activity, "没有可用的音频轨道")
                return
            }

            // 获取轨道名称列表
            val items = audioTracks.map { it.second }
            // 获取当前选中的轨道索引
            val currentTrackIndex = audioTracks.indexOfFirst { it.third }

            val btnAudioTrack = activity.findViewById<ImageView>(R.id.btnAudioTrack)
            showPopupDialog(
                btnAudioTrack,
                items,
                currentTrackIndex,
                showAbove = false,
                useFixedHeight = false,  // 改为自适应高度
                showScrollHint = false
            ) { position ->
                val trackId = audioTracks[position].first
                playbackEngine.selectAudioTrack(trackId)
                DialogUtils.showToastShort(activity, "已切换到: ${items[position]}")
                Log.d(TAG, "Audio track changed to: $trackId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show audio track dialog", e)
            DialogUtils.showToastShort(activity, "获取音频轨道失败")
        }
    }

    /**
     * 显示解码器选择对话框
     */
    fun showDecoderDialog() {
        val activity = activityRef.get() ?: return

        val items = listOf("硬件解码", "软件解码")
        val currentDecoder = preferencesManager.getHardwareDecoder()
        val currentSelection = if (currentDecoder) 0 else 1

        val btnDecoder = activity.findViewById<ImageView>(R.id.btnDecoder)
        showPopupDialog(
            btnDecoder,
            items,
            currentSelection,
            showAbove = false,
            useFixedHeight = false,
            showScrollHint = false
        ) { position ->
            val newDecoder = (position == 0)
            preferencesManager.setHardwareDecoder(newDecoder)
            playbackEngine.setHardwareDecoding(newDecoder)
            DialogUtils.showToastShort(activity, "已切换到${items[position]}")
            Log.d(TAG, "Decoder changed to: ${if (newDecoder) "hardware" else "software"}")
        }
    }

    /**
     * 显示通用弹出对话框
     */
    fun showPopupDialog(
        anchorView: View,
        items: List<String>,
        selectedPosition: Int = -1,
        showAbove: Boolean = false,
        useFixedHeight: Boolean = false,
        showScrollHint: Boolean = false,
        onItemClick: (Int) -> Unit
    ) {
        val activity = activityRef.get() ?: return

        val dialog = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        
        // 根据是否需要固定高度选择不同的布局文件
        val layoutRes = if (useFixedHeight) R.layout.dialog_popup_menu_fixed else R.layout.dialog_popup_menu
        val dialogView = activity.layoutInflater.inflate(layoutRes, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewPopup)

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.isVerticalScrollBarEnabled = false
        
        // 如果使用固定高度且需要显示滑动提示
        if (useFixedHeight && showScrollHint && items.size > 3) {
            val scrollHint = dialogView.findViewById<TextView>(R.id.scrollHint)
            scrollHint?.visibility = View.VISIBLE
        }

        val adapter = PopupMenuAdapter(items, selectedPosition) { position ->
            onItemClick(position)
            dialog.dismiss()
        }
        recyclerView.adapter = adapter

        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(true)

        // 获取锚点视图在屏幕上的位置
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]

        // 测量对话框尺寸
        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val dialogWidth = dialogView.measuredWidth.coerceAtLeast(anchorView.width)
        val dialogHeight = dialogView.measuredHeight

        // 计算对话框位置
        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        layoutParams?.x = anchorX + (anchorView.width - dialogWidth) / 2

        // 根据参数决定显示在上方还是下方
        layoutParams?.y = if (showAbove) {
            // 显示在按钮上方，不遮挡按钮
            anchorY - dialogHeight - 10
        } else {
            // 显示在按钮下方，不遮挡按钮
            anchorY + anchorView.height + 10
        }

        layoutParams?.width = dialogWidth
        layoutParams?.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams

        // 设置进场和出场动画
        window?.setWindowAnimations(R.style.PopupAnimation)
        
        dialog.show()
        
        // 追踪Dialog
        activeDialogs.add(dialog)
        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }
        
        // 如果有选中项且使用固定高度，自动滚动到选中位置
        if (selectedPosition >= 0 && useFixedHeight) {
            val nestedScrollView = dialogView.findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollViewPopup)
            nestedScrollView?.post {
                // 计算选中项的位置
                val itemHeight = 48.dpToPx() // 每个选项的高度
                val scrollViewHeight = nestedScrollView.height
                val targetY = selectedPosition * itemHeight - (scrollViewHeight / 2) + (itemHeight / 2)

                // 滚动到目标位置，让选中项居中
                nestedScrollView.smoothScrollTo(0, targetY.coerceAtLeast(0))
            }
        }
    }
    
    private fun Int.dpToPx(): Int {
        val activity = activityRef.get() ?: return this
        return (this * activity.resources.displayMetrics.density).toInt()
    }

    /**
     * 显示字幕选择对话框
     */
    fun showSubtitleDialog() {
        val activity = activityRef.get() ?: return

        val btnSubtitle = activity.findViewById<ImageView>(R.id.btnSubtitle)
        val menuItems = listOf("关闭字幕", "导入外部字幕", "切换字幕轨道", "字幕延迟", "字幕样式", "字幕杂项")

        showPopupDialog(
            btnSubtitle,
            menuItems,
            selectedPosition = -1,
            showAbove = false,
            useFixedHeight = true,
            showScrollHint = true
        ) { position ->
            when (position) {
                0 -> {
                    playbackEngine.setSubtitleTrack(0)
                    DialogUtils.showToastShort(activity, "字幕已关闭")
                }
                1 -> {
                    // 导入外部字幕的逻辑由Activity处理
                    (activity as? SubtitleDialogCallback)?.onImportSubtitle()
                }
                2 -> showSubtitleTrackDialog()
                3 -> showSubtitleDelayDialog()
                4 -> showSubtitleStyleDialog()
                5 -> showSubtitleMiscDialog()
            }
        }
    }

    /**
     * 显示字幕轨道切换对话框
     */
    private fun showSubtitleTrackDialog() {
        val activity = activityRef.get() ?: return

        try {
            val tracks = playbackEngine.getSubtitleTracks()
            if (tracks.isEmpty()) {
                DialogUtils.showToastShort(activity, "没有可用的字幕轨道")
                return
            }

            val currentTrack = playbackEngine.getCurrentSubtitleTrack()
            val btnSubtitle = activity.findViewById<ImageView>(R.id.btnSubtitle)
            
            val trackNames = tracks.map { it.second }

            showPopupDialog(
                btnSubtitle,
                trackNames,
                currentTrack,
                showAbove = true,
                useFixedHeight = true,
                showScrollHint = false
            ) { position ->
                playbackEngine.setSubtitleTrack(tracks[position].first)
                DialogUtils.showToastShort(activity, "已切换到: ${tracks[position].second}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show subtitle track dialog", e)
        }
    }

    /**
     * 显示字幕延迟调整对话框
     */
    fun showSubtitleDelayDialog() {
        val activity = activityRef.get() ?: return
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri() ?: return

        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_subtitle_delay)

        val etDelayValue = dialog.findViewById<EditText>(R.id.etDelayValue)
        val btnDelayPlus = dialog.findViewById<Button>(R.id.btnIncreaseDelay)
        val btnDelayMinus = dialog.findViewById<Button>(R.id.btnDecreaseDelay)
        val btnDelayReset = dialog.findViewById<Button>(R.id.btnResetDelay)

        val currentDelay = preferencesManager.getSubtitleDelay(videoUri.toString())
        etDelayValue.setText(String.format("%.2f", currentDelay))

        btnDelayPlus.setOnClickListener {
            val currentValue = etDelayValue.text.toString().toDoubleOrNull() ?: 0.0
            val newValue = currentValue + 0.1
            etDelayValue.setText(String.format("%.2f", newValue))
            playbackEngine.setSubtitleDelay(newValue)
            preferencesManager.setSubtitleDelay(videoUri.toString(), newValue)
        }

        btnDelayMinus.setOnClickListener {
            val currentValue = etDelayValue.text.toString().toDoubleOrNull() ?: 0.0
            val newValue = currentValue - 0.1
            etDelayValue.setText(String.format("%.2f", newValue))
            playbackEngine.setSubtitleDelay(newValue)
            preferencesManager.setSubtitleDelay(videoUri.toString(), newValue)
        }

        btnDelayReset.setOnClickListener {
            etDelayValue.setText("0.00")
            playbackEngine.setSubtitleDelay(0.0)
            preferencesManager.setSubtitleDelay(videoUri.toString(), 0.0)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)
        
        // 追踪Dialog
        activeDialogs.add(dialog)
        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }
        
        dialog.show()
    }

    /**
     * 显示字幕杂项设置对话框
     */
    fun showSubtitleMiscDialog() {
        val activity = activityRef.get() ?: return
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri() ?: return

        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_subtitle_misc)

        val seekBarScale = dialog.findViewById<SeekBar>(R.id.seekBarSize)
        val seekBarPos = dialog.findViewById<SeekBar>(R.id.seekBarPosition)
        val tvScaleValue = dialog.findViewById<TextView>(R.id.tvSizeValue)
        val tvPosValue = dialog.findViewById<TextView>(R.id.tvPositionValue)
        val btnReset = dialog.findViewById<Button>(R.id.btnReset)

        val uriString = videoUri.toString()
        val currentScale = (preferencesManager.getSubtitleScale(uriString) * 100).toInt()
        val currentPos = preferencesManager.getSubtitlePosition(uriString)

        seekBarScale.progress = currentScale
        seekBarPos.progress = currentPos
        tvScaleValue.text = "$currentScale%"
        tvPosValue.text = "$currentPos"

        seekBarScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvScaleValue.text = "$progress%"
                val scale = progress / 100.0
                playbackEngine.setSubtitleScale(scale)
                preferencesManager.setSubtitleScale(uriString, scale)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarPos.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPosValue.text = "$progress"
                playbackEngine.setSubtitlePosition(progress)
                preferencesManager.setSubtitlePosition(uriString, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnReset.setOnClickListener {
            seekBarScale.progress = 100
            seekBarPos.progress = 0
            playbackEngine.setSubtitleScale(1.0)
            playbackEngine.setSubtitlePosition(0)
            preferencesManager.setSubtitleScale(uriString, 1.0)
            preferencesManager.setSubtitlePosition(uriString, 0)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)
        
        // 追踪Dialog
        activeDialogs.add(dialog)
        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }
        
        dialog.show()
    }

    /**
     * 显示字幕样式设置对话框
     */
    fun showSubtitleStyleDialog() {
        val activity = activityRef.get() ?: return
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri() ?: return

        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_subtitle_style)

        val seekBarBorderSize = dialog.findViewById<SeekBar>(R.id.seekBarBorderSize)
        val tvBorderSizeValue = dialog.findViewById<TextView>(R.id.tvBorderSizeValue)
        val btnReset = dialog.findViewById<Button>(R.id.btnReset)

        val uriString = videoUri.toString()
        var currentBorderSize = preferencesManager.getSubtitleBorderSize(uriString).toDouble()

        seekBarBorderSize.progress = currentBorderSize.toInt()
        tvBorderSizeValue.text = "${currentBorderSize.toInt()}"

        seekBarBorderSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBorderSizeValue.text = "$progress"
                currentBorderSize = progress.toDouble()
                playbackEngine.setSubtitleBorderSize(progress)
                preferencesManager.setSubtitleBorderSize(uriString, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnReset.setOnClickListener {
            seekBarBorderSize.progress = 3
            playbackEngine.setSubtitleBorderSize(3)
            preferencesManager.setSubtitleBorderSize(uriString, 3)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)
        
        // 追踪Dialog
        activeDialogs.add(dialog)
        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }
        
        dialog.show()
    }

    /**
     * 显示播放速度选择对话框
     */
    fun showSpeedDialog(currentSpeed: Double) {
        val activity = activityRef.get() ?: return

        val speeds = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x", "2.5x", "3.0x")
        val speedValues = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0)
        val currentSelection = speedValues.indexOf(currentSpeed)

        val btnSpeed = activity.findViewById<ImageView>(R.id.btnSpeed)

        showPopupDialog(
            btnSpeed,
            speeds,
            currentSelection,
            showAbove = true,
            useFixedHeight = true,
            showScrollHint = true
        ) { position ->
            val newSpeed = speedValues[position]
            dialogCallback?.onSpeedChanged(newSpeed)
            DialogUtils.showToastShort(activity, "播放速度：${speeds[position]}")
        }
    }

    /**
     * 显示Anime4K模式选择对话框
     */
    fun showAnime4KModeDialog(currentMode: Anime4KManager.Mode) {
        val activity = activityRef.get() ?: return

        if (anime4KDialog != null && anime4KDialog!!.isShowing) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
        dialog.setContentView(R.layout.dialog_anime4k_mode)

        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val layoutParams = window?.attributes
        layoutParams?.gravity = android.view.Gravity.START or android.view.Gravity.TOP
        layoutParams?.x = 20
        layoutParams?.y = 100
        layoutParams?.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams?.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams?.flags = layoutParams?.flags?.or(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) ?: 0
        window?.attributes = layoutParams

        val rvMode = dialog.findViewById<RecyclerView>(R.id.rvAnime4KMode)
        val btnClose = dialog.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnAnime4KClose)

        val modes = listOf(
            Anime4KManager.Mode.OFF,
            Anime4KManager.Mode.A,
            Anime4KManager.Mode.B,
            Anime4KManager.Mode.C,
            Anime4KManager.Mode.A_PLUS,
            Anime4KManager.Mode.B_PLUS,
            Anime4KManager.Mode.C_PLUS
        )

        rvMode.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        rvMode.adapter = Anime4KModeAdapter(modes, currentMode) { mode ->
            val enabled = mode != Anime4KManager.Mode.OFF
            dialogCallback?.onAnime4KChanged(enabled, mode, Anime4KManager.Quality.BALANCED)
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        anime4KDialog = dialog
        
        // 追踪Dialog
        activeDialogs.add(dialog)
        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
            anime4KDialog = null
        }
        
        dialog.show()
    }

    /**
     * 显示更多选项对话框
     */
    fun showMoreOptionsDialog() {
        val activity = activityRef.get() ?: return

        // 获取当前视频URI以查询样式覆盖状态
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri()
        val assOverrideEnabled = videoUri?.let { 
            preferencesManager.isAssOverrideEnabled(it.toString())
        } ?: false

        // 动态显示样式覆盖状态
        val assOverrideText = if (assOverrideEnabled) "样式覆盖：开" else "样式覆盖：关"
        val items = listOf("章节", "截图", assOverrideText)
        val btnMore = activity.findViewById<ImageView>(R.id.btnMore)

        showPopupDialog(
            btnMore,
            items,
            selectedPosition = -1,
            showAbove = false,
            useFixedHeight = false,
            showScrollHint = false
        ) { position ->
            when (position) {
                0 -> showChapterDialog()
                1 -> (activity as? MoreOptionsCallback)?.onScreenshot()
                2 -> toggleAssOverride()  // 点击切换样式覆盖
            }
        }
    }

    /**
     * 切换ASS/SSA字幕样式覆盖
     */
    private fun toggleAssOverride() {
        val activity = activityRef.get() ?: return
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri() ?: return

        try {
            // 获取当前状态
            val currentState = preferencesManager.isAssOverrideEnabled(videoUri.toString())
            // 切换状态
            val newState = !currentState
            
            // 保存设置
            preferencesManager.setAssOverrideEnabled(videoUri.toString(), newState)
            
            // 立即应用到播放引擎
            playbackEngine.setAssOverride(newState)
            
            Log.d(TAG, "ASS override toggled: $newState")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle ASS override", e)
        }
    }

    /**
     * 显示章节选择对话框
     */
    fun showChapterDialog() {
        val activity = activityRef.get() ?: return

        try {
            val chapterCount = MPVLib.getPropertyInt("chapters") ?: 0
            if (chapterCount <= 0) {
                DialogUtils.showToastShort(activity, "此视频没有章节信息")
                return
            }

            val chapters = mutableListOf<String>()
            for (i in 0 until chapterCount) {
                val title = MPVLib.getPropertyString("chapter-list/$i/title") ?: "章节 ${i + 1}"
                chapters.add(title)
            }

            val currentChapter = MPVLib.getPropertyInt("chapter") ?: 0
            val btnMore = activity.findViewById<ImageView>(R.id.btnMore)

            showPopupDialog(
                btnMore,
                chapters,
                currentChapter,
                showAbove = false,
                useFixedHeight = true,
                showScrollHint = true
            ) { position ->
                MPVLib.setPropertyInt("chapter", position)
                DialogUtils.showToastShort(activity, "已跳转到: ${chapters[position]}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show chapter dialog", e)
        }
    }

    /**
     * 显示弹幕菜单对话框
     */
    fun showDanmakuDialog() {
        val activity = activityRef.get() ?: return

        val btnDanmaku = activity.findViewById<ImageView>(R.id.btnDanmaku)
        val menuItems = listOf(
            if (danmakuManager.isVisible()) "隐藏弹幕" else "显示弹幕",
            "导入弹幕",
            "弹幕设置"
        )

        showPopupDialog(
            btnDanmaku,
            menuItems,
            selectedPosition = -1,
            showAbove = false,
            useFixedHeight = false,
            showScrollHint = false
        ) { position ->
            when (position) {
                0 -> {
                    if (danmakuManager.isVisible()) {
                        danmakuManager.setVisibility(false)
                        DialogUtils.showToastShort(activity, "弹幕已隐藏")
                    } else {
                        danmakuManager.setVisibility(true)
                        DialogUtils.showToastShort(activity, "弹幕已显示")
                    }
                }
                1 -> (activity as? DanmakuDialogCallback)?.onImportDanmaku()
                2 -> showDanmakuSettingsDialog()
            }
        }
    }

    /**
     * 显示弹幕设置对话框
     */
    fun showDanmakuSettingsDialog() {
        val activity = activityRef.get() ?: return

        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_danmaku_settings)

        val seekBarSize = dialog.findViewById<SeekBar>(R.id.seekBarDanmakuSize)
        val seekBarSpeed = dialog.findViewById<SeekBar>(R.id.seekBarDanmakuSpeed)
        val seekBarAlpha = dialog.findViewById<SeekBar>(R.id.seekBarDanmakuAlpha)
        val seekBarStroke = dialog.findViewById<SeekBar>(R.id.seekBarDanmakuStroke)
        val tvSizeValue = dialog.findViewById<TextView>(R.id.tvDanmakuSizeValue)
        val tvSpeedValue = dialog.findViewById<TextView>(R.id.tvDanmakuSpeedValue)
        val tvAlphaValue = dialog.findViewById<TextView>(R.id.tvDanmakuAlphaValue)
        val tvStrokeValue = dialog.findViewById<TextView>(R.id.tvDanmakuStrokeValue)
        val btnReset = dialog.findViewById<Button>(R.id.btnReset)

        val defaultSize = 100
        val defaultSpeed = 100
        val defaultAlpha = 100
        val defaultStroke = 100

        seekBarSize.progress = defaultSize
        seekBarSpeed.progress = defaultSpeed
        seekBarAlpha.progress = defaultAlpha
        seekBarStroke.progress = defaultStroke

        tvSizeValue.text = "${defaultSize}%"
        tvSpeedValue.text = "${defaultSpeed}%"
        tvAlphaValue.text = "${defaultAlpha}%"
        tvStrokeValue.text = "${defaultStroke}%"

        fun updateResetButton() {
            val isAllDefault = seekBarSize.progress == defaultSize &&
                    seekBarSpeed.progress == defaultSpeed &&
                    seekBarAlpha.progress == defaultAlpha &&
                    seekBarStroke.progress == defaultStroke
            btnReset.isEnabled = !isAllDefault
        }
        updateResetButton()

        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSizeValue.text = "$progress%"
                danmakuManager.updateSize()
                updateResetButton()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSpeedValue.text = "$progress%"
                danmakuManager.updateSpeed()
                updateResetButton()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvAlphaValue.text = "$progress%"
                danmakuManager.updateAlpha()
                updateResetButton()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarStroke.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvStrokeValue.text = "$progress%"
                danmakuManager.updateStroke()
                updateResetButton()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnReset.setOnClickListener {
            seekBarSize.progress = defaultSize
            seekBarSpeed.progress = defaultSpeed
            seekBarAlpha.progress = defaultAlpha
            seekBarStroke.progress = defaultStroke

            danmakuManager.updateSize()
            danmakuManager.updateSpeed()
            danmakuManager.updateAlpha()
            danmakuManager.updateStroke()

            updateResetButton()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)
        
        // 追踪Dialog
        activeDialogs.add(dialog)
        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }
        
        dialog.show()
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        // 释放所有活动的Dialog
        activeDialogs.forEach { dialog ->
            try {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing dialog", e)
            }
        }
        activeDialogs.clear()
        
        // 释放anime4KDialog
        anime4KDialog?.dismiss()
        anime4KDialog = null
        
        // 清理回调
        dialogCallback = null
    }

    // 内部Adapter类
    private inner class PopupMenuAdapter(
        private val items: List<String>,
        private var selectedPosition: Int,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<PopupMenuAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val itemText: TextView = view.findViewById(R.id.itemText)
            val innerLayout: LinearLayout = view.findViewById(R.id.innerLayout)

            fun bind(position: Int) {
                val activity = activityRef.get() ?: return
                
                itemText.text = items[position]
                itemView.isClickable = true
                itemView.isFocusable = true
                itemView.background = null
                innerLayout.background = null

                if (selectedPosition == -1) {
                    itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
                } else {
                    val isSelected = position == selectedPosition
                    if (isSelected) {
                        itemText.setTextColor(
                            ThemeManager.getThemeColor(
                                activity,
                                com.google.android.material.R.attr.colorPrimary
                            )
                        )
                        itemText.setTypeface(null, android.graphics.Typeface.BOLD)
                    } else {
                        itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
                        itemText.setTypeface(null, android.graphics.Typeface.NORMAL)
                    }
                }

                itemView.setOnClickListener {
                    onItemClick(position)
                }
            }
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val activity = activityRef.get() ?: throw IllegalStateException("Activity is null")
            val view = activity.layoutInflater.inflate(R.layout.dialog_selection_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = items.size
    }
}

// 回调接口
interface SubtitleDialogCallback {
    fun onImportSubtitle()
}

interface DanmakuDialogCallback {
    fun onImportDanmaku()
}

interface MoreOptionsCallback {
    fun onScreenshot()
    fun onShowPlaylist()
}

interface VideoUriProvider {
    fun getVideoUri(): android.net.Uri?
}
