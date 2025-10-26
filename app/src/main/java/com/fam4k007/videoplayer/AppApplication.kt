package com.fam4k007.videoplayer

import android.app.Application
import com.fam4k007.videoplayer.manager.ThemeManager

/**
 * 应用全局Application类
 */
class AppApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化主题
        ThemeManager.initTheme(this)
    }
}
