package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.fam4k007.videoplayer.compose.HomeScreen
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.ThemeManager

class MainActivity : BaseActivity() {
    
    private lateinit var historyManager: PlaybackHistoryManager
    private var lastThemeName: String? = null
    private var needsRefresh = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        // 检查用户是否已同意协议
        if (!UserAgreementActivity.isAgreed(this)) {
            // 未同意，跳转到协议页面
            val intent = Intent(this, UserAgreementActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // 初始化历史记录管理器
        historyManager = PlaybackHistoryManager(this)
        lastThemeName = ThemeManager.getCurrentTheme(this).themeName
        
        setupContent()
    }
    
    override fun onResume() {
        super.onResume()
        // 检查主题是否改变
        val currentThemeName = ThemeManager.getCurrentTheme(this).themeName
        if (lastThemeName != null && lastThemeName != currentThemeName) {
            lastThemeName = currentThemeName
            needsRefresh = false
            recreate() // 主题改变，重新创建 Activity
        } else if (needsRefresh) {
            // 播放记录可能已更新，刷新界面
            needsRefresh = false
            setupContent()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 离开主页时标记需要刷新
        needsRefresh = true
    }
    
    private fun setupContent() {
        val activity = this
        
        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(activity).themeName)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = themeColors.background,
                    onBackground = themeColors.onBackground,
                    surface = themeColors.surface,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = themeColors.onSurface
                )
            ) {
                HomeScreen(
                    historyManager = historyManager,
                    onNavigateToSettings = {
                        startActivity(Intent(activity, SettingsComposeActivity::class.java))
                        overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                    }
                )
            }
        }
    }
}

