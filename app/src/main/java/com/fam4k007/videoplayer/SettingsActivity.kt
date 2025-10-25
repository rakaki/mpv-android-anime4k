package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View

/**
 * 设置页面
 */
class SettingsActivity : BaseActivity() {

    private lateinit var layoutPlaybackHistory: View
    private lateinit var layoutPlaybackSettings: View
    private lateinit var layoutHelp: View
    private lateinit var layoutAboutInfo: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // 设置Toolbar
        setupToolbar(R.id.toolbar, getString(R.string.settings), showBackButton = true)
        
        // 初始化视图
        layoutPlaybackHistory = findViewById(R.id.layoutPlaybackHistory)
        layoutPlaybackSettings = findViewById(R.id.layoutPlaybackSettings)
        layoutHelp = findViewById(R.id.layoutHelp)
        layoutAboutInfo = findViewById(R.id.layoutAboutInfo)
        
        // 设置监听器
        setupListeners()
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
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
}

