package com.fam4k007.videoplayer

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.fam4k007.videoplayer.manager.PreferencesManager
import com.fam4k007.videoplayer.utils.DialogUtils

/**
 * 播放设置页面
 */
class PlaybackSettingsActivity : BaseActivity() {

    private lateinit var switchPreciseSeeking: SwitchCompat
    private lateinit var tvPreciseSeekingDesc: TextView
    private lateinit var llSeekTimeContainer: LinearLayout
    private lateinit var tvSeekTimeValue: TextView
    private lateinit var seekBarLongPressSpeed: SeekBar
    private lateinit var tvLongPressSpeedValue: TextView
    
    // 快进时长选项
    private val seekTimeOptions = intArrayOf(3, 5, 10, 15, 20, 25, 30)
    
    // 设置管理器
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback_settings)
        
        // 初始化设置管理器
        preferencesManager = PreferencesManager.getInstance(this)
        
        // 设置Toolbar
        setupToolbar(R.id.toolbar, getString(R.string.playback_settings), showBackButton = true)
        
        // 初始化视图
        switchPreciseSeeking = findViewById(R.id.switchPreciseSeeking)
        tvPreciseSeekingDesc = findViewById(R.id.tvPreciseSeekingDesc)
        llSeekTimeContainer = findViewById(R.id.llSeekTimeContainer)
        tvSeekTimeValue = findViewById(R.id.tvSeekTimeValue)
        seekBarLongPressSpeed = findViewById(R.id.seekBarLongPressSpeed)
        tvLongPressSpeedValue = findViewById(R.id.tvLongPresSpeedValue)
        
        // 加载当前设置
        loadSettings()
        
        // 设置监听器
        setupListeners()
    }
    
    /**
     * 加载当前设置
     */
    private fun loadSettings() {
        val preciseSeeking = preferencesManager.isPreciseSeekingEnabled()
        switchPreciseSeeking.isChecked = preciseSeeking
        
        // 加载快进/快退时长设置
        val seekTime = preferencesManager.getSeekTime()
        updateSeekTimeDisplay(seekTime)
        
        // 加载长按倍速设置
        val longPressSpeed = preferencesManager.getLongPressSpeed()
        val speedProgress = when (longPressSpeed) {
            1.5f -> 0
            2.0f -> 1
            2.5f -> 2
            3.0f -> 3
            3.5f -> 4
            else -> 1
        }
        seekBarLongPressSpeed.progress = speedProgress
        updateLongPressSpeedDisplay(longPressSpeed)
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 精确进度控制开关
        switchPreciseSeeking.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setPreciseSeekingEnabled(isChecked)
            
            val message = if (isChecked) {
                "已启用精确定位模式\n(定位更准确但可能较慢)"
            } else {
                "已启用快速定位模式\n(定位更快但使用关键帧)"
            }
            DialogUtils.showToastShort(this, message)
        }
        
        // 快进/快退时长选择
        llSeekTimeContainer.setOnClickListener {
            showSeekTimeDialog()
        }
        
        // 长按倍速滑动条
        seekBarLongPressSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = when (progress) {
                    0 -> 1.5f
                    1 -> 2.0f
                    2 -> 2.5f
                    3 -> 3.0f
                    4 -> 3.5f
                    else -> 2.0f
                }
                updateLongPressSpeedDisplay(speed)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 1
                val speed = when (progress) {
                    0 -> 1.5f
                    1 -> 2.0f
                    2 -> 2.5f
                    3 -> 3.0f
                    4 -> 3.5f
                    else -> 2.0f
                }
                preferencesManager.setLongPressSpeed(speed)
                
                DialogUtils.showToastShort(
                    this@PlaybackSettingsActivity, 
                    "长按倍速已设置为 ${String.format("%.1f", speed)}x"
                )
            }
        })
    }
    
    /**
     * 显示快进/快退时长选择对话框
     */
    private fun showSeekTimeDialog() {
        val currentSeekTime = preferencesManager.getSeekTime()
        var selectedSeekTime = currentSeekTime
        
        // 从布局文件加载对话框视图
        val dialogView = layoutInflater.inflate(R.layout.dialog_seek_time_selector, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgSeekTimeOptions)
        
        // 根据当前设置选中对应的 RadioButton
        val selectedId = when (currentSeekTime) {
            3 -> R.id.rb3s
            5 -> R.id.rb5s
            10 -> R.id.rb10s
            15 -> R.id.rb15s
            20 -> R.id.rb20s
            25 -> R.id.rb25s
            30 -> R.id.rb30s
            else -> R.id.rb5s
        }
        radioGroup.check(selectedId)
        
        // 监听选择变化
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedSeekTime = when (checkedId) {
                R.id.rb3s -> 3
                R.id.rb5s -> 5
                R.id.rb10s -> 10
                R.id.rb15s -> 15
                R.id.rb20s -> 20
                R.id.rb25s -> 25
                R.id.rb30s -> 30
                else -> 5
            }
        }
        
        // 创建对话框
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // 设置按钮监听
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.btnConfirm)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnConfirm.setOnClickListener {
            if (selectedSeekTime != currentSeekTime) {
                preferencesManager.setSeekTime(selectedSeekTime)
                updateSeekTimeDisplay(selectedSeekTime)
                
                DialogUtils.showToastShort(
                    this,
                    "快进/快退已设置为 ${selectedSeekTime}秒"
                )
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * 更新快进/快退时长显示
     */
    private fun updateSeekTimeDisplay(seconds: Int) {
        tvSeekTimeValue.text = "${seconds}秒"
    }
    
    /**
     * 更新长按倍速显示
     */
    private fun updateLongPressSpeedDisplay(speed: Float) {
        tvLongPressSpeedValue.text = "${String.format("%.1f", speed)}x"
    }
}
