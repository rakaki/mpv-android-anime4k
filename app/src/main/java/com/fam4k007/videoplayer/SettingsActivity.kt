package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.fam4k007.videoplayer.utils.ThemeManager

/**
 * 设置页面
 */
class SettingsActivity : BaseActivity() {

    private lateinit var layoutThemeSettings: View
    private lateinit var layoutPlaybackHistory: View
    private lateinit var layoutPlaybackSettings: View
    private lateinit var layoutBiliBiliDanmaku: View
    private lateinit var layoutHelp: View
    private lateinit var layoutAboutInfo: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // 设置返回按钮
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        
        // 初始化视图
        layoutThemeSettings = findViewById(R.id.layoutThemeSettings)
        layoutPlaybackHistory = findViewById(R.id.layoutPlaybackHistory)
        layoutPlaybackSettings = findViewById(R.id.layoutPlaybackSettings)
        layoutBiliBiliDanmaku = findViewById(R.id.layoutBiliBiliDanmaku)
        layoutHelp = findViewById(R.id.layoutHelp)
        layoutAboutInfo = findViewById(R.id.layoutAboutInfo)
        
        // 设置监听器
        setupListeners()
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 主题设置点击
        layoutThemeSettings.setOnClickListener {
            showThemeDialog()
        }
        
        // 播放历史记录入口点击
        layoutPlaybackHistory.setOnClickListener {
            startActivity(Intent(this, PlaybackHistoryActivity::class.java))
            startActivityWithDefaultTransition()
        }
        
        // 播放设置入口点击
        layoutPlaybackSettings.setOnClickListener {
            startActivity(Intent(this, PlaybackSettingsActivity::class.java))
            startActivityWithDefaultTransition()
        }
        
        // 哔哩哔哩弹幕下载点击
        layoutBiliBiliDanmaku.setOnClickListener {
            startActivity(Intent(this, BiliBiliDanmakuActivity::class.java))
            startActivityWithDefaultTransition()
        }
        
        // 使用说明入口点击
        layoutHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
            startActivityWithDefaultTransition()
        }
        
        // 关于信息入口点击
        layoutAboutInfo.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
            startActivityWithDefaultTransition()
        }
    }
    
    /**
     * 显示主题选择对话框
     */
    private fun showThemeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_theme_selector, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // 获取控件
        val radioGroupTheme = dialogView.findViewById<RadioGroup>(R.id.radioGroupTheme)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        
        // 设置当前主题选中状态
        when (ThemeManager.getCurrentTheme(this)) {
            ThemeManager.Theme.BLUE_PURPLE -> dialogView.findViewById<RadioButton>(R.id.radioThemeBluePurple).isChecked = true
            ThemeManager.Theme.GREEN -> dialogView.findViewById<RadioButton>(R.id.radioThemeGreen).isChecked = true
            ThemeManager.Theme.ORANGE -> dialogView.findViewById<RadioButton>(R.id.radioThemeOrange).isChecked = true
            ThemeManager.Theme.PINK -> dialogView.findViewById<RadioButton>(R.id.radioThemePink).isChecked = true
            ThemeManager.Theme.INDIGO -> dialogView.findViewById<RadioButton>(R.id.radioThemeIndigo).isChecked = true
        }
        
        // 取消按钮
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // 确定按钮
        btnConfirm.setOnClickListener {
            // 获取选中的主题
            val selectedTheme = when (radioGroupTheme.checkedRadioButtonId) {
                R.id.radioThemeBluePurple -> ThemeManager.Theme.BLUE_PURPLE
                R.id.radioThemeGreen -> ThemeManager.Theme.GREEN
                R.id.radioThemeOrange -> ThemeManager.Theme.ORANGE
                R.id.radioThemePink -> ThemeManager.Theme.PINK
                R.id.radioThemeIndigo -> ThemeManager.Theme.INDIGO
                else -> ThemeManager.Theme.BLUE_PURPLE
            }
            
            // 应用主题
            ThemeManager.setTheme(this, selectedTheme)
            
            dialog.dismiss()
            
            // 重启当前Activity以应用主题
            recreate()
        }
        
        // 设置窗口背景为透明，让布局的圆角生效
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialog.show()
    }
}
