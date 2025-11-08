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
            val tracks = MPVLib.getPropertyString("track-list")
                ?.split(",")
                ?.filter { it.contains("audio") }
                ?: emptyList()

            if (tracks.isEmpty()) {
                DialogUtils.showToastShort(activity, "没有可用的音频轨道")
                return
            }

            val trackNames = tracks.mapIndexed { index, _ -> "音轨 ${index + 1}" }
            val currentTrack = MPVLib.getPropertyInt("aid") ?: 1
            val currentSelection = (currentTrack - 1).coerceIn(0, trackNames.size - 1)

            val btnAudioTrack = activity.findViewById<ImageView>(R.id.btnAudioTrack)
            showPopupDialog(
                btnAudioTrack,
                trackNames,
                currentSelection,
                showAbove = true,
                useFixedHeight = true,
                showScrollHint = false
            ) { position ->
                MPVLib.setPropertyInt("aid", position + 1)
                DialogUtils.showToastShort(activity, "已切换到音轨 ${position + 1}")
                Log.d(TAG, "Audio track changed to: ${position + 1}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show audio track dialog", e)
            DialogUtils.showToastShort(activity, "获取音频轨道失败")
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

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
        dialog.setContentView(R.layout.dialog_popup_menu)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerViewPopup)

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = PopupMenuAdapter(items, selectedPosition) { position ->
            onItemClick(position)
            dialog.dismiss()
        }

        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        val layoutParams = window?.attributes
        layoutParams?.gravity = android.view.Gravity.START or
                (if (showAbove) android.view.Gravity.TOP else android.view.Gravity.BOTTOM)

        val displayMetrics = activity.resources.displayMetrics
        val dialogWidth = (displayMetrics.widthPixels * 0.3).toInt()
                .coerceIn((150 * displayMetrics.density).toInt(), (300 * displayMetrics.density).toInt())

        val dialogHeight = if (useFixedHeight) {
            (250 * displayMetrics.density).toInt()
        } else {
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        }

        layoutParams?.width = dialogWidth
        layoutParams?.height = dialogHeight

        val marginLeftDp = 10
        val marginLeftPx = (marginLeftDp * displayMetrics.density).toInt()
        layoutParams?.x = location[0] - marginLeftPx

        if (showAbove) {
            val marginBottomDp = 60
            val marginBottomPx = (marginBottomDp * displayMetrics.density).toInt()
            layoutParams?.y = location[1] - dialogHeight - marginBottomPx
        } else {
            val marginTopDp = 60
            val marginTopPx = (marginTopDp * displayMetrics.density).toInt()
            layoutParams?.y = location[1] + anchorView.height + marginTopPx
        }

        window?.attributes = layoutParams
        dialog.setCanceledOnTouchOutside(true)
        
        // 追踪Dialog
        activeDialogs.add(dialog)
        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }
        
        dialog.show()
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
            showAbove = true,
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

        val items = listOf("章节", "截图", "播放列表")
        val btnMore = activity.findViewById<ImageView>(R.id.btnMore)

        showPopupDialog(
            btnMore,
            items,
            selectedPosition = -1,
            showAbove = true,
            useFixedHeight = false,
            showScrollHint = false
        ) { position ->
            when (position) {
                0 -> showChapterDialog()
                1 -> (activity as? MoreOptionsCallback)?.onScreenshot()
                2 -> (activity as? MoreOptionsCallback)?.onShowPlaylist()
            }
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
                showAbove = true,
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
            showAbove = true,
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
